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
    viewModel: IncomingTransferViewModel = hiltViewModel(),
    onAccepted: (peerName: String) -> Unit = {}
) {
    val pendingRequest by viewModel.pendingRequest.collectAsStateWithLifecycle()

    val request = pendingRequest ?: return

    IncomingTransferDialog(
        request = request,
        onAccept = { incoming ->
            viewModel.acceptRequest(incoming)
            onAccepted(incoming.senderName)
        },
        onReject = { incoming ->
            viewModel.rejectRequest(incoming)
        }
    )
}
