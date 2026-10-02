package com.example.filetransfer.domain.model

/**
 * Hasil akhir satu paket transfer (FT-05).
 * Dikonsumsi HistoryViewModel (Rani, FT-11) untuk disimpan ke Room
 * sebagai satu TransferRecord per transfer, bukan per file (PRD #2).
 */
data class CompletedTransfer(
    val transferId: String,
    val peerName: String,
    val direction: Direction,
    val message: String = "",
    val files: List<IncomingFileMeta> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
) {
    enum class Direction {
        SENT,
        RECEIVED
    }
}
