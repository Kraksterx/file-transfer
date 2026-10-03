package com.example.filetransfer.ui.transfer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferStatus
import com.example.filetransfer.ui.components.AttachmentRow
import com.example.filetransfer.ui.components.TransferProgressBar
import com.example.filetransfer.ui.components.formatBytes
import com.example.filetransfer.ui.theme.FileTransferTheme
import com.example.filetransfer.ui.theme.md_error
import com.example.filetransfer.ui.theme.md_success

/**
 * Layar Transfer (wireframe 2.4 - 2.7, FT-05 + FT-10).
 * Logika nyata ada di TransferViewModel; ini murni state hoisting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.addUris(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Transfer File")
                        Text(
                            "ke: ${state.peerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("<-") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (state.status) {
                TransferStatus.IDLE -> ComposeContent(
                    state = state,
                    onMessageChange = viewModel::onMessageChange,
                    onRemove = viewModel::removeAttachment,
                    onAddFile = { picker.launch(arrayOf("*/*")) },
                    onSend = viewModel::send,
                    onCancel = onBack
                )
                TransferStatus.SENDING -> SendingContent(
                    state = state,
                    onCancel = viewModel::cancelSending
                )
                TransferStatus.SUCCESS -> SuccessContent(
                    state = state,
                    onBack = onBack
                )
                TransferStatus.FAILED -> FailedContent(
                    state = state,
                    onRetry = viewModel::retry,
                    onCancel = viewModel::backToCompose
                )
            }
        }
    }
}

@Composable
private fun ComposeContent(
    state: TransferUiState,
    onMessageChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddFile: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Text("Lampiran (${state.attachments.size})", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.attachments, key = { it.id }) { att ->
            AttachmentRow(attachment = att, onRemove = onRemove)
        }
    }
    Spacer(Modifier.height(8.dp))

    OutlinedButton(onClick = onAddFile, modifier = Modifier.fillMaxWidth()) {
        Text("+ Tambah File")
    }
    Spacer(Modifier.height(12.dp))

    Text("Pesan", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = state.message,
        onValueChange = onMessageChange,
        placeholder = { Text("Tulis pesan di sini...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text("Batal")
        }
        Button(
            onClick = onSend,
            enabled = state.canSend,
            modifier = Modifier.weight(1f)
        ) {
            Text("Kirim")
        }
    }
}

@Composable
private fun SendingContent(
    state: TransferUiState,
    onCancel: () -> Unit
) {
    Text("Mengirim ke ${state.peerName}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.attachments, key = { it.id }) { att ->
            TransferProgressBar(
                fileName = att.name,
                fraction = state.progressFor(att.id)
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("Batalkan")
    }
}

@Composable
private fun SuccessContent(
    state: TransferUiState,
    onBack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("(v)", style = MaterialTheme.typography.displaySmall, color = md_success)
            Spacer(Modifier.height(8.dp))
            Text("File berhasil terkirim", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("To: ${state.peerName}", style = MaterialTheme.typography.bodyMedium)
            if (state.message.isNotBlank()) {
                Text("Pesan: \"${state.message}\"", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            state.attachments.forEach { att ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(att.name, style = MaterialTheme.typography.bodyMedium)
                    Text(formatBytes(att.sizeBytes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Kembali")
    }
}

@Composable
private fun FailedContent(
    state: TransferUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("(x)", style = MaterialTheme.typography.displaySmall, color = md_error)
            Spacer(Modifier.height(8.dp))
            Text("Transfer gagal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                state.errorMessage ?: "Koneksi terputus di tengah proses.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text("Batal")
        }
        Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
            Text("Coba Lagi")
        }
    }
}

// ---------- Previews (tanpa emulator) ----------

@Preview(showBackground = true)
@Composable
private fun PreviewCompose() {
    FileTransferTheme(dynamicColor = false) {
        ComposeContent(
            state = TransferUiState(
                peerName = "Samsung A54",
                attachments = listOf(
                    SelectedAttachment("1", "content://x/1", "foto_kelas.jpg", 4_200_000),
                    SelectedAttachment("2", "content://x/2", "tugas_pbo.pdf", 4_200_000)
                ),
                message = "Oke, ini ya"
            ),
            onMessageChange = {}, onRemove = {}, onAddFile = {}, onSend = {}, onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSending() {
    FileTransferTheme(dynamicColor = false) {
        SendingContent(
            state = TransferUiState(
                attachments = listOf(
                    SelectedAttachment("1", "content://x/1", "foto_kelas.jpg", 4_200_000),
                    SelectedAttachment("2", "content://x/2", "tugas_pbo.pdf", 4_200_000)
                ),
                status = TransferStatus.SENDING,
                progressFractions = mapOf("1" to 0.41f, "2" to 0f)
            ),
            onCancel = {}
        )
    }
}
