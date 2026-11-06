package com.simpletools.feature.scanner

import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.PageDraft
import com.simpletools.core.scanengine.Rotation
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.data.ScanRepository
import com.simpletools.feature.scanner.data.ScanStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ScanRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val storage by lazy { ScanStorage(tmp.root, tmp.newFolder("cache")) }

    /** A new repository on the same files: what the app sees after being closed and reopened. */
    private fun reopen() = ScanRepository(storage, CoroutineScope(dispatcher), dispatcher)

    private fun photo(): String = storage.newImageFile("photo").also { it.writeText("jpeg") }.name

    private fun draft(id: String, replaces: String? = null) = PageDraft(
        id = id, sourceFile = photo(), sourceWidth = 1000, sourceHeight = 1400,
        quad = CropQuad.default(), replacesPageId = replaces,
    )

    private fun pageFrom(d: PageDraft) = ScanPage(
        id = d.replacesPageId ?: d.id, sourceFile = d.sourceFile, sourceWidth = 1000, sourceHeight = 1400,
        pageFile = storage.newImageFile("page").also { it.writeText("jpeg") }.name,
        pageWidth = 900, pageHeight = 1300, quad = d.quad, rotation = d.rotation,
    )

    private suspend fun ScanRepository.addPage(id: String): ScanPage {
        val d = draft(id)
        startDraft(d)
        val page = pageFrom(d)
        commitDraft(page)
        return page
    }

    @Test
    fun `pages, order, rotation and the unfinished crop survive closing the app`() = runTest(dispatcher) {
        val repo = reopen()
        repo.addPage("a")
        repo.addPage("b")
        repo.addPage("c")
        repo.movePage(2, 0)
        repo.startDraft(draft("d"))
        repo.updateDraft(CropQuad.default(0.2f), Rotation(1))

        val reopened = reopen().current()
        assertEquals(listOf("c", "a", "b"), reopened.pages.map { it.id })
        assertEquals(CropQuad.default(0.2f), reopened.draft?.quad)
        assertEquals(Rotation(1), reopened.draft?.rotation)
    }

    @Test
    fun `retaking a page replaces it in place and deletes the old images`() = runTest(dispatcher) {
        val repo = reopen()
        repo.addPage("a")
        val old = repo.addPage("b")
        repo.addPage("c")

        val retake = draft("b2", replaces = "b")
        repo.startDraft(retake)
        repo.commitDraft(pageFrom(retake))

        val doc = repo.current()
        assertEquals(listOf("a", "b", "c"), doc.pages.map { it.id })
        assertEquals(retake.sourceFile, doc.pages[1].sourceFile)
        assertFalse(storage.file(old.pageFile).exists())
        assertFalse(storage.file(old.sourceFile).exists())
    }

    @Test
    fun `adjusting the crop of a page keeps its photo`() = runTest(dispatcher) {
        val repo = reopen()
        val page = repo.addPage("a")
        assertTrue(repo.startEditing("a"))
        val d = repo.current().draft!!
        assertEquals(page.sourceFile, d.sourceFile)
        repo.discardDraft()
        assertTrue(storage.file(page.sourceFile).exists())
        assertNull(repo.current().draft)
    }

    @Test
    fun `discarding a new photo deletes it`() = runTest(dispatcher) {
        val repo = reopen()
        val d = draft("x")
        repo.startDraft(d)
        repo.discardDraft()
        assertFalse(storage.file(d.sourceFile).exists())
    }

    @Test
    fun `deleting a page removes its images, new document removes everything`() = runTest(dispatcher) {
        val repo = reopen()
        val a = repo.addPage("a")
        val b = repo.addPage("b")
        repo.deletePage("a")
        assertFalse(storage.file(a.pageFile).exists())
        assertEquals(listOf("b"), repo.current().pages.map { it.id })

        repo.clear()
        assertTrue(repo.current().isEmpty)
        assertFalse(storage.file(b.pageFile).exists())
        assertTrue(reopen().current().isEmpty)
    }

    @Test
    fun `a page whose image disappeared is dropped instead of breaking the export`() = runTest(dispatcher) {
        val repo = reopen()
        val a = repo.addPage("a")
        repo.addPage("b")
        storage.file(a.pageFile).delete()
        assertEquals(listOf("b"), reopen().current().pages.map { it.id })
    }
}
