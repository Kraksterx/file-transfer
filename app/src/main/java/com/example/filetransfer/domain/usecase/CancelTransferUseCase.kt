package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.repository.TransferRepository
import javax.inject.Inject

/** FT-10: batalkan transfer berjalan. */
class CancelTransferUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    operator fun invoke() = repository.cancel()
}
