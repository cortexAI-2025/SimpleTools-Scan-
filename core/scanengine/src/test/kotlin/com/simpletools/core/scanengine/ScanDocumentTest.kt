package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDocumentTest {

    private fun page(id: String) = ScanPage(
        id = id, sourceFile = "$id-src.jpg", sourceWidth = 100, sourceHeight = 140,
        pageFile = "$id-page.jpg", pageWidth = 90, pageHeight = 130, quad = CropQuad.default(),
    )

    private fun draft(id: String, replaces: String? = null) = PageDraft(
        id = id, sourceFile = "$id-src.jpg", sourceWidth = 100, sourceHeight = 140,
        quad = CropQuad.default(), replacesPageId = replaces,
    )

    private val abc = ScanDocument(pages = listOf(page("a"), page("b"), page("c")))

    private fun ScanDocument.ids() = pages.map { it.id }

    @Test
    fun `pages can be moved to any position`() {
        assertEquals(listOf("c", "a", "b"), abc.movePage(2, 0).ids())
        assertEquals(listOf("b", "c", "a"), abc.movePage(0, 2).ids())
        assertEquals(listOf("a", "c", "b"), abc.movePage(2, 1).ids())
        assertEquals(abc, abc.movePage(0, 3))
        assertEquals(abc, abc.movePage(-1, 0))
    }

    @Test
    fun `removing a page keeps the order of the others`() {
        assertEquals(listOf("a", "c"), abc.removePage("b").ids())
    }

    @Test
    fun `committing a new draft appends the page`() {
        val doc = abc.copy(draft = draft("d")).commit(page("d"))
        assertEquals(listOf("a", "b", "c", "d"), doc.ids())
        assertNull(doc.draft)
    }

    @Test
    fun `committing a retake replaces the page at the same position`() {
        val doc = abc.copy(draft = draft("b2", replaces = "b")).commit(page("b2"))
        assertEquals(listOf("a", "b2", "c"), doc.ids())
    }

    @Test
    fun `a retake of a page deleted meanwhile is appended`() {
        val doc = abc.removePage("b").copy(draft = draft("b2", replaces = "b")).commit(page("b2"))
        assertEquals(listOf("a", "c", "b2"), doc.ids())
    }

    @Test
    fun `document survives encoding, including the unfinished crop and rotations`() {
        val doc = ScanDocument(
            pages = listOf(page("a").copy(rotation = Rotation(3))),
            draft = draft("d").copy(quad = CropQuad.default().withCorner(Corner.TopLeft, NormalizedPoint(0.2f, 0.3f)), rotation = Rotation(1)),
        )
        assertEquals(doc, ScanDocument.decodeOrNull(doc.encode()))
    }

    @Test
    fun `unreadable or newer documents are ignored instead of crashing`() {
        assertNull(ScanDocument.decodeOrNull("{not json"))
        assertNull(ScanDocument.decodeOrNull("""{"pages":[],"schemaVersion":99}"""))
        assertEquals(ScanDocument(), ScanDocument.decodeOrNull("""{"pages":[],"futureField":1}"""))
    }

    @Test
    fun `referenced files cover pages and draft`() {
        val files = abc.copy(draft = draft("d")).referencedFiles()
        assertTrue(files.containsAll(listOf("a-src.jpg", "a-page.jpg", "c-page.jpg", "d-src.jpg")))
        assertEquals(7, files.size)
    }
}
