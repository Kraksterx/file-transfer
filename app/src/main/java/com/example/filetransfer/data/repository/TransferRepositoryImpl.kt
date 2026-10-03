package com.example.filetransfer.data.repository

import android.util.Log
import com.example.filetransfer.data.file.FileDataSource
import com.example.filetransfer.data.local.TransferHistoryDao
import com.example.filetransfer.data.local.TransferRecordEntity
import com.example.filetransfer.data.socket.IncomingFileSink
import com.example.filetransfer.data.socket.MessageCodec
import com.example.filetransfer.data.socket.TransferSessionProvider
import com.example.filetransfer.domain.model.CompletedTransfer
import com.example.filetransfer.domain.model.IncomingFileMeta
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferProgress
import com.example.filetransfer.domain.repository.TransferRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Orkestrasi transfer dua-fase (FT-05) di atas MessageCodec (FT-08)
 * dan FileDataSource (FT-09). Menyimpan riwayat ke Room (FT-11).
 *
 * Aturan baca: repository TIDAK PERNAH membaca socket langsung.
 * Satu-satunya reader adalah loop provider; RESPONSE dirute via
 * awaitResponse, byte file via IncomingFileSink.
 */
@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val sessionProvider: TransferSessionProvider,
    private val fileDataSource: FileDataSource,
    private val historyDao: TransferHistoryDao? = null,
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
                sessionProvider.awaitSession() // validasi socket hidup, gagal cepat bila putus

                sessionProvider.sendRequest(
                    MessageCodec.RequestFrame(
                        transferId = transferId,
                        senderName = senderName,
                        message = message,
                        files = attachments.map { IncomingFileMeta(it.name, it.sizeBytes) }
                    )
                )

                val response = try {
                    sessionProvider.awaitResponse(transferId)
                } catch (e: TimeoutCancellationException) {
                    throw IOException(
                        "Peer tidak merespons dalam " +
                            "${TransferSessionProvider.RESPONSE_TIMEOUT_MS / 1000} detik. " +
                            "Pastikan hanya SATU sisi yang menekan Kirim dan sisi " +
                            "satunya menekan Terima pada dialog."
                    )
                }
                if (!response.accepted) {
                    throw IOException("Peer menolak transfer (${response.transferId})")
                }

                attachments.forEach { attachment ->
                    coroutineContext.ensureActive()
                    streamFileOut(transferId, attachment)
                }

                val completedTransfer = CompletedTransfer(
                    transferId = transferId,
                    peerName = peerName,
                    direction = CompletedTransfer.Direction.SENT,
                    message = message,
                    files = attachments.map { IncomingFileMeta(it.name, it.sizeBytes) }
                )
                _completed.emit(completedTransfer)

                // Simpan ke Room (FT-11)
                historyDao?.insertRecord(
                    TransferRecordEntity(
                        transferId = transferId,
                        peerName = peerName,
                        direction = "SENT",
                        message = message,
                        fileCount = attachments.size,
                        totalSizeBytes = attachments.sumOf { it.sizeBytes },
                        fileNamesJson = attachments.joinToString(", ") { it.name }
                    )
                )
                Unit
            }.also { activeJob = null }
        }
    }

    /** Tulis satu file: FILE_HEADER + chunk + FILE_END dengan progres per chunk. */
    private suspend fun streamFileOut(
        transferId: String,
        attachment: SelectedAttachment
    ) {
        sessionProvider.sendFileHeader(
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
                sessionProvider.sendFileChunk(transferId, buffer, read)
                sent += read
                publishProgress(attachment.id, sent, attachment.sizeBytes)
            }
        }
        sessionProvider.sendFileEnd(transferId)
    }

    override suspend fun respondToRequest(
        request: IncomingTransferRequest,
        accept: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        activeJob = coroutineContext[Job]
        runCatchingCancellable {
            sessionProvider.awaitSession() // validasi socket hidup, gagal cepat bila putus

            // Daftarkan sink DULU agar byte yang datang langsung ada penampungnya.
            val completion = CompletableDeferred<Result<Unit>>()
            sessionProvider.setIncomingSink(
                RepositoryIncomingSink(
                    expectedFileCount = request.files.size,
                    completion = completion
                )
            )
            try {
                sessionProvider.sendResponse(
                    MessageCodec.ResponseFrame(request.transferId, accept)
                )
                if (!accept) return@runCatchingCancellable

                // Tunggu read loop provider selesai menerima semua file.
                completion.await().getOrThrow()

                val completedTransfer = CompletedTransfer(
                    transferId = request.transferId,
                    peerName = request.senderName,
                    direction = CompletedTransfer.Direction.RECEIVED,
                    message = request.message,
                    files = request.files
                )
                _completed.emit(completedTransfer)

                // Simpan ke Room (FT-11)
                historyDao?.insertRecord(
                    TransferRecordEntity(
                        transferId = request.transferId,
                        peerName = request.senderName,
                        direction = "RECEIVED",
                        message = request.message,
                        fileCount = request.files.size,
                        totalSizeBytes = request.files.sumOf { it.sizeBytes },
                        fileNamesJson = request.files.joinToString(", ") { it.name }
                    )
                )
                Unit
            } finally {
                sessionProvider.setIncomingSink(null)
            }
        }.also { activeJob = null }
    }

    /**
     * Menampung byte file dari read loop provider dan menyimpannya
     * via MediaStore (FT-09) dengan progres per file (FT-10).
     */
    private inner class RepositoryIncomingSink(
        private val expectedFileCount: Int,
        private val completion: CompletableDeferred<Result<Unit>>
    ) : IncomingFileSink {

        private var out: OutputStream? = null
        private var fileKey = ""
        private var fileName = ""
        private var mimeType = ""
        private var expectedBytes = 0L
        private var writtenBytes = 0L
        private var filesDone = 0

        override suspend fun onHeader(header: MessageCodec.FileHeaderFrame) {
            runCatching { out?.close() }
            fileKey = header.fileName
            fileName = header.fileName
            mimeType = header.mimeType
            expectedBytes = header.fileSize
            writtenBytes = 0L
            publishProgress(fileKey, 0L, expectedBytes)
            out = try {
                fileDataSource.createDownloadStream(fileName, mimeType.ifEmpty { null })
            } catch (e: Exception) {
                completion.complete(Result.failure(e))
                throw e
            }
        }

        override suspend fun onChunk(transferId: String, bytes: ByteArray) {
            val sink = out
            if (sink == null) {
                completion.complete(
                    Result.failure(IOException("CHUNK datang tanpa FILE_HEADER"))
                )
                throw IOException("CHUNK datang tanpa FILE_HEADER")
            }
            sink.write(bytes)
            writtenBytes += bytes.size
            publishProgress(fileKey, writtenBytes, expectedBytes)
        }

        override suspend fun onEnd(transferId: String) {
            runCatching { out?.close() }
            out = null
            fileDataSource.finalizeDownload(fileName, mimeType.ifEmpty { null })
            publishProgress(fileKey, expectedBytes, expectedBytes)
            filesDone += 1
            if (filesDone >= expectedFileCount) {
                completion.complete(Result.success(Unit))
            }
        }

        override suspend fun onError(transferId: String, cause: Throwable) {
            runCatching { out?.close() }
            out = null
            completion.complete(Result.failure(cause))
        }
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
        Log.e(TAG, "transfer gagal: ${e.message}", e)
        Result.failure(e)
    }

    companion object {
        private const val TAG = "TransferProto"
    }
}
