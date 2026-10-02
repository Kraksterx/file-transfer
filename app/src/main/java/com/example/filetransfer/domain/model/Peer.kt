package com.example.filetransfer.domain.model

enum class PeerStatus {
    CONNECTED,
    INVITED,
    FAILED,
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN
}

data class Peer(
    val deviceName: String,
    val deviceAddress: String,
    val primaryDeviceType: String? = null,
    val status: PeerStatus = PeerStatus.AVAILABLE,
    val isGroupOwner: Boolean = false
)
