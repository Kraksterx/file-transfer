package com.example.filetransfer.ui.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.filetransfer.domain.model.IncomingFileMeta
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.ui.components.formatBytes
import com.example.filetransfer.ui.theme.FileTransferTheme

/**
 * Dialog global "Permintaan Transfer Masuk" (FT-05, PRD asumsi #1).
 * Di-host di level root/activity agar terlihat dari tab mana pun,
 * bukan di dalam TransferScreen.
 */
@Composable
fun IncomingTransferDialog(
    request: IncomingTransferRequest,
    onAccept: (IncomingTransferRequest) -> Unit,
    onReject: (IncomingTransferRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { /* wajib pilih Terima/Tolak, tidak bisa dismiss */ },
        title = { Text("Permintaan Transfer Masuk") },
        text = {
            Column {
                Text(
                    "Dari: ${request.senderName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (request.message.isNotBlank()) {
                    Text(
                        "Pesan: \"${request.message}\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                request.files.forEach { f ->
                    Text(
                        "- ${f.name} (${formatBytes(f.sizeBytes)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAccept(request) }) { Text("Terima") }
        },
        dismissButton = {
            TextButton(onClick = { onReject(request) }) { Text("Tolak") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewIncomingDialog() {
    FileTransferTheme {
        IncomingTransferDialog(
            request = IncomingTransferRequest(
                transferId = "t1",
                senderName = "Redmi Note 12",
                message = "Oke, ini ya",
                files = listOf(
                    IncomingFileMeta("materi_kuliah.zip", 12_000_000)
                )
            ),
            onAccept = {},
            onReject = {}
        )
    }
}
