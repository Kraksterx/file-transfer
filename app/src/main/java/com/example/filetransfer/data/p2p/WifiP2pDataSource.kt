package com.example.filetransfer.data.p2p

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PeerStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiP2pDataSource(
    private val context: Context
) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager

    private var wifiChannel: WifiP2pManager.Channel? = manager?.initialize(
        context,
        context.mainLooper,
        null
    )

    fun observeP2pState(): Flow<Boolean> = callbackFlow {
        val wifiP2pManager = manager
        val p2pChannel = wifiChannel

        if (wifiP2pManager == null || p2pChannel == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION) {
                    val state = intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED
                    )
                    trySend(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }
            }
        }

        val filter = IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        requestInitialP2pState(wifiP2pManager, p2pChannel) { isEnabled ->
            trySend(isEnabled)
        }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission")
    fun observePeers(): Flow<List<Peer>> = callbackFlow {
        val wifiP2pManager = manager
        val p2pChannel = wifiChannel

        if (wifiP2pManager == null || p2pChannel == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val peerListListener = WifiP2pManager.PeerListListener { peerList: WifiP2pDeviceList ->
            val domainPeers = peerList.deviceList.map { device ->
                device.toDomainPeer()
            }
            trySend(domainPeers)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
                    if (hasNearbyWifiPermission()) {
                        wifiP2pManager.requestPeers(p2pChannel, peerListListener)
                    }
                }
            }
        }

        val filter = IntentFilter(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        if (hasNearbyWifiPermission()) {
            wifiP2pManager.requestPeers(p2pChannel, peerListListener)
        }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun observeConnectionInfo(): Flow<WifiP2pInfo> = callbackFlow {
        val wifiP2pManager = manager
        val p2pChannel = wifiChannel

        if (wifiP2pManager == null || p2pChannel == null) {
            close()
            return@callbackFlow
        }

        val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->
            trySend(info)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
                    wifiP2pManager.requestConnectionInfo(p2pChannel, connectionListener)
                }
            }
        }

        val filter = IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        wifiP2pManager.requestConnectionInfo(p2pChannel, connectionListener)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        val wifiP2pManager = manager ?: run { onFailure(-1); return }
        val p2pChannel = wifiChannel ?: run { onFailure(-1); return }

        wifiP2pManager.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        })
    }

    fun stopPeerDiscovery(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        val wifiP2pManager = manager ?: return
        val p2pChannel = wifiChannel ?: return

        wifiP2pManager.stopPeerDiscovery(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        val wifiP2pManager = manager ?: run { onFailure(-1); return }
        val p2pChannel = wifiChannel ?: run { onFailure(-1); return }

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        wifiP2pManager.connect(p2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        })
    }

    fun disconnect(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        val wifiP2pManager = manager ?: return
        val p2pChannel = wifiChannel ?: return

        wifiP2pManager.removeGroup(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onSuccess()
            override fun onFailure(reasonCode: Int) = onFailure(reasonCode)
        })
    }

    private fun requestInitialP2pState(
        manager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        onResult: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        if (!hasNearbyWifiPermission()) {
            return
        }

        manager.requestP2pState(channel) { state ->
            onResult(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
        }
    }

    fun hasNearbyWifiPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun WifiP2pDevice.toDomainPeer(): Peer {
        val peerStatus = when (status) {
            WifiP2pDevice.CONNECTED -> PeerStatus.CONNECTED
            WifiP2pDevice.INVITED -> PeerStatus.INVITED
            WifiP2pDevice.FAILED -> PeerStatus.FAILED
            WifiP2pDevice.AVAILABLE -> PeerStatus.AVAILABLE
            WifiP2pDevice.UNAVAILABLE -> PeerStatus.UNAVAILABLE
            else -> PeerStatus.UNKNOWN
        }
        return Peer(
            deviceName = deviceName.ifBlank { "Unknown Device" },
            deviceAddress = deviceAddress,
            primaryDeviceType = primaryDeviceType,
            status = peerStatus,
            isGroupOwner = isGroupOwner
        )
    }
}
