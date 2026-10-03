package com.example.filetransfer.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Root dialog host (PRD asumsi #1): "Permintaan Transfer Masuk" tidak
 * terikat satu layar, tapi di-host di level activity agar tetap muncul
 * walau pengguna sedang di tab Riwayat atau Cari Perangkat.
 */
@Composable
fun IncomingTransferHost(
    viewModel: IncomingTransferViewModel = hiltViewModel()
) {
    val pendingRequest by viewModel.pendingRequest.collectAsStateWithLifecycle()
    val receivedSummary by viewModel.receivedSummary.collectAsStateWithLifecycle()

    val request = pendingRequest
    if (request != null) {
        IncomingTransferDialog(
            request = request,
            onAccept = { incoming -> viewModel.acceptRequest(incoming) },
            onReject = { incoming -> viewModel.rejectRequest(incoming) }
        )
    }

    receivedSummary?.let { done ->
        IncomingTransferCompletedDialog(
            request = done,
            onDismiss = viewModel::dismissReceivedSummary
        )
    }
}
