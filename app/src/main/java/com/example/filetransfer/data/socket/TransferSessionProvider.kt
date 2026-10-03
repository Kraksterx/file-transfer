package com.example.filetransfer.data.socket

import com.example.filetransfer.domain.model.IncomingTransferRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream

/**
 * Satu sesi socket P2P yang sudah established (FT-04, milik Faqih).
 * Di sini hanya kontrak stream yang dipakai lapisan transfer.
 */
class TransferSession(
    val input: InputStream,
    val output: OutputStream
) {
    fun close() {
        runCatching { input.close() }
        runCatching { output.close() }
    }
}

/**
 * Penerima byte file masuk. Diimplementasikan repository (punya FileDataSource),
 * dipanggil HANYA dari read loop provider — tidak ada reader lain.
 */
interface IncomingFileSink {
    suspend fun onHeader(header: MessageCodec.FileHeaderFrame)
    suspend fun onChunk(transferId: String, bytes: ByteArray)
    suspend fun onEnd(transferId: String)
    suspend fun onError(transferId: String, cause: Throwable)
}

/**
 * Seam antara SocketDataSource (Faqih, FT-04) dan lapisan transfer (FT-05).
 *
 * Aturan keras: read loop di [observeIncomingRequests] adalah SATU-SATUNYA
 * pembaca InputStream. Repository tidak boleh membaca stream langsung —
 * RESPONSE dirute via [awaitResponse], byte file via [IncomingFileSink].
 * Semua tulis lewat mutex internal agar frame tidak interleave.
 */
interface TransferSessionProvider {
    /** True bila socket siap (state ServiceReady). */
    val isSessionReady: StateFlow<Boolean>

    /** Validasi socket masih hidup; lempar IOException bila belum siap / putus. */
    suspend fun awaitSession(): TransferSession

    /**
     * Read loop: SATU-SATUNYA pembaca stream. Emit permintaan masuk,
     * rute RESPONSE ke [awaitResponse], teruskan byte file ke sink.
     */
    fun observeIncomingRequests(): Flow<IncomingTransferRequest>

    /** Tutup sesi (disconnect / error fatal). */
    fun closeSession()

    // ---- Sisi pengirim (tulis + tunggu respons, tanpa membaca langsung) ----

    suspend fun sendRequest(frame: MessageCodec.RequestFrame)

    /**
     * Tunggu RESPONSE untuk [transferId].
     * @throws kotlinx.coroutines.TimeoutCancellationException bila peer tak merespons.
     */
    suspend fun awaitResponse(
        transferId: String,
        timeoutMs: Long = RESPONSE_TIMEOUT_MS
    ): MessageCodec.ResponseFrame

    suspend fun sendFileHeader(frame: MessageCodec.FileHeaderFrame)
    suspend fun sendFileChunk(transferId: String, buffer: ByteArray, len: Int)
    suspend fun sendFileEnd(transferId: String)

    // ---- Sisi penerima ----

    suspend fun sendResponse(frame: MessageCodec.ResponseFrame)

    /** Daftarkan sink SEBELUM menulis RESPONSE accept (hindari race). */
    fun setIncomingSink(sink: IncomingFileSink?)

    companion object {
        const val RESPONSE_TIMEOUT_MS = 30_000L
    }
}
