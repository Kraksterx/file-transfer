package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * FT-05 sisi penerima: requests yang masuk, untuk dialog global di ui/root/.
 */
class ObserveIncomingTransferUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    operator fun invoke(): Flow<IncomingTransferRequest> =
        repository.observeIncomingRequests()
}
