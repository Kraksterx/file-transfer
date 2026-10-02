package com.example.filetransfer.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferProgress
import com.example.filetransfer.domain.model.TransferStatus
import com.example.filetransfer.domain.usecase.CancelTransferUseCase
import com.example.filetransfer.domain.usecase.SendTransferUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel layar Transfer (FT-05, FT-10).
 * Progres per file diambil dari TransferRepository.progress (StateFlow).
 * Selalu instantiate lewat [Factory] agar bisa DI dari AppContainer.
 */
class TransferViewModel(
    private val sendTransfer: SendTransferUseCase,
    private val cancelTransfer: CancelTransferUseCase,
    private val progress: StateFlow<Map<String, TransferProgress>>,
    private val fileDataSource: com.example.filetransfer.data.file.FileDataSource,
    private val senderName: String = "Pixel 7"
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var sendingJob: Job? = null

    init {
        viewModelScope.launch {
            progress.collect { map ->
                _uiState.update { state ->
                    state.copy(progressFractions = map.mapValues { (_, p) -> p.fraction })
                }
            }
        }
    }

    fun onMessageChange(value: String) {
        _uiState.update { it.copy(message = value) }
    }

    fun addAttachments(files: List<SelectedAttachment>) {
        if (files.isEmpty()) return
        _uiState.update { state ->
            val existing = state.attachments.map { it.id }.toSet()
            state.copy(attachments = state.attachments + files.filterNot { existing.contains(it.id) })
        }
    }

    /** Resolve Uri picker via FileDataSource (FT-09); Screen hanya teruskan Uri. */
    fun addUris(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        addAttachments(uris.map { fileDataSource.resolveAttachment(it) })
    }

    fun removeAttachment(fileId: String) {
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments.filterNot { it.id == fileId },
                progressFractions = state.progressFractions - fileId
            )
        }
    }

    fun send() {
        val current = _uiState.value
        if (!current.canSend) return

        sendingJob?.cancel()
        _uiState.update {
            it.copy(
                status = TransferStatus.SENDING,
                errorMessage = null,
                progressFractions = it.attachments.associate { a -> a.id to 0f }
            )
        }

        sendingJob = viewModelScope.launch {
            val result = sendTransfer(
                senderName = senderName,
                peerName = _uiState.value.peerName,
                attachments = _uiState.value.attachments,
                message = _uiState.value.message
            )
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(status = TransferStatus.SUCCESS) },
                    onFailure = { e ->
                        state.copy(
                            status = TransferStatus.FAILED,
                            errorMessage = e.message ?: "Transfer gagal"
                        )
                    }
                )
            }
        }
    }

    fun cancelSending() {
        sendingJob?.cancel()
        cancelTransfer()
        backToCompose()
    }

    fun backToCompose() {
        sendingJob?.cancel()
        _uiState.update {
            it.copy(
                status = TransferStatus.IDLE,
                progressFractions = emptyMap(),
                errorMessage = null
            )
        }
    }

    fun retry() {
        backToCompose()
        send()
    }

    /** DI manual: ViewModel di-inject lewat factory (AppContainer). */
    class Factory(
        private val sendTransfer: SendTransferUseCase,
        private val cancelTransfer: CancelTransferUseCase,
        private val progress: StateFlow<Map<String, TransferProgress>>,
        private val fileDataSource: com.example.filetransfer.data.file.FileDataSource,
        private val senderName: String = "Pixel 7"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TransferViewModel(sendTransfer, cancelTransfer, progress, fileDataSource, senderName) as T
    }
}
