package com.example.filetransfer.ui.root

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.filetransfer.P2pApp
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.usecase.ObserveIncomingTransferUseCase
import com.example.filetransfer.domain.usecase.RespondToTransferUseCase
import kotlinx.coroutines.launch

/**
 * Root dialog host (PRD asumsi #1): "Permintaan Transfer Masuk" tidak
 * terikat satu layar, tapi di-host di level activity agar tetap muncul
 * walau pengguna sedang di tab Riwayat atau Cari Perangkat.
 */
@Composable
fun IncomingTransferHost(
    onAccepted: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as P2pApp
    val container = app.appContainer

    val observeRequests: ObserveIncomingTransferUseCase =
        remember(container) { container.observeIncomingTransferUseCase }
    val respondToTransfer: RespondToTransferUseCase =
        remember(container) { container.respondToTransferUseCase }

    var pendingRequest by remember { mutableStateOf<IncomingTransferRequest?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(observeRequests) {
        observeRequests().collect { pendingRequest = it }
    }

    val request = pendingRequest ?: return

    IncomingTransferDialog(
        request = request,
        onAccept = { incoming ->
            pendingRequest = null
            onAccepted()
            // Terima: balas ACCEPT lalu receiver stream & simpan file.
            scope.launch { respondToTransfer(incoming, accept = true) }
        },
        onReject = { incoming ->
            pendingRequest = null
            scope.launch { respondToTransfer(incoming, accept = false) }
        }
    )
}
