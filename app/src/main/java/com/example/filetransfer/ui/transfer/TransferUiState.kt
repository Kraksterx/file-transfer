package com.example.filetransfer.ui.transfer

import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferStatus

/**
 * UiState layar Transfer (UDF: StateFlow di ViewModel -> UI).
 * FT-05 (compose), FT-10 (progres per fileId).
 */
data class TransferUiState(
    val peerName: String = "Samsung A54",
    val attachments: List<SelectedAttachment> = emptyList(),
    val message: String = "",
    val status: TransferStatus = TransferStatus.IDLE,
    val progressFractions: Map<String, Float> = emptyMap(),
    val errorMessage: String? = null
) {
    val canSend: Boolean
        get() = attachments.isNotEmpty() && status != TransferStatus.SENDING

    fun progressFor(fileId: String): Float = progressFractions[fileId] ?: 0f
}
