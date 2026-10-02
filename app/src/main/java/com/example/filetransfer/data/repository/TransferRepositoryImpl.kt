package com.example.filetransfer.data.repository

import com.example.filetransfer.data.file.FileDataSource
import com.example.filetransfer.data.socket.MessageCodec
import com.example.filetransfer.data.socket.TransferSession
import com.example.filetransfer.data.socket.TransferSessionProvider
import com.example.filetransfer.domain.model.CompletedTransfer
import com.example.filetransfer.domain.model.IncomingFileMeta
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferProgress
import com.example.filetransfer.domain.repository.TransferRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Orkestrasi transfer dua-fase (FT-05) di atas MessageCodec (FT-08)
 * dan FileDataSource (FT-09). Semua operasi I/O di Dispatchers.IO.
 */
class TransferRepositoryImpl(
    private val sessionProvider: TransferSessionProvider,
    private val fileDataSource: FileDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TransferRepository {

    private val _progress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())

    override val progress: StateFlow<Map<String, TransferProgress>> = _progress.asStateFlow()

    private val _completed = MutableSharedFlow<CompletedTransfer>(extraBufferCapacity = 8)
    override val completedTransfers: SharedFlow<CompletedTransfer> = _completed.asSharedFlow()

    /** Job transfer aktif, dipakai FT-10 untuk pembatalan. */
    @Volatile
    private var activeJob: Job? = null

    override fun observeIncomingRequests(): Flow<IncomingTransferRequest> =
        sessionProvider.observeIncomingRequests()

    override suspend fun sendTransfer(
        transferId: String,
        senderName: String,
        peerName: String,
        attachments: List<SelectedAttachment>,
        message: String
    ): Result<Unit> {
        if (attachments.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tidak ada file untuk dikirim"))
        }
        return withContext(ioDispatcher) {
            activeJob = coroutineContext[Job]
            runCatchingCancellable {
                val session = sessionProvider.awaitSession()

                MessageCodec.writeRequest(
                    session.output,
                    MessageCodec.RequestFrame(
                        transferId = transferId,
                        senderName = senderName,
                        message = message,
                        files = attachments.map { IncomingFileMeta(it.name, it.sizeBytes) }
                    )
                )

                val response = MessageCodec.readResponse(session.input)
                if (!response.accepted) {
                    throw IOException("Peer menolak transfer (${response.transferId})")
                }

                attachments.forEach { attachment ->
                    coroutineContext.ensureActive()
                    streamFileOut(session, transferId, attachment)
                }

                _completed.emit(
                    CompletedTransfer(
                        transferId = transferId,
                        peerName = peerName,
                        direction = CompletedTransfer.Direction.SENT,
                        message = message,
                        files = attachments.map { IncomingFileMeta(it.name, it.sizeBytes) }
                    )
                )
            }.also { activeJob = null }
        }
    }

    /** Tulis satu file: FILE_HEADER + chunk + FILE_END dengan progres per chunk. */
    private suspend fun streamFileOut(
        session: TransferSession,
        transferId: String,
        attachment: SelectedAttachment
    ) {
        MessageCodec.writeFileHeader(
            session.output,
            MessageCodec.FileHeaderFrame(
                transferId = transferId,
                fileName = attachment.name,
                fileSize = attachment.sizeBytes,
                mimeType = attachment.mimeType.orEmpty()
            )
        )

        val buffer = ByteArray(MessageCodec.CHUNK_SIZE)
        var sent = 0L
        fileDataSource.openInputStream(attachment).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                MessageCodec.writeChunk(session.output, transferId, buffer, read)
                sent += read
                publishProgress(attachment.id, sent, attachment.sizeBytes)
            }
        }
        MessageCodec.writeFileEnd(session.output, transferId)
    }

    override suspend fun respondToRequest(
        request: IncomingTransferRequest,
        accept: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        activeJob = coroutineContext[Job]
        runCatchingCancellable {
            val session = sessionProvider.awaitSession()
            MessageCodec.writeResponse(
                session.output,
                MessageCodec.ResponseFrame(request.transferId, accept)
            )
            if (!accept) return@runCatchingCancellable

            // Terima: stream file yang diminta peer, lalu simpan ke Downloads.
            request.files.forEach { meta ->
                coroutineContext.ensureActive()
                receiveFileIn(session, request.transferId, meta)
            }

            _completed.emit(
                CompletedTransfer(
                    transferId = request.transferId,
                    peerName = request.senderName,
                    direction = CompletedTransfer.Direction.RECEIVED,
                    message = request.message,
                    files = request.files
                )
            )
        }.also { activeJob = null }
    }

    /** Baca satu file: FILE_HEADER, chunk*, FILE_END -> simpan via MediaStore. */
    private suspend fun receiveFileIn(
        session: TransferSession,
        transferId: String,
        meta: IncomingFileMeta
    ) {
        val header = MessageCodec.readFileHeader(session.input)
        if (header.transferId != transferId) {
            throw IOException("transferId tidak cocok: ${header.transferId} != $transferId")
        }

        var written = 0L
        val out = fileDataSource.createDownloadStream(header.fileName, header.mimeType)
        out.use { sink ->
            while (MessageCodec.readFrameType(session.input) != MessageCodec.FRAME_FILE_END) {
                coroutineContext.ensureActive()
                val (_, chunk) = MessageCodec.readChunkBody(session.input)
                sink.write(chunk)
                written += chunk.size
                publishProgress(meta.name, written, header.fileSize)
            }
        }
        fileDataSource.finalizeDownload(header.fileName, header.mimeType)
    }

    override fun cancel() {
        activeJob?.cancel()
        activeJob = null
        _progress.value = emptyMap()
    }

    private fun publishProgress(fileId: String, transferred: Long, total: Long) {
        _progress.update { current ->
            current + (fileId to TransferProgress(fileId, transferred, total))
        }
    }

    /**
     * Seperti runCatching, tapi CancellationException diteruskan agar
     * pembatalan (FT-10) tidak salah dilaporkan sebagai "gagal".
     */
    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
