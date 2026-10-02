package com.example.filetransfer.data.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class SocketDataSource {

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null

    val isSocketConnected: Boolean
        get() = activeSocket?.isConnected == true && activeSocket?.isClosed == false

    suspend fun startServer(port: Int = DEFAULT_PORT): Socket = withContext(Dispatchers.IO) {
        closeSockets()
        val server = ServerSocket(port)
        serverSocket = server
        val client = server.accept()
        activeSocket = client
        client
    }

    suspend fun connectToServer(
        hostAddress: InetAddress,
        port: Int = DEFAULT_PORT,
        timeoutMs: Int = 10_000
    ): Socket = withContext(Dispatchers.IO) {
        closeSockets()
        val socket = Socket()
        socket.connect(java.net.InetSocketAddress(hostAddress, port), timeoutMs)
        activeSocket = socket
        socket
    }

    fun getActiveSocket(): Socket? = activeSocket

    fun closeSockets() {
        try {
            activeSocket?.close()
        } catch (_: IOException) {}
        activeSocket = null

        try {
            serverSocket?.close()
        } catch (_: IOException) {}
        serverSocket = null
    }

    companion object {
        const val DEFAULT_PORT = 8888
    }
}
