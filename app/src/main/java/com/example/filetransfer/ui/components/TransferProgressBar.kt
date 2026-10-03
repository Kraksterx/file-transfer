package com.example.filetransfer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Progres per file (wireframe [23], FT-10).
 * Menampilkan nama file + bar + persen, atau "menunggu..." bila 0%.
 */
@Composable
fun TransferProgressBar(
    fileName: String,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val pct = (fraction.coerceIn(0f, 1f) * 100).toInt()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (pct <= 0) "menunggu..." else "$pct%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
