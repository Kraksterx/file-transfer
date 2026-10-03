package com.example.filetransfer.domain.model

/**
 * Satu file yang dipilih pengirim via System Picker (FT-09).
 * uri disimpan sebagai String agar domain layer bebas Android
 * (bisa dipakai unit test JVM); konversi ke Uri di data layer.
 */
data class SelectedAttachment(
    val id: String,
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String? = null
)

/** Meta file untuk dialog permintaan masuk (sisi penerima, FT-05). */
data class IncomingFileMeta(
    val name: String,
    val sizeBytes: Long
)

/** Status layar transfer sisi pengirim (wireframe 2.4 - 2.7). */
enum class TransferStatus {
    IDLE,
    SENDING,
    SUCCESS,
    FAILED
}

/** Progres per file (FT-10). Key = SelectedAttachment.id. */
data class TransferProgress(
    val fileId: String,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 1L
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

/**
 * Permintaan transfer masuk dari peer (FT-05 dua-fase).
 * Di-host di level root/activity agar tidak terlewat (PRD asumsi #1).
 */
data class IncomingTransferRequest(
    val transferId: String,
    val senderName: String,
    val message: String = "",
    val files: List<IncomingFileMeta> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)
