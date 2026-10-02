package com.example.filetransfer.domain.model

import java.net.InetAddress

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Discovering : ConnectionState
    data class Connecting(val targetDeviceAddress: String) : ConnectionState
    data class Connected(
        val isGroupOwner: Boolean,
        val groupOwnerAddress: InetAddress?
    ) : ConnectionState
    data class ServiceReady(
        val isGroupOwner: Boolean,
        val hostAddress: String
    ) : ConnectionState
    data class Failed(val error: P2pError) : ConnectionState
}
