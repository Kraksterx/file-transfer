package com.example.filetransfer.domain.repository

import com.example.filetransfer.domain.model.ConnectionState
import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.model.PrerequisiteState
import kotlinx.coroutines.flow.Flow

interface P2pRepository {
    val prerequisitesState: Flow<PrerequisiteState>
    val connectionState: Flow<ConnectionState>
    val peers: Flow<List<Peer>>

    fun discoverPeers()
    fun stopPeerDiscovery()
    fun connect(peer: Peer)
    fun disconnect()
}
