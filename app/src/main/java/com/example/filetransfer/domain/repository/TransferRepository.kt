package com.example.filetransfer.domain.repository

import com.example.filetransfer.domain.model.CompletedTransfer
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Kontrak transfer file (FT-05, FT-08, FT-09, FT-10).
 * Implementasi: data/repository/TransferRepositoryImpl
 */
interface TransferRepository {

    /** Progres per file, key = SelectedAttachment.id (FT-10). */
    val progress: StateFlow<Map<String, TransferProgress>>

    /** Permintaan transfer masuk dari peer (FT-05, sisi penerima). */
    fun observeIncomingRequests(): Flow<IncomingTransferRequest>

    /** Transfer yang selesai (sukses), untuk disimpan ke riwayat (FT-11). */
    val completedTransfers: SharedFlow<CompletedTransfer>

    /**
     * Kirim satu paket transfer: tulis REQUEST, tunggu RESPONSE,
     * lalu stream semua file bila diterima.
     */
    suspend fun sendTransfer(
        transferId: String,
        senderName: String,
        peerName: String,
        attachments: List<SelectedAttachment>,
        message: String
    ): Result<Unit>

    /** Jawab permintaan masuk: true = terima (lalu stream file), false = tolak. */
    suspend fun respondToRequest(
        request: IncomingTransferRequest,
        accept: Boolean
    ): Result<Unit>

    /** Batalkan transfer yang sedang berjalan (FT-10). */
    fun cancel()
}
