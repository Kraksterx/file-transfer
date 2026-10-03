package com.example.filetransfer.data.socket

import com.example.filetransfer.domain.model.IncomingFileMeta
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Protokol transfer biner v1 (FT-08).
 *
 * Kontrak bareng Faqih (SocketDataSource). Selama SocketDataSource
 * belum ada, engine ini bisa diuji lewat loopback ByteArray streams.
 *
 * Frame (big-endian, ditulis via DataOutputStream):
 * ```
 * REQUEST     : [FRAME_REQUEST=1][version Int][transferId UTF][senderName UTF]
 *               [message UTF][fileCount Int][{fileName UTF, fileSize Long} * N]
 * RESPONSE    : [FRAME_RESPONSE=2][transferId UTF][accepted Boolean]
 * FILE_HEADER : [FRAME_FILE_HEADER=3][transferId UTF][fileName UTF]
 *               [fileSize Long][mimeType UTF]
 * FILE_CHUNK  : [FRAME_FILE_CHUNK=4][transferId UTF][len Int][bytes[len]]
 * FILE_END    : [FRAME_FILE_END=5][transferId UTF]
 * ```
 * Alur dua-fase (FT-05): pengirim -> REQUEST, penerima -> RESPONSE.
 * Bila accepted=true, pengirim lanjut [FILE_HEADER, FILE_CHUNK*, FILE_END] per file.
 */
object MessageCodec {

    const val PROTOCOL_VERSION = 1
    const val FRAME_REQUEST = 1
    const val FRAME_RESPONSE = 2
    const val FRAME_FILE_HEADER = 3
    const val FRAME_FILE_CHUNK = 4
    const val FRAME_FILE_END = 5

    /** Batas aman agar satu chunk tidak membebani memori. */
    const val CHUNK_SIZE = 32 * 1024

    private const val MAX_FILES = 100
    private const val MAX_FRAME_SIZE = CHUNK_SIZE * 4

    data class RequestFrame(
        val transferId: String,
        val senderName: String,
        val message: String,
        val files: List<IncomingFileMeta>
    )

    data class ResponseFrame(
        val transferId: String,
        val accepted: Boolean
    )

    data class FileHeaderFrame(
        val transferId: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String = ""
    )

    fun writeRequest(out: OutputStream, frame: RequestFrame) {
        require(frame.files.size <= MAX_FILES) { "Maksimal $MAX_FILES file per transfer" }
        val data = DataOutputStream(out)
        data.writeInt(FRAME_REQUEST)
        data.writeInt(PROTOCOL_VERSION)
        data.writeUTF(frame.transferId)
        data.writeUTF(frame.senderName)
        data.writeUTF(frame.message)
        data.writeInt(frame.files.size)
        frame.files.forEach { f ->
            data.writeUTF(f.name)
            data.writeLong(f.sizeBytes)
        }
        data.flush()
    }

    fun readRequest(`in`: InputStream): RequestFrame {
        val data = DataInputStream(`in`)
        val type = try {
            data.readInt()
        } catch (e: EOFException) {
            throw CodecException("Stream habis saat baca frame REQUEST", e)
        }
        if (type != FRAME_REQUEST) throw CodecException("Frame bukan REQUEST (dapat $type)")
        return readRequestBody(data)
    }

    /**
     * Baca body REQUEST. Tipe frame SUDAH harus dikonsumsi lebih dulu
     * lewat [readFrameType]; dipakai read loop single-reader.
     */
    fun readRequestBody(`in`: InputStream): RequestFrame {
        val data = dataInputOf(`in`)
        data.readInt() // version: dibaca, ditoleransi untuk kompatibilitas maju
        val transferId = data.readUTF()
        val senderName = data.readUTF()
        val message = data.readUTF()
        val count = data.readInt()
        if (count !in 0..MAX_FILES) throw CodecException("fileCount tidak wajar: $count")
        val files = (0 until count).map {
            IncomingFileMeta(name = data.readUTF(), sizeBytes = data.readLong())
        }
        return RequestFrame(transferId, senderName, message, files)
    }

    fun writeResponse(out: OutputStream, frame: ResponseFrame) {
        val data = DataOutputStream(out)
        data.writeInt(FRAME_RESPONSE)
        data.writeUTF(frame.transferId)
        data.writeBoolean(frame.accepted)
        data.flush()
    }

    fun readResponse(`in`: InputStream): ResponseFrame {
        val data = DataInputStream(`in`)
        val type = data.readInt()
        if (type != FRAME_RESPONSE) throw CodecException("Frame bukan RESPONSE (dapat $type)")
        return readResponseBody(data)
    }

    /** Baca body RESPONSE; tipe frame sudah dikonsumsi via [readFrameType]. */
    fun readResponseBody(`in`: InputStream): ResponseFrame {
        val data = dataInputOf(`in`)
        return ResponseFrame(transferId = data.readUTF(), accepted = data.readBoolean())
    }

    fun writeFileHeader(out: OutputStream, frame: FileHeaderFrame) {
        val data = DataOutputStream(out)
        data.writeInt(FRAME_FILE_HEADER)
        data.writeUTF(frame.transferId)
        data.writeUTF(frame.fileName)
        data.writeLong(frame.fileSize)
        data.writeUTF(frame.mimeType)
        data.flush()
    }

    fun readFileHeader(`in`: InputStream): FileHeaderFrame {
        val data = DataInputStream(`in`)
        val type = data.readInt()
        if (type != FRAME_FILE_HEADER) throw CodecException("Frame bukan FILE_HEADER (dapat $type)")
        return readFileHeaderBody(data)
    }

    /** Baca body FILE_HEADER; tipe frame sudah dikonsumsi via [readFrameType]. */
    fun readFileHeaderBody(`in`: InputStream): FileHeaderFrame {
        val data = dataInputOf(`in`)
        return FileHeaderFrame(
            transferId = data.readUTF(),
            fileName = data.readUTF(),
            fileSize = data.readLong(),
            mimeType = data.readUTF()
        )
    }

    /** Tulis satu chunk; [len] harus <= buffer.size. */
    fun writeChunk(out: OutputStream, transferId: String, buffer: ByteArray, len: Int) {
        require(len in 0..buffer.size) { "len di luar ukuran buffer" }
        val data = DataOutputStream(out)
        data.writeInt(FRAME_FILE_CHUNK)
        data.writeUTF(transferId)
        data.writeInt(len)
        if (len > 0) data.write(buffer, 0, len)
        data.flush()
    }

    /**
     * Baca payload satu chunk. Tipe frame SUDAH harus dikonsumsi lebih dulu
     * lewat [readFrameType]; fungsi ini hanya membaca sisanya.
     * @return (transferId, payload)
     */
    fun readChunkBody(`in`: InputStream, maxChunk: Int = CHUNK_SIZE): Pair<String, ByteArray> {
        val data = DataInputStream(`in`)
        val transferId = data.readUTF()
        val len = data.readInt()
        if (len !in 0..MAX_FRAME_SIZE) throw CodecException("Panjang chunk tidak wajar: $len")
        val buf = ByteArray(len)
        if (len > 0) data.readFully(buf)
        return transferId to buf
    }

    fun writeFileEnd(out: OutputStream, transferId: String) {
        val data = DataOutputStream(out)
        data.writeInt(FRAME_FILE_END)
        data.writeUTF(transferId)
        data.flush()
    }

    /** Baca body FILE_END (transferId); tipe frame sudah dikonsumsi via [readFrameType]. */
    fun readFileEndBody(`in`: InputStream): String = dataInputOf(`in`).readUTF()

    /** Baca tipe frame berikutnya; dipakai engine untuk dispatch. */
    fun readFrameType(`in`: InputStream): Int {
        val type = try {
            DataInputStream(`in`).readInt()
        } catch (e: EOFException) {
            throw CodecException("Stream habis saat baca tipe frame", e)
        }
        if (type < FRAME_REQUEST || type > FRAME_FILE_END) {
            throw CodecException("Tipe frame tidak dikenal: $type")
        }
        return type
    }

    class CodecException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private fun dataInputOf(`in`: InputStream): DataInputStream =
        `in` as? DataInputStream ?: DataInputStream(`in`)
}
