package com.example.filetransfer.domain.model

sealed interface P2pError {
    data object WifiP2pDisabled : P2pError
    data object PermissionDenied : P2pError
    data object LocationDisabled : P2pError
    data class DiscoveryFailed(val code: Int) : P2pError
    data class ConnectionFailed(val code: Int) : P2pError
    data class SocketError(val message: String) : P2pError
    data class Unknown(val message: String) : P2pError
}
