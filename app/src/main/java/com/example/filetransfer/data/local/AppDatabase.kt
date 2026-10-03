package com.example.filetransfer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransferRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferHistoryDao
}
