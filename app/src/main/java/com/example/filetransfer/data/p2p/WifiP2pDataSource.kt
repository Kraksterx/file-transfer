package com.example.filetransfer.data.p2p

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiP2pDataSource(
    private val context: Context
) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE)
                as? WifiP2pManager

    private var channel: WifiP2pManager.Channel? = null

    init {
        if (manager != null) {
            channel = manager.initialize(
                context,
                context.mainLooper,
                null
            )
        }
    }

    fun observeP2pState(): Flow<Boolean> = callbackFlow {

        val wifiP2pManager = manager
        val wifiP2pChannel = this@WifiP2pDataSource.channel

        if (wifiP2pManager == null || wifiP2pChannel == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action !=
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
                ) {
                    return
                }

                val state = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    WifiP2pManager.WIFI_P2P_STATE_DISABLED
                )

                trySend(
                    state ==
                            WifiP2pManager.WIFI_P2P_STATE_ENABLED
                )
            }
        }

        val filter = IntentFilter().apply {
            addAction(
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
            )
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        requestInitialP2pState(
            wifiP2pManager,
            wifiP2pChannel
        ) { isEnabled ->
            trySend(isEnabled)
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
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
            onResult(
                state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            )
        }
    }

    private fun hasNearbyWifiPermission(): Boolean {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

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
}