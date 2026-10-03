package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * FT-05 sisi penerima: balas permintaan dengan ACCEPT atau REJECT.
 * Kalau accept=true, repository sekaligus menerima & menyimpan file.
 */
class RespondToTransferUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    suspend operator fun invoke(
        request: IncomingTransferRequest,
        accept: Boolean
    ): Result<Unit> = repository.respondToRequest(request, accept)
}
