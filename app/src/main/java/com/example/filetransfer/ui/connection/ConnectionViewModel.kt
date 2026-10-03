package com.example.filetransfer.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filetransfer.domain.model.ConnectionState
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PrerequisiteState
import com.example.filetransfer.domain.repository.P2pRepository
import com.example.filetransfer.domain.usecase.ConnectToPeerUseCase
import com.example.filetransfer.domain.usecase.DisconnectUseCase
import com.example.filetransfer.domain.usecase.DiscoverPeersUseCase
import com.example.filetransfer.domain.usecase.ObservePrerequisitesUseCase
import com.example.filetransfer.domain.usecase.RefreshPrerequisitesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    observePrerequisitesUseCase: ObservePrerequisitesUseCase,
    private val discoverPeersUseCase: DiscoverPeersUseCase,
    private val connectToPeerUseCase: ConnectToPeerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val refreshPrerequisitesUseCase: RefreshPrerequisitesUseCase,
    repository: P2pRepository
) : ViewModel() {

    val prerequisiteState: StateFlow<PrerequisiteState> = observePrerequisitesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PrerequisiteState()
        )

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Idle
        )

    val peers: StateFlow<List<Peer>> = repository.peers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun discoverPeers() {
        discoverPeersUseCase()
    }

    fun connect(peer: Peer) {
        connectToPeerUseCase(peer)
    }

    fun disconnect() {
        disconnectUseCase()
    }

    /** Cek ulang prasyarat (dipanggil tiap ON_RESUME agar banner reaktif). */
    fun refreshPrerequisites() {
        refreshPrerequisitesUseCase()
    }
}
