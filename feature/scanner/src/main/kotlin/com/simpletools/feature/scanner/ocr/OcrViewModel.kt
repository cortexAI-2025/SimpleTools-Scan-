package com.simpletools.feature.scanner.ocr

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.simpletools.core.scanengine.OcrHit
import com.simpletools.core.scanengine.OcrPage
import com.simpletools.core.scanengine.OcrSearch
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.ScannerDependencies
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OcrUiState(
    val loading: Boolean = true,
    /** One entry per page of the document, in order; null while the page is not recognised yet. */
    val texts: List<OcrPage?> = emptyList(),
    val running: Boolean = false,
    /** 1-based number of the page being recognised while [running]. */
    val currentPage: Int = 0,
    val failed: Boolean = false,
    val query: String = "",
    val hits: List<OcrHit> = emptyList(),
    val exporting: Boolean = false,
) {
    val pageCount: Int get() = texts.size
    val recognizedCount: Int get() = texts.count { it != null }
    val complete: Boolean get() = pageCount > 0 && recognizedCount == pageCount
}

sealed interface OcrEvent {
    data class Exported(val fileName: String) : OcrEvent
    data object ExportFailed : OcrEvent
}

/**
 * PRO text recognition of the document. Pages are recognised one by one, each result is saved as soon
 * as it is known: stopping, or the app being closed, keeps what was done and the next run continues.
 */
class OcrViewModel(private val deps: ScannerDependencies) : ViewModel() {

    private val state = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = state.asStateFlow()

    private val events = Channel<OcrEvent>(Channel.BUFFERED)
    val eventFlow: Flow<OcrEvent> = events.receiveAsFlow()

    private var pages: List<ScanPage> = emptyList()

    init {
        viewModelScope.launch {
            pages = deps.repository.current().pages
            val texts = withContext(Dispatchers.IO) { pages.map { deps.storage.readOcr(it.pageFile) } }
            state.update { it.copy(loading = false, texts = texts) }
            refreshHits()
        }
    }

    fun recognize() {
        if (state.value.running || pages.isEmpty()) return
        state.update { it.copy(running = true, failed = false) }
        viewModelScope.launch {
            for ((index, page) in pages.withIndex()) {
                if (state.value.texts.getOrNull(index) != null) continue
                state.update { it.copy(currentPage = index + 1) }
                val recognized = runCatching {
                    withContext(Dispatchers.Default) { recognizePage(page) }
                }.getOrNull()
                if (recognized == null) {
                    state.update { it.copy(running = false, currentPage = 0, failed = true) }
                    refreshHits()
                    return@launch
                }
                state.update { s -> s.copy(texts = s.texts.mapIndexed { i, t -> if (i == index) recognized else t }) }
            }
            state.update { it.copy(running = false, currentPage = 0) }
            refreshHits()
        }
    }

    fun setQuery(query: String) {
        state.update { it.copy(query = query) }
        refreshHits()
    }

    /** The recognised text of the whole document, page after page. */
    fun plainText(): String = OcrSearch.toPlainText(state.value.texts.map { it ?: EMPTY_PAGE })

    fun exportText(uri: Uri, fileName: String) = export(fileName) {
        val text = plainText()
        withContext(Dispatchers.IO) {
            val out = deps.appContext.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    /** A PDF of all the pages. Pages without recognised text stay images only. */
    fun exportSearchablePdf(uri: Uri, fileName: String) = export(fileName) {
        withContext(Dispatchers.IO) {
            val resolver: ContentResolver = deps.appContext.contentResolver
            val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write")
            try {
                out.use { deps.exporter.write(pages, fileName.removeSuffix(".pdf"), it, searchable = true) }
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
        }
    }

    private fun export(fileName: String, block: suspend () -> Unit) {
        if (state.value.exporting) return
        state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            val result = runCatching { block() }
            state.update { it.copy(exporting = false) }
            events.send(if (result.isSuccess) OcrEvent.Exported(fileName) else OcrEvent.ExportFailed)
        }
    }

    private suspend fun recognizePage(page: ScanPage): OcrPage {
        val bitmap = deps.imageProcessor.decodeForOcr(deps.storage.file(page.pageFile))
            ?: throw IOException("Unreadable page")
        try {
            val result = deps.ocr.recognize(bitmap)
            deps.storage.writeOcr(page.pageFile, result)
            return result
        } finally {
            bitmap.recycle()
        }
    }

    private fun refreshHits() {
        val s = state.value
        val hits = if (s.query.isBlank()) emptyList() else OcrSearch.search(s.texts.map { it ?: EMPTY_PAGE }, s.query)
        state.update { it.copy(hits = hits) }
    }

    companion object {
        private val EMPTY_PAGE = OcrPage(width = 1, height = 1, lines = emptyList())

        fun factory(deps: ScannerDependencies) = viewModelFactory {
            initializer { OcrViewModel(deps) }
        }
    }
}
