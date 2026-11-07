package com.simpletools.core.scanengine

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** A PDF with an invisible text layer must be searchable, and its image must stay the same. */
class SearchablePdfTest {

    private fun jpeg(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
    }

    private fun page(width: Int, height: Int, lines: List<List<OcrWord>>) = OcrPage(
        width = width,
        height = height,
        lines = lines.map { OcrLine(it) },
    )

    private fun extractText(pdf: ByteArray): String =
        Loader.loadPDF(pdf).use { doc -> PDFTextStripper().getText(doc) }

    @Test
    fun `the words of an ocr page are found in the pdf text`() {
        val bytes = jpeg(800, 1100)
        val ocr = page(
            width = 800,
            height = 1100,
            lines = listOf(
                listOf(OcrWord("Bonjour", 80, 100, 300, 150), OcrWord("monde", 320, 100, 520, 150)),
                listOf(OcrWord("Facture", 80, 300, 350, 350)),
            ),
        )
        val out = ByteArrayOutputStream()
        JpegPdfWriter(out, "test").use { writer ->
            val info = JpegInfo.read(ByteArrayInputStream(bytes))
            val layout = PdfPageLayout.fitA4(info.width, info.height)
            writer.addPage(ByteArrayInputStream(bytes), bytes.size.toLong(), info, layout, PdfTextLayer.runsFor(ocr, layout))
            writer.finish()
        }
        val text = extractText(out.toByteArray())
        assertTrue("text was: $text", text.contains("Bonjour"))
        assertTrue("text was: $text", text.contains("monde"))
        assertTrue("text was: $text", text.contains("Facture"))
    }

    @Test
    fun `a pdf without text stays a plain image pdf`() {
        val bytes = jpeg(200, 300)
        val out = ByteArrayOutputStream()
        JpegPdfWriter(out).use { writer ->
            writer.addPage(ByteArrayInputStream(bytes), bytes.size.toLong(), JpegInfo.read(ByteArrayInputStream(bytes)))
            writer.finish()
        }
        assertEquals("", extractText(out.toByteArray()).trim())
    }

    @Test
    fun `runs are placed inside the image area of the page`() {
        val layout = PdfPageLayout.fitA4(800, 1100)
        val runs = PdfTextLayer.runsFor(page(800, 1100, listOf(listOf(OcrWord("Test", 0, 0, 800, 1100)))), layout)
        val run = runs.single()
        assertEquals(layout.imageX, run.x, 1e-6)
        assertTrue(run.baseline >= layout.imageY - 1e-6)
        assertTrue(run.horizontalScale > 0)
    }

    @Test
    fun `characters outside WinAnsi are replaced, never corrupt the file`() {
        assertEquals("a?b", PdfTextLayer.sanitize("a中b"))
        assertEquals("été", PdfTextLayer.sanitize("été"))
    }

    @Test
    fun `brackets and backslashes are escaped in the content stream`() {
        val bytes = jpeg(100, 100)
        val ocr = page(100, 100, listOf(listOf(OcrWord("(x)\\", 10, 10, 90, 40))))
        val out = ByteArrayOutputStream()
        JpegPdfWriter(out).use { writer ->
            val info = JpegInfo.read(ByteArrayInputStream(bytes))
            val layout = PdfPageLayout.fitA4(100, 100)
            writer.addPage(ByteArrayInputStream(bytes), bytes.size.toLong(), info, layout, PdfTextLayer.runsFor(ocr, layout))
            writer.finish()
        }
        // The file must still open, and the text must come back unchanged.
        assertTrue(extractText(out.toByteArray()).contains("(x)\\"))
    }
}
