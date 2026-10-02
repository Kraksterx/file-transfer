package com.example.filetransfer.data.repository

import android.content.Context
import android.location.LocationManager
import com.example.filetransfer.data.p2p.WifiP2pDataSource
import com.example.filetransfer.data.socket.SocketDataSource
import com.example.filetransfer.domain.model.ConnectionState
import com.example.filetransfer.domain.model.P2pError
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PrerequisiteState
import com.example.filetransfer.domain.repository.P2pRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class P2pRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val p2pDataSource: WifiP2pDataSource,
    private val socketDataSource: SocketDataSource
) : P2pRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    override val peers: Flow<List<Peer>> = p2pDataSource.observePeers()

    private val _permissionsGranted = MutableStateFlow(p2pDataSource.hasNearbyWifiPermission())

    override val prerequisitesState: Flow<PrerequisiteState> = combine(
        p2pDataSource.observeP2pState(),
        _permissionsGranted
    ) { p2pEnabled, permissionGranted ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        PrerequisiteState(
            wifiP2pEnabled = p2pEnabled,
            permissionGranted = permissionGranted,
            locationEnabled = locationEnabled
        )
    }

    init {
        observeConnectionInfo()
    }

    fun updatePermissionStatus(isGranted: Boolean) {
        _permissionsGranted.value = isGranted
    }

    private fun observeConnectionInfo() {
        scope.launch {
            p2pDataSource.observeConnectionInfo().collect { info ->
                if (info.groupFormed) {
                    _connectionState.value = ConnectionState.Connected(
                        isGroupOwner = info.isGroupOwner,
                        groupOwnerAddress = info.groupOwnerAddress
                    )
                    establishSocket(info.isGroupOwner, info.groupOwnerAddress)
                } else {
                    socketDataSource.closeSockets()
                    _connectionState.value = ConnectionState.Idle
                }
            }
        }
    }

    private fun establishSocket(isGroupOwner: Boolean, groupOwnerAddress: java.net.InetAddress?) {
        scope.launch {
            try {
                if (isGroupOwner) {
                    socketDataSource.startServer()
                    _connectionState.value = ConnectionState.ServiceReady(
                        isGroupOwner = true,
                        hostAddress = "0.0.0.0"
                    )
                } else {
                    if (groupOwnerAddress != null) {
                        socketDataSource.connectToServer(groupOwnerAddress)
                        _connectionState.value = ConnectionState.ServiceReady(
                            isGroupOwner = false,
                            hostAddress = groupOwnerAddress.hostAddress ?: ""
                        )
                    } else {
                        _connectionState.value = ConnectionState.Failed(
                            P2pError.SocketError("Group owner address is null")
                        )
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(
                    P2pError.SocketError(e.message ?: "Failed to establish socket")
                )
            }
        }
    }

    override fun discoverPeers() {
        _connectionState.value = ConnectionState.Discovering
        p2pDataSource.discoverPeers(
            onSuccess = {},
            onFailure = { code ->
                _connectionState.value = ConnectionState.Failed(P2pError.DiscoveryFailed(code))
            }
        )
    }

    override fun stopPeerDiscovery() {
        p2pDataSource.stopPeerDiscovery()
        if (_connectionState.value is ConnectionState.Discovering) {
            _connectionState.value = ConnectionState.Idle
        }
    }

    override fun connect(peer: Peer) {
        _connectionState.value = ConnectionState.Connecting(peer.deviceAddress)
        p2pDataSource.connect(
            deviceAddress = peer.deviceAddress,
            onSuccess = {},
            onFailure = { code ->
                _connectionState.value = ConnectionState.Failed(P2pError.ConnectionFailed(code))
            }
        )
    }

    override fun disconnect() {
        socketDataSource.closeSockets()
        p2pDataSource.disconnect(
            onSuccess = {
                _connectionState.value = ConnectionState.Idle
            },
            onFailure = {
                _connectionState.value = ConnectionState.Idle
            }
        )
    }
}
