package com.example.filetransfer.di

import android.content.Context
import com.example.filetransfer.data.p2p.WifiP2pDataSource
import com.example.filetransfer.data.repository.P2pRepositoryImpl
import com.example.filetransfer.data.socket.SocketDataSource
import com.example.filetransfer.domain.repository.P2pRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object P2pModule {

    @Provides
    @Singleton
    fun provideWifiP2pDataSource(
        @ApplicationContext context: Context
    ): WifiP2pDataSource {
        return WifiP2pDataSource(context)
    }

    @Provides
    @Singleton
    fun provideSocketDataSource(): SocketDataSource {
        return SocketDataSource()
    }

    @Provides
    @Singleton
    fun provideP2pRepository(
        @ApplicationContext context: Context,
        wifiP2pDataSource: WifiP2pDataSource,
        socketDataSource: SocketDataSource
    ): P2pRepository {
        return P2pRepositoryImpl(context, wifiP2pDataSource, socketDataSource)
    }
}
