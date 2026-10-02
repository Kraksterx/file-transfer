package com.example.filetransfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.filetransfer.ui.root.IncomingTransferHost
import com.example.filetransfer.ui.theme.FileTransferTheme
import com.example.filetransfer.ui.transfer.TransferScreen
import com.example.filetransfer.ui.transfer.TransferViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as P2pApp).appContainer

        setContent {
            FileTransferTheme {
                // TODO: ganti dengan AppNavHost (Rani) setelah NavigationCompose type-safe siap.
                val transferViewModel = remember {
                    TransferViewModel(
                        sendTransfer = container.sendTransferUseCase,
                        cancelTransfer = container.cancelTransferUseCase,
                        progress = container.transferRepository.progress,
                        fileDataSource = container.fileDataSource
                    )
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TransferScreen(
                            viewModel = transferViewModel,
                            onBack = { /* navigasi balik nanti */ }
                        )
                        // Dialog permintaan masuk: di-host di level activity (PRD asumsi #1)
                        IncomingTransferHost()
                    }
                }
            }
        }
    }
}
