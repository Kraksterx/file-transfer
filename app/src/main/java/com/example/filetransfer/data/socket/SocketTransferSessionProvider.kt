package com.example.filetransfer.data.socket

import com.example.filetransfer.domain.model.IncomingTransferRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketTransferSessionProvider @Inject constructor(
    private val socketDataSource: SocketDataSource
) : TransferSessionProvider {

    private val _isSessionReady = MutableStateFlow(false)
    override val isSessionReady: StateFlow<Boolean> = _isSessionReady.asStateFlow()

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
            val socket = socketDataSource.getActiveSocket() ?: break
            if (socket.isClosed) break
            try {
                val req = MessageCodec.readRequest(socket.getInputStream())
                emit(
                    IncomingTransferRequest(
                        transferId = req.transferId,
                        senderName = req.senderName,
                        message = req.message,
                        files = req.files
                    )
                )
            } catch (_: Exception) {
                break
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun closeSession() {
        _isSessionReady.value = false
        socketDataSource.closeSockets()
    }
}
