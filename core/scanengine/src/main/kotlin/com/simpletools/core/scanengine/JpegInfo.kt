package com.simpletools.core.scanengine

import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/** Size and color components of a JPEG, read from its frame header without decoding pixels. */
data class JpegInfo(val width: Int, val height: Int, val components: Int) {

    companion object {
        /** @throws IOException when [input] is not a JPEG this writer can embed. */
        fun read(input: InputStream): JpegInfo {
            val data = DataInputStream(input.buffered())
            try {
                if (data.readUnsignedByte() != 0xFF || data.readUnsignedByte() != 0xD8) throw IOException("Not a JPEG file")
                while (true) {
                    var marker = data.readUnsignedByte()
                    if (marker != 0xFF) throw IOException("Corrupted JPEG")
                    while (marker == 0xFF) marker = data.readUnsignedByte()
                    if (marker == 0xD9 || marker == 0xDA) throw IOException("JPEG has no frame header")
                    if (marker in 0xD0..0xD7 || marker == 0x01) continue
                    val length = data.readUnsignedShort()
                    if (length < 2) throw IOException("Corrupted JPEG")
                    val isFrame = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
                    if (isFrame) {
                        data.readUnsignedByte() // precision
                        val height = data.readUnsignedShort()
                        val width = data.readUnsignedShort()
                        val components = data.readUnsignedByte()
                        if (width == 0 || height == 0) throw IOException("JPEG has no size")
                        if (components != 1 && components != 3) throw IOException("Unsupported JPEG color components: $components")
                        return JpegInfo(width, height, components)
                    }
                    data.skipFully(length - 2)
                }
            } catch (e: EOFException) {
                throw IOException("Truncated JPEG", e)
            }
        }

        private fun DataInputStream.skipFully(count: Int) {
            var remaining = count
            while (remaining > 0) {
                val skipped = skipBytes(remaining)
                if (skipped <= 0) {
                    readUnsignedByte()
                    remaining--
                } else {
                    remaining -= skipped
                }
            }
        }
    }
}
