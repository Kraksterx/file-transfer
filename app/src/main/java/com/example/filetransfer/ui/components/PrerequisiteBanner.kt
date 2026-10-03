package com.example.filetransfer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filetransfer.domain.model.PrerequisiteState
import com.example.filetransfer.ui.theme.LocalSemanticColors

@Composable
fun PrerequisiteBanner(
    prerequisiteState: PrerequisiteState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (prerequisiteState.isReady) return

    val warningColor = LocalSemanticColors.current.warning

    val (title, description, buttonText, action) = when {
        !prerequisiteState.wifiP2pEnabled -> Quadruple(
            "Wi-Fi Direct Mati",
            "Aktifkan Wi-Fi di Pengaturan untuk menggunakan P2P File Transfer.",
            "Buka Pengaturan",
            onOpenSettings
        )
        !prerequisiteState.permissionGranted -> Quadruple(
            "Izin Perangkat Sekitar Dibutuhkan",
            "Aplikasi membutuhkan izin untuk menemukan perangkat terdekat.",
            "Berikan Izin",
            onRequestPermission
        )
        !prerequisiteState.locationEnabled -> Quadruple(
            "Layanan Lokasi Mati",
            "Aktifkan Lokasi/GPS untuk mendeteksi perangkat Wi-Fi Direct.",
            "Buka Pengaturan",
            onOpenSettings
        )
        else -> Quadruple("Prasyarat Belum Terpenuhi", "", "Buka Pengaturan", onOpenSettings)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = warningColor.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = warningColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = action,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonText)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
