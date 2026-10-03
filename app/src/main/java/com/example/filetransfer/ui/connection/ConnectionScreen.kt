package com.example.filetransfer.ui.connection

import androidx.compose.runtime.Composable
import com.example.filetransfer.ui.discovery.DiscoveryScreen

@Composable
fun ConnectionScreen(
    onRequestPermission: () -> Unit = {},
    onConnectedToPeer: () -> Unit = {}
) {
    DiscoveryScreen(
        onRequestPermission = onRequestPermission,
        onConnectedToPeer = onConnectedToPeer
    )
}
