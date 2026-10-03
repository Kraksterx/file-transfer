package com.example.filetransfer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferId: String,
    val peerName: String,
    val direction: String, // "SENT" or "RECEIVED"
    val message: String,
    val fileCount: Int,
    val totalSizeBytes: Long,
    val fileNamesJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
