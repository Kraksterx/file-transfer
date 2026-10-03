package com.example.filetransfer.data.socket

import com.example.filetransfer.domain.model.IncomingFileMeta
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * FT-08: uji round-trip MessageCodec lewat loopback streams.
 * Tidak butuh perangkat kedua atau socket sungguhan.
 */
class MessageCodecTest {

    @Test
    fun requestFrame_roundTrip() {
        val out = ByteArrayOutputStream()
        val original = MessageCodec.RequestFrame(
            transferId = "transfer-1",
            senderName = "Pixel 7",
            message = "Oke, ini ya",
            files = listOf(
                IncomingFileMeta("foto_kelas.jpg", 4_200_000),
                IncomingFileMeta("tugas_pbo.pdf", 1_048_576)
            )
        )

        MessageCodec.writeRequest(out, original)
        val decoded = MessageCodec.readRequest(ByteArrayInputStream(out.toByteArray()))

        assertEquals(original, decoded)
    }

    @Test
    fun requestFrame_pejabatDanNamaFilePanjang() {
        val out = ByteArrayOutputStream()
        val original = MessageCodec.RequestFrame(
            transferId = "abc",
            senderName = "Redmi Note 12",
            message = "berkas",
            files = listOf(IncomingFileMeta("nama-yang-panjang-sekali.zip", 10))
        )

        MessageCodec.writeRequest(out, original)
        val decoded = MessageCodec.readRequest(ByteArrayInputStream(out.toByteArray()))

        assertEquals("nama-yang-panjang-sekali.zip", decoded.files.first().name)
    }

    @Test
    fun responseFrame_roundTrip_acceptDanReject() {
        listOf(true, false).forEach { accepted ->
            val out = ByteArrayOutputStream()
            MessageCodec.writeResponse(
                out,
                MessageCodec.ResponseFrame("transfer-1", accepted)
            )
            val decoded = MessageCodec.readResponse(ByteArrayInputStream(out.toByteArray()))
            assertEquals("transfer-1", decoded.transferId)
            assertEquals(accepted, decoded.accepted)
        }
    }

    @Test
    fun fileHeader_roundTrip() {
        val out = ByteArrayOutputStream()
        MessageCodec.writeFileHeader(
            out,
            MessageCodec.FileHeaderFrame(
                transferId = "t1",
                fileName = "materi.zip",
                fileSize = 12_000_000,
                mimeType = "application/zip"
            )
        )
        val decoded = MessageCodec.readFileHeader(ByteArrayInputStream(out.toByteArray()))
        assertEquals("materi.zip", decoded.fileName)
        assertEquals(12_000_000L, decoded.fileSize)
        assertEquals("application/zip", decoded.mimeType)
    }

    @Test
    fun chunk_bisaDirangkaiLebihDariSatuChunk() {
        val out = ByteArrayOutputStream()
        val payload = ByteArray(MessageCodec.CHUNK_SIZE * 2 + 7) { (it % 256).toByte() }
        val buffer = ByteArray(MessageCodec.CHUNK_SIZE)

        // tulis 2 chunk penuh + sisa
        var offset = 0
        repeat(2) {
            payload.copyInto(
                buffer,
                destinationOffset = 0,
                startIndex = offset,
                endIndex = offset + MessageCodec.CHUNK_SIZE
            )
            MessageCodec.writeChunk(out, "t1", buffer, buffer.size)
            offset += MessageCodec.CHUNK_SIZE
        }
        val tail = payload.copyOfRange(offset, payload.size)
        MessageCodec.writeChunk(out, "t1", tail, tail.size)
        MessageCodec.writeFileEnd(out, "t1")

        val stream = ByteArrayInputStream(out.toByteArray())
        val rebuilt = java.io.ByteArrayOutputStream()
        while (MessageCodec.readFrameType(stream) != MessageCodec.FRAME_FILE_END) {
            val (_, chunk) = MessageCodec.readChunkBody(stream)
            rebuilt.write(chunk)
        }

        assertEquals(payload.size, rebuilt.size())
        assertArrayEquals(payload, rebuilt.toByteArray())
    }

    @Test
    fun fileEnd_terbacaSebagaiTipeFrame() {
        val out = ByteArrayOutputStream()
        MessageCodec.writeFileEnd(out, "t1")
        val stream = ByteArrayInputStream(out.toByteArray())
        assertEquals(MessageCodec.FRAME_FILE_END, MessageCodec.readFrameType(stream))
    }

    @Test(expected = MessageCodec.CodecException::class)
    fun tipeFrameTidakDikenal_ditolak() {
        val out = ByteArrayOutputStream()
        java.io.DataOutputStream(out).writeInt(99)
        MessageCodec.readFrameType(ByteArrayInputStream(out.toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun writeRequest_melebihiBatasFile_ditolak() {
        val files = (1..101).map { IncomingFileMeta("f$it", 1) }
        MessageCodec.writeRequest(
            ByteArrayOutputStream(),
            MessageCodec.RequestFrame("t", "s", "m", files)
        )
    }

    @Test
    fun chunkNolPanjang_tetapValid() {
        val out = ByteArrayOutputStream()
        MessageCodec.writeChunk(out, "t1", ByteArray(0), 0)
        val stream = ByteArrayInputStream(out.toByteArray())
        assertEquals(MessageCodec.FRAME_FILE_CHUNK, MessageCodec.readFrameType(stream))
        val (_, chunk) = MessageCodec.readChunkBody(stream)
        assertTrue(chunk.isEmpty())
    }
}
