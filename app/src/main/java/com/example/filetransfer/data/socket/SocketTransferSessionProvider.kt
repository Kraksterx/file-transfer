package com.example.filetransfer.data.socket

import android.util.Log
import com.example.filetransfer.domain.model.IncomingTransferRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi [TransferSessionProvider] di atas socket TCP (FT-04).
 *
 * SATU reader: hanya [observeIncomingRequests] yang membaca InputStream.
 * - REQUEST  -> emit ke Flow (dialog penerima).
 * - RESPONSE -> lengkapi [awaitResponse] pengirim (rendezvous per transferId).
 * - FILE_*   -> teruskan ke [IncomingFileSink] aktif, atau buang agar
 *   stream tetap sejajar bila tidak ada sink (mis. habis cancel).
 * Semua tulis digembok [writeMutex] agar frame tidak interleave.
 */
@Singleton
class SocketTransferSessionProvider @Inject constructor(
    private val socketDataSource: SocketDataSource
) : TransferSessionProvider {

    private val _isSessionReady = MutableStateFlow(false)
    override val isSessionReady: StateFlow<Boolean> = _isSessionReady.asStateFlow()

    private val writeMutex = Mutex()

    @Volatile
    private var activeSink: IncomingFileSink? = null

    private val pendingResponses =
        ConcurrentHashMap<String, CompletableDeferred<MessageCodec.ResponseFrame>>()

    fun updateSessionReady(isReady: Boolean) {
        _isSessionReady.value = isReady
    }

    override suspend fun awaitSession(): TransferSession = withContext(Dispatchers.IO) {
        val socket = socketDataSource.getActiveSocket()
            ?: throw IOException("Socket belum terhubung")
        if (!socket.isConnected || socket.isClosed) {
            throw IOException("Socket sudah terputus")
        }
        TransferSession(
            input = socket.getInputStream(),
            output = socket.getOutputStream()
        )
    }

    override fun observeIncomingRequests(): Flow<IncomingTransferRequest> = flow {
        while (_isSessionReady.value) {
            val socket = socketDataSource.getActiveSocket()
            if (socket == null || socket.isClosed) {
                Log.w(TAG, "loop: socket hilang, berhenti membaca")
                break
            }
            val input = socket.getInputStream()
            val type = try {
                MessageCodec.readFrameType(input)
            } catch (e: MessageCodec.CodecException) {
                if (e.cause is EOFException) {
                    Log.w(TAG, "loop: stream habis (peer menutup koneksi)")
                } else {
                    Log.e(TAG, "loop: frame rusak, berhenti", e)
                }
                break
            } catch (e: IOException) {
                Log.w(TAG, "loop: socket putus: ${e.message}")
                break
            }
            try {
                when (type) {
                    MessageCodec.FRAME_REQUEST -> {
                        val req = MessageCodec.readRequestBody(input)
                        Log.i(
                            TAG,
                            "loop: REQUEST masuk id=${req.transferId} dari=${req.senderName} " +
                                "file=${req.files.size}"
                        )
                        emit(
                            IncomingTransferRequest(
                                transferId = req.transferId,
                                senderName = req.senderName,
                                message = req.message,
                                files = req.files
                            )
                        )
                    }
                    MessageCodec.FRAME_RESPONSE -> {
                        val res = MessageCodec.readResponseBody(input)
                        Log.i(
                            TAG,
                            "loop: RESPONSE masuk id=${res.transferId} accepted=${res.accepted}"
                        )
                        val waiting = pendingResponses.remove(res.transferId)
                        if (waiting == null) {
                            Log.w(TAG, "loop: RESPONSE yatim (tak ada pengirim menunggu) id=${res.transferId}")
                        } else {
                            waiting.complete(res)
                        }
                    }
                    MessageCodec.FRAME_FILE_HEADER -> {
                        val header = MessageCodec.readFileHeaderBody(input)
                        val sink = activeSink
                        if (sink == null) {
                            Log.w(TAG, "loop: FILE_HEADER tanpa sink, lewati file ${header.fileName}")
                            skipIncomingFile(input, header.transferId)
                        } else {
                            Log.i(TAG, "loop: FILE mulai ${header.fileName} (${header.fileSize} B)")
                            sink.onHeader(header)
                        }
                    }
                    MessageCodec.FRAME_FILE_CHUNK -> {
                        val (transferId, bytes) = MessageCodec.readChunkBody(input)
                        activeSink?.onChunk(transferId, bytes)
                            ?: Log.w(TAG, "loop: CHUNK ${bytes.size} B tanpa sink, dibuang")
                    }
                    MessageCodec.FRAME_FILE_END -> {
                        val transferId = MessageCodec.readFileEndBody(input)
                        Log.i(TAG, "loop: FILE selesai id=$transferId")
                        activeSink?.onEnd(transferId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loop: gagal proses frame, berhenti", e)
                activeSink?.onError("", e)
                break
            }
        }
        Log.i(TAG, "loop: keluar (isSessionReady=${_isSessionReady.value})")
    }.flowOn(Dispatchers.IO)

    /** Lewati satu file utuh (header sudah dibaca) agar stream tetap sejajar. */
    private suspend fun skipIncomingFile(
        input: java.io.InputStream,
        transferId: String
    ) {
        while (true) {
            when (MessageCodec.readFrameType(input)) {
                MessageCodec.FRAME_FILE_CHUNK -> MessageCodec.readChunkBody(input)
                MessageCodec.FRAME_FILE_END -> {
                    MessageCodec.readFileEndBody(input)
                    Log.i(TAG, "loop: lewati file id=$transferId selesai")
                    return
                }
                else -> throw MessageCodec.CodecException("Frame tak terduga saat melewati file")
            }
        }
    }

    override suspend fun sendRequest(frame: MessageCodec.RequestFrame) {
        val session = awaitSession()
        writeMutex.withLock { MessageCodec.writeRequest(session.output, frame) }
        Log.d(TAG, "kirim REQUEST id=${frame.transferId} file=${frame.files.size}")
    }

    override suspend fun awaitResponse(
        transferId: String,
        timeoutMs: Long
    ): MessageCodec.ResponseFrame {
        val deferred = CompletableDeferred<MessageCodec.ResponseFrame>()
        pendingResponses[transferId] = deferred
        try {
            Log.d(TAG, "tunggu RESPONSE id=$transferId (timeout ${timeoutMs}ms)")
            return withTimeout(timeoutMs) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "RESPONSE timeout id=$transferId")
            throw e
        } finally {
            pendingResponses.remove(transferId)
        }
    }

    override suspend fun sendResponse(frame: MessageCodec.ResponseFrame) {
        val session = awaitSession()
        writeMutex.withLock { MessageCodec.writeResponse(session.output, frame) }
        Log.i(TAG, "kirim RESPONSE id=${frame.transferId} accepted=${frame.accepted}")
    }

    override suspend fun sendFileHeader(frame: MessageCodec.FileHeaderFrame) {
        val session = awaitSession()
        writeMutex.withLock { MessageCodec.writeFileHeader(session.output, frame) }
        Log.d(TAG, "kirim FILE_HEADER ${frame.fileName} (${frame.fileSize} B)")
    }

    override suspend fun sendFileChunk(transferId: String, buffer: ByteArray, len: Int) {
        val session = awaitSession()
        writeMutex.withLock { MessageCodec.writeChunk(session.output, transferId, buffer, len) }
    }

    override suspend fun sendFileEnd(transferId: String) {
        val session = awaitSession()
        writeMutex.withLock { MessageCodec.writeFileEnd(session.output, transferId) }
        Log.d(TAG, "kirim FILE_END id=$transferId")
    }

    override fun setIncomingSink(sink: IncomingFileSink?) {
        activeSink = sink
    }

    override fun closeSession() {
        _isSessionReady.value = false
        socketDataSource.closeSockets()
    }

    companion object {
        private const val TAG = "TransferProto"
    }
}
