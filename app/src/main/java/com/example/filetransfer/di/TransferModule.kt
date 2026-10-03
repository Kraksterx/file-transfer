package com.example.filetransfer.di

import android.content.Context
import com.example.filetransfer.data.file.FileDataSource
import com.example.filetransfer.data.repository.TransferRepositoryImpl
import com.example.filetransfer.data.socket.TransferSessionProvider
import com.example.filetransfer.data.socket.UnavailableSessionProvider
import com.example.filetransfer.domain.repository.TransferRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransferModule {

    @Provides
    @Singleton
    fun provideFileDataSource(
        @ApplicationContext context: Context
    ): FileDataSource {
        return FileDataSource(context)
    }

    @Provides
    @Singleton
    fun provideTransferSessionProvider(): TransferSessionProvider {
        return UnavailableSessionProvider()
    }

    @Provides
    @Singleton
    fun provideTransferRepository(
        sessionProvider: TransferSessionProvider,
        fileDataSource: FileDataSource
    ): TransferRepository {
        return TransferRepositoryImpl(
            sessionProvider = sessionProvider,
            fileDataSource = fileDataSource
        )
    }
}
