package com.example.filetransfer.di

import android.content.Context
import androidx.room.Room
import com.example.filetransfer.data.local.AppDatabase
import com.example.filetransfer.data.local.TransferHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "p2p_file_transfer.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTransferHistoryDao(
        database: AppDatabase
    ): TransferHistoryDao {
        return database.transferHistoryDao()
    }
}
