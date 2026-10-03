package com.example.filetransfer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PeerStatus
import com.example.filetransfer.ui.theme.LocalSemanticColors

@Composable
fun PeerCard(
    peer: Peer,
    isConnecting: Boolean = false,
    onConnect: (Peer) -> Unit,
    onDisconnect: () -> Unit = {}
) {
    val semanticColors = LocalSemanticColors.current

    val (statusText, statusColor) = when (peer.status) {
        PeerStatus.CONNECTED -> "Terhubung" to semanticColors.success
        PeerStatus.INVITED -> "Diundang..." to semanticColors.warning
        PeerStatus.FAILED -> "Gagal" to semanticColors.error
        PeerStatus.AVAILABLE -> "Tersedia" to MaterialTheme.colorScheme.onSurfaceVariant
        PeerStatus.UNAVAILABLE -> "Tidak Tersedia" to semanticColors.error
        PeerStatus.UNKNOWN -> "Tidak Diketahui" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.deviceName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = peer.deviceAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.15f),
                        labelColor = statusColor
                    )
                )
            }

            if (peer.status == PeerStatus.CONNECTED) {
                OutlinedButton(onClick = onDisconnect) {
                    Text("Putuskan")
                }
            } else {
                Button(
                    onClick = { onConnect(peer) },
                    enabled = !isConnecting && peer.status == PeerStatus.AVAILABLE
                ) {
                    Text(if (isConnecting) "Menghubungkan..." else "Hubungkan")
                }
            }
        }
    }
}
