package com.example.filetransfer.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filetransfer.domain.model.IncomingTransferRequest
import com.example.filetransfer.domain.usecase.ObserveIncomingTransferUseCase
import com.example.filetransfer.domain.usecase.RespondToTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomingTransferViewModel @Inject constructor(
    private val observeIncomingTransferUseCase: ObserveIncomingTransferUseCase,
    private val respondToTransferUseCase: RespondToTransferUseCase
) : ViewModel() {

    private val _pendingRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val pendingRequest: StateFlow<IncomingTransferRequest?> = _pendingRequest.asStateFlow()

    init {
        viewModelScope.launch {
            observeIncomingTransferUseCase().collect { request ->
                _pendingRequest.value = request
            }
        }
    }

    fun acceptRequest(request: IncomingTransferRequest) {
        _pendingRequest.value = null
        viewModelScope.launch {
            respondToTransferUseCase(request, accept = true)
        }
    }

    fun rejectRequest(request: IncomingTransferRequest) {
        _pendingRequest.value = null
        viewModelScope.launch {
            respondToTransferUseCase(request, accept = false)
        }
    }
}
