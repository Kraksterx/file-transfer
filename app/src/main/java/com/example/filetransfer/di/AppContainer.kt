package com.example.filetransfer.di

import android.content.Context
import com.example.filetransfer.data.file.FileDataSource
import com.example.filetransfer.data.p2p.WifiP2pDataSource
import com.example.filetransfer.data.repository.TransferRepositoryImpl
import com.example.filetransfer.data.socket.TransferSessionProvider
import com.example.filetransfer.data.socket.UnavailableSessionProvider
import com.example.filetransfer.domain.repository.TransferRepository
import com.example.filetransfer.domain.usecase.CancelTransferUseCase
import com.example.filetransfer.domain.usecase.ObserveIncomingTransferUseCase
import com.example.filetransfer.domain.usecase.RespondToTransferUseCase
import com.example.filetransfer.domain.usecase.SendTransferUseCase

/**
 * DI manual (PRD: AppContainer, Hilt opsional).
 * Semua repository berumur aplikasi (singleton).
 */
class AppContainer(
    context: Context,
    // Ganti ke SocketDataSourceFaqih setelah FT-04 masuk.
    sessionProvider: TransferSessionProvider = UnavailableSessionProvider()
) {

    private val appContext = context.applicationContext

    val wifiP2pDataSource = WifiP2pDataSource(context = appContext)

    val fileDataSource = FileDataSource(appContext)

    private val transferSessionProvider = sessionProvider

    val transferRepository: TransferRepository = TransferRepositoryImpl(
        sessionProvider = transferSessionProvider,
        fileDataSource = fileDataSource
    )

    val sendTransferUseCase = SendTransferUseCase(transferRepository)
    val observeIncomingTransferUseCase = ObserveIncomingTransferUseCase(transferRepository)
    val respondToTransferUseCase = RespondToTransferUseCase(transferRepository)
    val cancelTransferUseCase = CancelTransferUseCase(transferRepository)
}
