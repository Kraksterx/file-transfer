package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.repository.P2pRepository
import javax.inject.Inject

class DiscoverPeersUseCase @Inject constructor(
    private val repository: P2pRepository
) {
    operator fun invoke() {
        repository.discoverPeers()
    }
}
