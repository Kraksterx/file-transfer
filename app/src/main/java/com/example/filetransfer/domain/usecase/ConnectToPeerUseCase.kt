package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.model.Peer
import com.example.filetransfer.domain.repository.P2pRepository
import javax.inject.Inject

class ConnectToPeerUseCase @Inject constructor(
    private val repository: P2pRepository
) {
    operator fun invoke(peer: Peer) {
        repository.connect(peer)
    }
}
