package com.simpletools.feature.scanner.data

import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.PageDraft
import com.simpletools.core.scanengine.Rotation
import com.simpletools.core.scanengine.ScanDocument
import com.simpletools.core.scanengine.ScanPage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The single document being scanned. Every change is written to disk before it is published,
 * so closing the app (or Android killing it) at any moment keeps the pages, their order and
 * the page being cropped.
 */
class ScanRepository(
    private val storage: ScanStorage,
    scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val state = MutableStateFlow<ScanDocument?>(null)

    /** null while the saved document is being read (a few milliseconds at launch). */
    val document: StateFlow<ScanDocument?> = state.asStateFlow()

    init {
        scope.launch(io) {
            mutex.withLock {
                val loaded = storage.readDocument()
                storage.deleteUnreferenced(loaded)
                state.value = loaded
            }
        }
    }

    suspend fun current(): ScanDocument = state.filterNotNull().first()

    fun file(name: String) = storage.file(name)

    /** Starts cropping a new photo. An unfinished page is replaced (its new photo deleted). */
    suspend fun startDraft(draft: PageDraft) = update { doc ->
        doc.draft?.takeIf { it.ownsSource && it.sourceFile != draft.sourceFile }?.let { storage.delete(it.sourceFile) }
        doc.copy(draft = draft)
    }

    /** Re-crops an existing page from its original photo. */
    suspend fun startEditing(pageId: String): Boolean {
        var started = false
        update { doc ->
            val page = doc.pages.firstOrNull { it.id == pageId } ?: return@update doc
            doc.draft?.takeIf { it.ownsSource }?.let { storage.delete(it.sourceFile) }
            started = true
            doc.copy(
                draft = PageDraft(
                    id = storage.newId(),
                    sourceFile = page.sourceFile,
                    sourceWidth = page.sourceWidth,
                    sourceHeight = page.sourceHeight,
                    quad = page.quad,
                    rotation = page.rotation,
                    replacesPageId = page.id,
                    ownsSource = false,
                ),
            )
        }
        return started
    }

    suspend fun updateDraft(quad: CropQuad, rotation: Rotation) = update { doc ->
        val draft = doc.draft ?: return@update doc
        doc.copy(draft = draft.copy(quad = quad, rotation = rotation))
    }

    suspend fun discardDraft() = update { doc ->
        val draft = doc.draft ?: return@update doc
        if (draft.ownsSource) storage.delete(draft.sourceFile)
        doc.copy(draft = null)
    }

    /** Saves the corrected page of the current draft, replacing the page it was editing. */
    suspend fun commitDraft(page: ScanPage) = update { doc ->
        val draft = doc.draft ?: return@update doc
        doc.pages.firstOrNull { it.id == draft.replacesPageId }?.let { old ->
            if (old.pageFile != page.pageFile) storage.delete(old.pageFile)
            if (draft.ownsSource && old.sourceFile != page.sourceFile) storage.delete(old.sourceFile)
        }
        doc.commit(page)
    }

    suspend fun movePage(from: Int, to: Int) = update { it.movePage(from, to) }

    suspend fun deletePage(pageId: String) = update { doc ->
        val page = doc.pages.firstOrNull { it.id == pageId } ?: return@update doc
        val draft = doc.draft
        // An unfinished re-crop of this page keeps its photo and becomes a new page.
        val keepSource = draft != null && draft.sourceFile == page.sourceFile
        storage.delete(page.pageFile)
        if (!keepSource) storage.delete(page.sourceFile)
        val newDraft = if (keepSource) draft!!.copy(replacesPageId = null, ownsSource = true) else draft
        doc.removePage(pageId).copy(draft = newDraft)
    }

    /** Starts a new, empty document and deletes every photo of the previous one. */
    suspend fun clear() = update {
        storage.deleteAll()
        ScanDocument()
    }

    private suspend fun update(transform: (ScanDocument) -> ScanDocument): ScanDocument {
        val current = current()
        return mutex.withLock {
            val base = state.value ?: current
            withContext(io) {
                val next = transform(base)
                if (next != base) storage.writeDocument(next)
                next
            }.also { state.value = it }
        }
    }
}
