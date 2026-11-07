package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextTest {

    private fun word(text: String, l: Int, t: Int, r: Int, b: Int) = OcrWord(text, l, t, r, b)

    private val invoice = OcrPage(
        width = 1000,
        height = 1400,
        lines = listOf(
            OcrLine(listOf(word("Facture", 100, 100, 300, 140), word("n°", 310, 100, 360, 140), word("42", 370, 100, 420, 140))),
            OcrLine(listOf(word("Émission", 100, 200, 320, 240), word("été", 330, 200, 420, 240))),
        ),
    )

    @Test
    fun `search ignores case and accents`() {
        val hits = OcrSearch.search(listOf(invoice), "EMISSION ete")
        assertEquals(listOf(OcrHit(0, "Émission été")), hits)
    }

    @Test
    fun `search finds the page index of every matching line`() {
        val other = OcrPage(1000, 1400, listOf(OcrLine(listOf(word("Facture", 0, 0, 10, 10)))))
        val hits = OcrSearch.search(listOf(invoice, other), "facture")
        assertEquals(listOf(0, 1), hits.map { it.pageIndex })
    }

    @Test
    fun `an empty query finds nothing`() {
        assertTrue(OcrSearch.search(listOf(invoice), "   ").isEmpty())
    }

    @Test
    fun `plain text keeps lines and page order`() {
        val second = OcrPage(1000, 1400, listOf(OcrLine(listOf(word("Page", 0, 0, 1, 1), word("deux", 2, 0, 3, 1)))))
        assertEquals("Facture n° 42\nÉmission été\n\nPage deux", OcrSearch.toPlainText(listOf(invoice, second)))
    }

    @Test
    fun `an unreadable stored page means no text, not a crash`() {
        assertNull(OcrPage.decodeOrNull("{not json"))
        val json = OcrPage.encode(invoice)
        assertEquals(invoice, OcrPage.decodeOrNull(json))
    }

    @Test
    fun `an empty page is recognised as empty`() {
        assertTrue(OcrPage(10, 10, emptyList()).isEmpty)
        assertTrue(!invoice.isEmpty)
    }
}
