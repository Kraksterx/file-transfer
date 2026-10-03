package com.example.filetransfer.data.socket

import com.example.filetransfer.domain.model.IncomingTransferRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.IOException

/**
 * Placeholder sampai Faqih push SocketDataSource (FT-04).
 * Semua operasi gagal cepat dengan pesan jelas supaya tidak ada
 * crash diam-diam saat UI sudah memakai transfer layer.
 */
class UnavailableSessionProvider : TransferSessionProvider {

    private val ready = MutableStateFlow(false)
    override val isSessionReady: StateFlow<Boolean> = ready.asStateFlow()

    override suspend fun awaitSession(): TransferSession =
        throw IOException("Socket belum siap (ServiceReady). Menunggu SocketDataSource FT-04.")

    override fun observeIncomingRequests(): Flow<IncomingTransferRequest> = emptyFlow()

    override fun closeSession() {
        ready.value = false
    }
}
