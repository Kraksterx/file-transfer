package com.example.filetransfer.ui.history

import com.example.filetransfer.data.local.TransferRecordEntity

data class HistoryUiState(
    val records: List<TransferRecordEntity> = emptyList(),
    val isLoading: Boolean = false
)
