package com.example.filetransfer.di

import android.content.Context
import com.example.filetransfer.data.p2p.WifiP2pDataSource
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
}
