package com.example.filetransfer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Discovery : Screen("discovery", "Cari", Icons.Default.Search)
    data object Transfer : Screen("transfer", "Transfer", Icons.Default.Send)
    data object History : Screen("history", "Riwayat", Icons.Default.List)
}

val bottomNavItems = listOf(
    Screen.Discovery,
    Screen.Transfer,
    Screen.History
)
