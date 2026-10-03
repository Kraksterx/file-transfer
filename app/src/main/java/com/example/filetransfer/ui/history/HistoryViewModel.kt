package com.example.filetransfer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filetransfer.data.local.TransferHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: TransferHistoryDao
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = historyDao.getAllRecords()
        .map { records ->
            HistoryUiState(records = records, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = true)
        )

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            historyDao.deleteRecord(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }
}
