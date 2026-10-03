package com.example.filetransfer.data.file

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.filetransfer.domain.model.SelectedAttachment
import java.io.InputStream
import java.io.OutputStream

/**
 * FT-09: akses file tanpa izin storage.
 * - Baca: System Picker -> ContentResolver.openInputStream(uri)
 * - Simpan: MediaStore Downloads (API 29+), fallback Public/Downloads (<=28)
 */
class FileDataSource(
    context: Context
) {

    private val appContext = context.applicationContext
    private val resolver: ContentResolver get() = appContext.contentResolver

    /** Ubah Uri picker jadi SelectedAttachment (nama + ukuran asli, bukan lastPathSegment). */
    fun resolveAttachment(uri: Uri): SelectedAttachment {
        var name = uri.lastPathSegment ?: "file"
        var size = 0L
        var mime: String? = null
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
            mime = resolver.getType(uri)
        } catch (_: Exception) {
            // metadata tidak wajib; tetap lanjut dengan fallback
        }
        return SelectedAttachment(
            id = uri.toString(),
            uri = uri.toString(),
            name = name,
            sizeBytes = size,
            mimeType = mime
        )
    }

    fun openInputStream(attachment: SelectedAttachment): InputStream =
        resolver.openInputStream(Uri.parse(attachment.uri))
            ?: throw java.io.IOException("Gagal membuka ${attachment.name}")

    /** Simpan file masuk ke folder Download tanpa izin storage. */
    fun createDownloadStream(displayName: String, mimeType: String?): OutputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, uniqueDisplayName(displayName))
                put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item = resolver.insert(collection, values)
                ?: throw java.io.IOException("Gagal membuat entri MediaStore")
            resolver.openOutputStream(item)
                ?: throw java.io.IOException("Gagal membuka output MediaStore")
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            java.io.FileOutputStream(java.io.File(dir, uniqueDisplayName(displayName)))
        }
    }

    /** Tandai entri MediaStore siap dibaca pengguna setelah stream ditutup (API 29+). */
    fun finalizeDownload(displayName: String, mimeType: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        resolver.update(
            collection,
            values,
            selection,
            arrayOf(uniqueDisplayName(displayName))
        )
    }

    private fun uniqueDisplayName(name: String): String {
        val stamp = System.currentTimeMillis()
        val dot = name.lastIndexOf('.')
        return if (dot > 0) {
            "${name.substring(0, dot)}_$stamp${name.substring(dot)}"
        } else {
            "${name}_$stamp"
        }
    }
}
