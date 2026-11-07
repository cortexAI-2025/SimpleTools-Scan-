package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtsuAndFiltersTest {

    @Test
    fun `otsu separates two well-known grey levels`() {
        val histogram = IntArray(256)
        histogram[20] = 100
        histogram[200] = 100
        val t = OtsuThreshold.compute(histogram)
        assertTrue("threshold $t must lie between the two levels", t in 20 until 200)
    }

    @Test
    fun `otsu on an empty histogram gives the middle grey`() {
        assertEquals(127, OtsuThreshold.compute(IntArray(256)))
    }

    @Test
    fun `otsu needs a 256 bin histogram`() {
        try {
            OtsuThreshold.compute(IntArray(10))
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `contrast is clamped to the PRO range and maps to a gain`() {
        assertEquals(PageContrast.MAX, PageContrast.clamp(999))
        assertEquals(PageContrast.MIN, PageContrast.clamp(-999))
        assertEquals(1.5f, PageContrast.gain(50), 1e-6f)
        assertEquals(1f, PageContrast.gain(0), 1e-6f)
    }

    @Test
    fun `documents saved before filters still load as colour pages`() {
        val legacy = """{"pages":[{"id":"p1","sourceFile":"s.jpg","sourceWidth":10,"sourceHeight":10,
            "pageFile":"p.jpg","pageWidth":10,"pageHeight":10,
            "quad":{"topLeft":{"x":0.1,"y":0.1},"topRight":{"x":0.9,"y":0.1},"bottomRight":{"x":0.9,"y":0.9},"bottomLeft":{"x":0.1,"y":0.9}}}],
            "schemaVersion":1}"""
        val doc = ScanDocument.decodeOrNull(legacy)
        assertEquals(ScanFilter.Color, doc!!.pages.single().filter)
        assertEquals(0, doc.pages.single().contrast)
    }

    @Test
    fun `a filter survives a save and reload`() {
        val page = ScanPage(
            id = "p", sourceFile = "s.jpg", sourceWidth = 10, sourceHeight = 10, pageFile = "p.jpg",
            pageWidth = 10, pageHeight = 10, quad = CropQuad.default(), filter = ScanFilter.BlackAndWhite, contrast = 20,
        )
        val reloaded = ScanDocument.decodeOrNull(ScanDocument(pages = listOf(page)).encode())!!.pages.single()
        assertEquals(ScanFilter.BlackAndWhite, reloaded.filter)
        assertEquals(20, reloaded.contrast)
    }
}
