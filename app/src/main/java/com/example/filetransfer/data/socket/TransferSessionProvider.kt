package com.example.filetransfer.data.socket

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
 * Seam antara SocketDataSource (Faqih, FT-04) dan lapisan transfer (FT-05).
 * DT: 아직 belum ada -> dipakai [UnavailableSessionProvider].
 */
interface TransferSessionProvider {
    /** True bila socket siap (state ServiceReady). */
    val isSessionReady: kotlinx.coroutines.flow.StateFlow<Boolean>

    /** Tunggu sesi established; lempar exception bila belum siap / putus. */
    suspend fun awaitSession(): TransferSession

    /** Loop pemantau: emissions = permintaan transfer masuk dari peer. */
    fun observeIncomingRequests(): kotlinx.coroutines.flow.Flow<
        com.example.filetransfer.domain.model.IncomingTransferRequest
        >

    /** Tutup sesi (disconnect / error fatal). */
    fun closeSession()
}
