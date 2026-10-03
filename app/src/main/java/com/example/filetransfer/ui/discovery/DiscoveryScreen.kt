package com.example.filetransfer.ui.discovery

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filetransfer.domain.model.ConnectionState
import com.example.filetransfer.ui.components.PeerCard
import com.example.filetransfer.ui.components.PrerequisiteBanner
import com.example.filetransfer.ui.connection.ConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: ConnectionViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit = {},
    onConnectedToPeer: () -> Unit = {}
) {
    val context = LocalContext.current
    val prerequisiteState by viewModel.prerequisiteState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    val isDiscovering = connectionState is ConnectionState.Discovering
    val isConnecting = connectionState is ConnectionState.Connecting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cari Perangkat") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrerequisiteBanner(
                prerequisiteState = prerequisiteState,
                onRequestPermission = onRequestPermission,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (isDiscovering) {
                            viewModel.disconnect()
                        } else {
                            viewModel.discoverPeers()
                        }
                    },
                    enabled = prerequisiteState.isReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDiscovering) "Mencari... (Batal)" else "Cari Perangkat")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (peers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDiscovering) "Sedang mencari perangkat..." else "Belum ada perangkat ditemukan.\nTekan 'Cari Perangkat' untuk mulai.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = peers,
                        key = { it.deviceAddress }
                    ) { peer ->
                        PeerCard(
                            peer = peer,
                            isConnecting = isConnecting,
                            onConnect = { viewModel.connect(it) },
                            onDisconnect = { viewModel.disconnect() }
                        )
                    }
                }
            }
        }
    }
}
