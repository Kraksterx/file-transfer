package com.example.filetransfer.ui.connection

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filetransfer.domain.model.ConnectionState
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PeerStatus
import com.example.filetransfer.domain.model.PrerequisiteState

// Colors based on the mockup
val PrimaryBlue = Color(0xFF005BAA)
val BackgroundLight = Color(0xFFF4F7FB)
val TextDark = Color(0xFF1A1A1A)
val TextGray = Color(0xFF666666)
val SuccessGreen = Color(0xFF008A56)
val DangerRed = Color(0xFFD32F2F)
val DangerRedBg = Color(0xFFFFEAEB)
val LightBlueBg = Color(0xFFE8F1F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel = hiltViewModel(),
    onNavigateToTransfer: () -> Unit = {}
) {
    val prerequisiteState by viewModel.prerequisiteState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    val isDiscovering = connectionState is ConnectionState.Discovering

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy((-6).dp)) {
                        Text(
                            "Wifi Direct Share",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Discovery Cari",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = "App Logo", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.PersonOutline, contentDescription = "Profile", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WifiTethering, contentDescription = "Cari") },
                    label = { Text("Cari") },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = LightBlueBg
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") },
                    selected = false,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning if prerequisites not met
                if (!prerequisiteState.isReady) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DangerRedBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Prasyarat Belum Terpenuhi", fontWeight = FontWeight.Bold, color = DangerRed)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pastikan Wi-Fi, Lokasi, dan Izin Aplikasi diaktifkan.", color = TextDark, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { /* Implement Settings Intent */ },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Buka Pengaturan", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // My Device Card
                item {
                    MyDeviceCard(prerequisiteState, isDiscovering)
                }

                if (isDiscovering) {
                    // Radar Animation State
                    item {
                        RadarSearchingView(onCancel = { viewModel.disconnect() })
                    }
                } else {
                    // Speed Info Card
                    item {
                        SpeedInfoCard()
                    }

                    // Search Button
                    item {
                        Button(
                            onClick = { viewModel.discoverPeers() },
                            enabled = prerequisiteState.isReady,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cari Perangkat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Peers List (if not discovering, or if discovering and found some)
                if (peers.isNotEmpty() || !isDiscovering) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Perangkat tersedia", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = LightBlueBg,
                                    shape = CircleShape,
                                    modifier = Modifier.defaultMinSize(minWidth = 24.dp)
                                ) {
                                    Text(
                                        text = peers.size.toString(),
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (isDiscovering) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Radar aktif", fontSize = 12.sp, color = TextGray)
                                }
                            }
                        }
                    }

                    if (peers.isEmpty() && !isDiscovering) {
                        item {
                            Text(
                                "Perangkat tidak ditemukan",
                                color = TextGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(peers) { peer ->
                            PeerItemCard(
                                peer = peer,
                                onConnect = { viewModel.connect(it) },
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }
                    }
                }

                // Tips Card
                item {
                    TipsCard()
                }
            }
        }
    }
}

@Composable
fun MyDeviceCard(prerequisiteState: PrerequisiteState, isDiscovering: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = PrimaryBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(android.os.Build.MODEL, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(Saya)", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    if (isDiscovering) "Wi-Fi Direct 5GHz • Terbuka untuk sekitar" else "Siap menerima & mengirim",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
            if (!isDiscovering) {
                Surface(
                    color = if (prerequisiteState.wifiP2pEnabled) PrimaryBlue else Color.Gray,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (prerequisiteState.wifiP2pEnabled) Color.Green else Color.LightGray))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (prerequisiteState.wifiP2pEnabled) "P2P: ON" else "P2P: OFF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SuccessGreen))
            }
        }
    }
}

@Composable
fun SpeedInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("KECEPATAN MAKSIMAL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Wi-Fi Direct 5GHz", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kanal lokal aman • Tanpa kuota internet", color = TextGray, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryBlue)
            }
        }
    }
}

@Composable
fun RadarSearchingView(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dashed circles
                Box(modifier = Modifier.fillMaxSize().border(2.dp, LightBlueBg, CircleShape))
                Box(modifier = Modifier.size(120.dp).border(2.dp, LightBlueBg, CircleShape))
                
                // Rotating radar line/icons (simulated)
                Box(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                    Box(modifier = Modifier.size(10.dp).align(Alignment.TopCenter).clip(CircleShape).background(PrimaryBlue))
                }

                // Center Phone
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color.White)
                        Text(android.os.Build.MODEL, color = Color.White, fontSize = 10.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Mencari perangkat...", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pastikan perangkat penerima telah mengaktifkan mode Wi-Fi Direct dan visibilitas perangkat diatur ke publik.",
                color = TextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRedBg, contentColor = DangerRed),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batalkan Pencarian", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PeerItemCard(peer: Peer, onConnect: (Peer) -> Unit, onDisconnect: () -> Unit) {
    val isInvited = peer.status == PeerStatus.INVITED
    val isConnected = peer.status == PeerStatus.CONNECTED
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
                if (isInvited) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.deviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (peer.status) {
                        PeerStatus.CONNECTED -> SuccessGreen
                        PeerStatus.INVITED -> PrimaryBlue
                        else -> TextGray
                    }
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (peer.status) {
                            PeerStatus.CONNECTED -> "Terhubung"
                            PeerStatus.INVITED -> "Diundang..."
                            else -> "Tersedia"
                        },
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            if (isInvited || isConnected) {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRedBg, contentColor = DangerRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batal")
                }
            } else {
                Button(
                    onClick = { onConnect(peer) },
                    colors = ButtonDefaults.buttonColors(containerColor = LightBlueBg, contentColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hubungkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TipsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightBlueBg.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Tips Berbagi Cepat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Pastikan perangkat tujuan membuka halaman pencarian ini dan berada dalam jangkauan 10 meter.",
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
