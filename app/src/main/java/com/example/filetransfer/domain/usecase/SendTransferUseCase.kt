package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.repository.TransferRepository
import java.util.UUID

/**
 * FT-05 sisi pengirim: buat transferId, kirim REQUEST, tunggu respons,
 * lalu stream file. Progres dibaca dari repository (FT-10).
 */
class SendTransferUseCase(
    private val repository: TransferRepository
) {
    suspend operator fun invoke(
        senderName: String,
        peerName: String,
        attachments: List<SelectedAttachment>,
        message: String,
        transferId: String = UUID.randomUUID().toString()
    ): Result<Unit> = repository.sendTransfer(
        transferId = transferId,
        senderName = senderName,
        peerName = peerName,
        attachments = attachments,
        message = message
    )
}
