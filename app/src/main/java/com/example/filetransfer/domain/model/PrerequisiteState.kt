package com.example.filetransfer.domain.model

data class PrerequisiteState(
    val wifiP2pEnabled: Boolean = false,
    val permissionGranted: Boolean = false,
    val locationEnabled: Boolean = false
) {
    val isReady: Boolean
        get() =
            wifiP2pEnabled &&
                    permissionGranted &&
                    locationEnabled
}