package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.repository.TransferRepository

/** FT-10: batalkan transfer berjalan. */
class CancelTransferUseCase(
    private val repository: TransferRepository
) {
    operator fun invoke() = repository.cancel()
}
