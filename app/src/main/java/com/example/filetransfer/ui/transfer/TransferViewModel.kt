package com.example.filetransfer.ui.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filetransfer.data.file.FileDataSource
import com.example.filetransfer.domain.model.SelectedAttachment
import com.example.filetransfer.domain.model.TransferStatus
import com.example.filetransfer.domain.repository.TransferRepository
import com.example.filetransfer.domain.usecase.CancelTransferUseCase
import com.example.filetransfer.domain.usecase.SendTransferUseCase
import com.example.filetransfer.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel layar Transfer (FT-05, FT-10).
 * Progres per file diambil dari TransferRepository.progress (StateFlow).
 * Hilt Injected.
 */
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val sendTransfer: SendTransferUseCase,
    private val cancelTransfer: CancelTransferUseCase,
    private val transferRepository: TransferRepository,
    private val fileDataSource: FileDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransferUiState(
            peerName = savedStateHandle.get<String>(Screen.Transfer.ARG_PEER_NAME).orEmpty()
        )
    )
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val senderName: String = "Pixel 7"
    private var sendingJob: Job? = null

    init {
        viewModelScope.launch {
            transferRepository.progress.collect { map ->
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
}
