package com.example.filetransfer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryDao {

    @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TransferRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TransferRecordEntity)

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM transfer_records")
    suspend fun clearAll()
}
