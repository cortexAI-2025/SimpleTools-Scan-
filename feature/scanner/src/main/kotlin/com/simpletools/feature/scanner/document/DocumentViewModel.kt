package com.simpletools.feature.scanner.document

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpletools.core.scanengine.PdfFileName
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.ScannerDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class ExportAction { Save, Share, OpenInPdf }

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val done: Int, val total: Int) : ExportState

    /** The pages are untouched; [action] can be tried again with the same [fileName]. */
    data class Failed(val action: ExportAction, val fileName: String) : ExportState
}

sealed interface DocumentEvent {
    data class Saved(val fileName: String) : DocumentEvent
    data object SaveCancelled : DocumentEvent
    data class Share(val uri: Uri) : DocumentEvent
    data class OpenInPdf(val uri: Uri, val packageName: String) : DocumentEvent
    data object OpenDraft : DocumentEvent
}

data class DocumentUiState(
    val loading: Boolean = true,
    val pages: List<ScanPage> = emptyList(),
    val hasDraft: Boolean = false,
    val export: ExportState = ExportState.Idle,
)

class DocumentViewModel(private val deps: ScannerDependencies) : ViewModel() {

    private val export = MutableStateFlow<ExportState>(ExportState.Idle)
    private val events = Channel<DocumentEvent>(Channel.BUFFERED)
    val eventFlow: Flow<DocumentEvent> = events.receiveAsFlow()

    val uiState: StateFlow<DocumentUiState> = combine(deps.repository.document, export) { doc, exportState ->
        if (doc == null) {
            DocumentUiState(loading = true, export = exportState)
        } else {
            DocumentUiState(loading = false, pages = doc.pages, hasDraft = doc.draft != null, export = exportState)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentUiState())

    fun pageFile(page: ScanPage): File = deps.repository.file(page.pageFile)

    fun movePage(from: Int, to: Int) {
        viewModelScope.launch { deps.repository.movePage(from, to) }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch { deps.repository.deletePage(pageId) }
    }

    fun newDocument() {
        viewModelScope.launch { deps.repository.clear() }
    }

    fun discardDraft() {
        viewModelScope.launch { deps.repository.discardDraft() }
    }

    fun editPage(pageId: String) {
        viewModelScope.launch {
            if (deps.repository.startEditing(pageId)) events.send(DocumentEvent.OpenDraft)
        }
    }

    /** The installed SimpleTools PDF, if any (release first, then a debug build). */
    fun pdfAppPackage(): String? = PDF_PACKAGES.firstOrNull { name ->
        runCatching { deps.appContext.packageManager.getPackageInfo(name, 0) }.isSuccess
    }

    /** The user closed the system "save as" screen without choosing a place. */
    fun onSaveCancelled() {
        export.value = ExportState.Idle
        viewModelScope.launch { events.send(DocumentEvent.SaveCancelled) }
    }

    /** Writes the PDF where the user chose in the system file picker. */
    fun saveTo(uri: Uri, fileName: String) = launchExport(ExportAction.Save, fileName) { pages, title, onPage ->
        val resolver = deps.appContext.contentResolver
        try {
            val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write to $uri")
            out.use { deps.exporter.write(pages, title, it, onPage) }
        } catch (e: Exception) {
            deletePartial(resolver, uri)
            throw e
        }
        DocumentEvent.Saved(fileName)
    }

    /** Writes the PDF in the private cache, then hands it to the share sheet or SimpleTools PDF. */
    fun exportForApps(action: ExportAction, fileName: String) = launchExport(action, fileName) { pages, title, onPage ->
        val dir = deps.storage.exportsDir
        dir.deleteRecursively()
        if (!dir.mkdirs() && !dir.isDirectory) throw IOException("Cannot create $dir")
        val file = File(dir, fileName)
        try {
            file.outputStream().use { deps.exporter.write(pages, title, it, onPage) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        val context = deps.appContext
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val pdfApp = pdfAppPackage()
        if (action == ExportAction.OpenInPdf && pdfApp != null) DocumentEvent.OpenInPdf(uri, pdfApp) else DocumentEvent.Share(uri)
    }

    fun dismissExportError() {
        export.value = ExportState.Idle
    }

    private fun launchExport(
        action: ExportAction,
        fileName: String,
        block: suspend (List<ScanPage>, String, (Int) -> Unit) -> DocumentEvent,
    ) {
        if (export.value is ExportState.Running) return
        viewModelScope.launch {
            val pages = deps.repository.current().pages
            if (pages.isEmpty()) return@launch
            export.value = ExportState.Running(0, pages.size)
            val title = fileName.removeSuffix(PdfFileName.EXTENSION)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    block(pages, title) { done -> export.value = ExportState.Running(done, pages.size) }
                }
            }
            result.onSuccess {
                export.value = ExportState.Idle
                events.send(it)
            }.onFailure {
                export.value = ExportState.Failed(action, fileName)
            }
        }
    }

    private fun deletePartial(resolver: ContentResolver, uri: Uri) {
        runCatching {
            when {
                uri.scheme == ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).delete() }
                DocumentsContract.isDocumentUri(deps.appContext, uri) -> DocumentsContract.deleteDocument(resolver, uri)
                else -> Unit
            }
        }
    }

    companion object {
        val PDF_PACKAGES = listOf("com.simpletools.pdf", "com.simpletools.pdf.debug")

        fun authority(context: Context) = "${context.packageName}.scan.files"

        fun factory(deps: ScannerDependencies) = viewModelFactory {
            initializer { DocumentViewModel(deps) }
        }
    }
}
