package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfFileNameTest {

    @Test
    fun `keeps readable names, accents included`() {
        assertEquals("Facture été 2026", PdfFileName.sanitize("  Facture été 2026  "))
        assertEquals("Contrat.pdf", PdfFileName.withExtension("Contrat", "x"))
    }

    @Test
    fun `removes characters forbidden by file systems and a typed extension`() {
        assertEquals("a b c d", PdfFileName.sanitize("a/b:c*d"))
        assertEquals("rapport", PdfFileName.sanitize("rapport.PDF"))
        assertEquals("x", PdfFileName.sanitize("..x.."))
    }

    @Test
    fun `blank names are invalid and fall back`() {
        assertFalse(PdfFileName.isValid("  /// "))
        assertTrue(PdfFileName.isValid("a"))
        assertEquals("Scan 1.pdf", PdfFileName.withExtension("???", "Scan 1"))
    }

    @Test
    fun `long names are shortened`() {
        assertEquals(PdfFileName.MAX_LENGTH, PdfFileName.sanitize("x".repeat(500)).length)
    }
}
