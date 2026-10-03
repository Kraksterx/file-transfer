package com.example.filetransfer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Discovery : Screen("discovery", "Cari", Icons.Default.Search)
    data object History : Screen("history", "Riwayat", Icons.Default.List)
    /** Destination (bukan tab): dibuka via "Kirim File" setelah peer terhubung. */
    data object Transfer : Screen("transfer/{peerName}", "Transfer", Icons.Default.Send) {
        const val ARG_PEER_NAME = "peerName"
        fun createRoute(peerName: String) = "transfer/${android.net.Uri.encode(peerName)}"
    }
}

/** Transfer bukan tab bar — transfer butuh koneksi dulu (via Kirim File). */
val bottomNavItems = listOf(
    Screen.Discovery,
    Screen.History
)
