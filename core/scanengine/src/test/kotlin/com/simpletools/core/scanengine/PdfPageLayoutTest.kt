package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfPageLayoutTest {

    @Test
    fun `portrait image fills the width of a portrait A4 and is centered`() {
        val l = PdfPageLayout.fitA4(1000, 2000)
        assertEquals(PdfPageLayout.A4_WIDTH, l.pageWidth, 0.001)
        assertEquals(PdfPageLayout.A4_HEIGHT, l.imageHeight, 0.001)
        assertEquals((l.pageWidth - l.imageWidth) / 2, l.imageX, 0.001)
        assertEquals(0.0, l.imageY, 0.001)
    }

    @Test
    fun `landscape image goes on a landscape A4`() {
        val l = PdfPageLayout.fitA4(3000, 1000)
        assertEquals(PdfPageLayout.A4_HEIGHT, l.pageWidth, 0.001)
        assertEquals(PdfPageLayout.A4_WIDTH, l.pageHeight, 0.001)
        assertEquals(l.pageWidth, l.imageWidth, 0.001)
    }
}
