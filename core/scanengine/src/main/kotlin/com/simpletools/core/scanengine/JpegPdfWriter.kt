package com.simpletools.core.scanengine

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

/**
 * Streams a multi-page PDF where every page is one JPEG image.
 *
 * Pages are written one at a time: each JPEG is copied as-is into the file (DCTDecode, no
 * re-encoding, no decoding to pixels) and only a few offsets are kept in memory, so memory
 * use does not grow with the number or the size of the photos.
 *
 * Usage: [addPage] for every page in order, then [finish]. [close] does not close [output].
 */
class JpegPdfWriter(output: OutputStream, private val title: String? = null) : Closeable {

    private val out = CountingOutputStream(output)
    private val offsets = mutableMapOf<Int, Long>()
    private val pageObjects = mutableListOf<Int>()
    private var nextObject = FIRST_FREE_OBJECT
    private var finished = false

    init {
        writeAscii("%PDF-1.4\n")
        // Binary comment: tells transfer tools that the file is binary.
        out.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
    }

    val pageCount: Int get() = pageObjects.size

    /**
     * Adds a page showing the JPEG read from [jpeg] (exactly [length] bytes), fitted on A4.
     * @throws IOException when the image is not a usable JPEG or the stream fails.
     */
    fun addPage(
        jpeg: InputStream,
        length: Long,
        info: JpegInfo,
        layout: PdfPageLayout = PdfPageLayout.fitA4(info.width, info.height),
        texts: List<PdfTextRun> = emptyList(),
    ) {
        check(!finished) { "PDF already finished" }
        require(length > 0) { "Empty image" }
        val colorSpace = if (info.components == 1) "/DeviceGray" else "/DeviceRGB"

        val image = beginObject()
        writeAscii(
            "<< /Type /XObject /Subtype /Image /Width ${info.width} /Height ${info.height} " +
                "/ColorSpace $colorSpace /BitsPerComponent 8 /Filter /DCTDecode /Length $length >>\nstream\n",
        )
        val copied = jpeg.copyTo(out, BUFFER_SIZE)
        if (copied != length) throw IOException("Image size changed while writing ($copied of $length bytes)")
        writeAscii("\nendstream\nendobj\n")

        val font = if (texts.isEmpty()) null else helveticaObject()
        val content = ByteArrayOutputStream()
        content.write(
            "q\n${n(layout.imageWidth)} 0 0 ${n(layout.imageHeight)} ${n(layout.imageX)} ${n(layout.imageY)} cm\n/Im0 Do\nQ\n"
                .toByteArray(Charsets.US_ASCII),
        )
        // Invisible text (render mode 3): searchable and selectable, never drawn over the image.
        for (run in texts) {
            content.write(
                "BT 3 Tr /F1 ${n(run.fontSize)} Tf ${n(run.horizontalScale)} Tz 1 0 0 1 ${n(run.x)} ${n(run.baseline)} Tm (".toByteArray(Charsets.US_ASCII),
            )
            for (byte in PdfTextLayer.encode(run.text)) {
                val code = byte.toInt() and 0xFF
                if (code == '('.code || code == ')'.code || code == '\\'.code) content.write('\\'.code)
                content.write(code)
            }
            content.write(") Tj ET\n".toByteArray(Charsets.US_ASCII))
        }
        val contentBytes = content.toByteArray()
        val contents = beginObject()
        writeAscii("<< /Length ${contentBytes.size} >>\nstream\n")
        out.write(contentBytes)
        writeAscii("\nendstream\nendobj\n")

        val fontResource = font?.let { " /Font << /F1 $it 0 R >>" }.orEmpty()
        val page = beginObject()
        writeAscii(
            "<< /Type /Page /Parent $PAGES_OBJECT 0 R /MediaBox [0 0 ${n(layout.pageWidth)} ${n(layout.pageHeight)}] " +
                "/Resources << /XObject << /Im0 $image 0 R >>$fontResource >> /Contents $contents 0 R >>\nendobj\n",
        )
        pageObjects += page
    }

    /** Writes the page tree, the cross-reference table and the trailer. */
    fun finish() {
        check(!finished) { "PDF already finished" }
        check(pageObjects.isNotEmpty()) { "A PDF needs at least one page" }
        finished = true

        beginObject(PAGES_OBJECT)
        writeAscii("<< /Type /Pages /Kids [${pageObjects.joinToString(" ") { "$it 0 R" }}] /Count ${pageObjects.size} >>\nendobj\n")

        beginObject(CATALOG_OBJECT)
        writeAscii("<< /Type /Catalog /Pages $PAGES_OBJECT 0 R >>\nendobj\n")

        val info = beginObject()
        val titleEntry = title?.takeIf { it.isNotBlank() }?.let { " /Title ${pdfText(it)}" }.orEmpty()
        writeAscii("<< /Producer (SimpleTools Scan)$titleEntry >>\nendobj\n")

        val xref = out.count
        val size = nextObject
        val table = StringBuilder("xref\n0 $size\n0000000000 65535 f \n")
        for (obj in 1 until size) {
            val offset = offsets[obj] ?: error("Object $obj was never written")
            table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset))
        }
        writeAscii(table.toString())
        writeAscii("trailer\n<< /Size $size /Root $CATALOG_OBJECT 0 R /Info $info 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.flush()
    }

    override fun close() {
        out.flush()
    }

    /** The Helvetica font object, written once, the first time a page carries text. */
    private fun helveticaObject(): Int = helvetica ?: run {
        val number = beginObject()
        writeAscii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
        helvetica = number
        number
    }

    private var helvetica: Int? = null

    private fun beginObject(number: Int = nextObject++): Int {
        offsets[number] = out.count
        writeAscii("$number 0 obj\n")
        return number
    }

    private fun writeAscii(text: String) = out.write(text.toByteArray(Charsets.US_ASCII))

    private class CountingOutputStream(target: OutputStream) : FilterOutputStream(target) {
        var count = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }

    companion object {
        private const val CATALOG_OBJECT = 1
        private const val PAGES_OBJECT = 2
        private const val FIRST_FREE_OBJECT = 3
        private const val BUFFER_SIZE = 64 * 1024

        /** PDF numbers never use a locale (a French decimal comma would corrupt the file). */
        internal fun n(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

        /** A PDF text string: UTF-16BE with BOM, as hex, valid for any characters. */
        internal fun pdfText(text: String): String {
            val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + text.toByteArray(Charsets.UTF_16BE)
            return bytes.joinToString(separator = "", prefix = "<", postfix = ">") { String.format(Locale.ROOT, "%02X", it) }
        }
    }
}
