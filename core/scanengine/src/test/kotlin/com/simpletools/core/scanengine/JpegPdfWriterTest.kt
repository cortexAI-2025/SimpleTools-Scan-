package com.simpletools.core.scanengine

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import javax.imageio.ImageIO

class JpegPdfWriterTest {

    private fun jpeg(width: Int, height: Int, color: Color, band: Color? = null): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        if (band != null) {
            g.color = band
            g.fillRect(0, 0, width, height / 5)
        }
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
    }

    private fun pdfOf(vararg images: ByteArray, title: String? = null): ByteArray {
        val out = ByteArrayOutputStream()
        JpegPdfWriter(out, title).use { writer ->
            images.forEach { bytes ->
                val info = JpegInfo.read(ByteArrayInputStream(bytes))
                writer.addPage(ByteArrayInputStream(bytes), bytes.size.toLong(), info)
            }
            writer.finish()
        }
        return out.toByteArray()
    }

    private fun Int.rgb() = Color(this)

    @Test
    fun `reads the size of a jpeg without decoding it`() {
        assertEquals(JpegInfo(640, 480, 3), JpegInfo.read(ByteArrayInputStream(jpeg(640, 480, Color.WHITE))))
    }

    @Test
    fun `rejects files that are not jpeg`() {
        try {
            JpegInfo.read(ByteArrayInputStream("%PDF-1.4".toByteArray()))
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }

    @Test
    fun `writes a multi-page pdf that an independent reader opens in the same order`() {
        val red = jpeg(800, 1100, Color.WHITE, band = Color.RED)
        val green = jpeg(1100, 800, Color.WHITE, band = Color.GREEN)
        val blue = jpeg(800, 1100, Color.WHITE, band = Color.BLUE)

        Loader.loadPDF(pdfOf(red, green, blue, title = "Scan été")).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertEquals("Scan été", doc.documentInformation.title)
            // Portrait images on portrait A4, landscape on landscape A4.
            assertEquals(PdfPageLayout.A4_WIDTH, doc.getPage(0).mediaBox.width.toDouble(), 0.01)
            assertEquals(PdfPageLayout.A4_HEIGHT, doc.getPage(1).mediaBox.width.toDouble(), 0.01)

            val renderer = PDFRenderer(doc)
            val expectedBands = listOf(Color.RED, Color.GREEN, Color.BLUE)
            expectedBands.forEachIndexed { index, expected ->
                val page = renderer.renderImage(index, 0.5f)
                val band = page.getRGB(page.width / 2, page.height / 20).rgb()
                val body = page.getRGB(page.width / 2, page.height * 3 / 4).rgb()
                assertTrue("page ${index + 1} band $band", band.isClose(expected))
                assertTrue("page ${index + 1} body $body", body.isClose(Color.WHITE))
            }
        }
    }

    @Test
    fun `file size stays close to the size of the images`() {
        val image = jpeg(2000, 2800, Color.WHITE, band = Color.DARK_GRAY)
        val pdf = pdfOf(image, image, image)
        assertTrue(pdf.size < image.size * 3 + 4096)
    }

    @Test
    fun `numbers are written without locale even on french devices`() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            val pdf = pdfOf(jpeg(100, 100, Color.WHITE))
            val text = String(pdf, Charsets.ISO_8859_1)
            assertTrue(text.contains("/MediaBox [0 0 595.28 841.89]"))
            Loader.loadPDF(pdf).use { assertEquals(1, it.numberOfPages) }
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun `cross reference offsets point at their objects`() {
        val pdf = pdfOf(jpeg(10, 10, Color.WHITE), jpeg(10, 10, Color.BLACK))
        val text = String(pdf, Charsets.ISO_8859_1)
        val xrefAt = text.substringAfterLast("startxref\n").substringBefore("\n").trim().toInt()
        assertTrue(text.startsWith("xref", xrefAt))
        val entries = text.substring(xrefAt).lines().drop(3).takeWhile { it.endsWith(" n ") }
        entries.forEachIndexed { index, line ->
            val offset = line.substring(0, 10).toInt()
            assertTrue("object ${index + 1}", text.startsWith("${index + 1} 0 obj", offset))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `an empty document is refused`() {
        JpegPdfWriter(ByteArrayOutputStream()).finish()
    }

    @Test
    fun `a stream that fails surfaces an IOException`() {
        val failing = object : java.io.OutputStream() {
            var written = 0
            override fun write(b: Int) {
                if (++written > 2000) throw IOException("disk full")
            }
        }
        val image = jpeg(400, 400, Color.WHITE)
        try {
            JpegPdfWriter(failing).use {
                it.addPage(ByteArrayInputStream(image), image.size.toLong(), JpegInfo.read(ByteArrayInputStream(image)))
                it.finish()
            }
            fail("expected IOException")
        } catch (_: IOException) {
        }
    }

    private fun Color.isClose(other: Color, tolerance: Int = 40) =
        kotlin.math.abs(red - other.red) <= tolerance &&
            kotlin.math.abs(green - other.green) <= tolerance &&
            kotlin.math.abs(blue - other.blue) <= tolerance
}
