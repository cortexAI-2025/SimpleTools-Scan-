package com.simpletools.feature.scanner.ocr

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsSecondaryButton
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.scanengine.OcrPage
import com.simpletools.core.scanengine.OcrSearch
import com.simpletools.core.scanengine.PdfFileName
import com.simpletools.core.ui.LoadingState
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.feature.scanner.R
import com.simpletools.feature.scanner.ScanIcons
import kotlinx.coroutines.launch

@Composable
internal fun OcrRoute(viewModel: OcrViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val exportedFormat = stringResource(R.string.scan_ocr_exported)
    val exportFailed = stringResource(R.string.scan_ocr_export_failed)
    val copiedMessage = stringResource(R.string.scan_ocr_copied)
    val textName = stringResource(R.string.scan_ocr_default_name)
    val pdfName = stringResource(R.string.scan_ocr_default_name)

    val txtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TEXT_MIME)) { uri ->
        if (uri != null) viewModel.exportText(uri, "$textName.txt")
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF_MIME)) { uri ->
        if (uri != null) viewModel.exportSearchablePdf(uri, PdfFileName.withExtension(pdfName, pdfName))
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is OcrEvent.Exported -> snackbar.showSnackbar(exportedFormat.format(event.fileName))
                OcrEvent.ExportFailed -> snackbar.showSnackbar(exportFailed)
            }
        }
    }

    OcrScreen(
        state = state,
        snackbarHostState = snackbar,
        onNavigateBack = onNavigateBack,
        onRecognize = viewModel::recognize,
        onQueryChange = viewModel::setQuery,
        onCopy = {
            context.copyText(viewModel.plainText())
            // A snackbar confirms the copy: the system clipboard notice is not shown on every Android version.
            scope.launch { snackbar.showSnackbar(copiedMessage) }
        },
        onExportText = { txtLauncher.launch("$textName.txt") },
        onExportPdf = { pdfLauncher.launch(PdfFileName.withExtension(pdfName, pdfName)) },
    )
}

@Composable
internal fun OcrScreen(
    state: OcrUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRecognize: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCopy: () -> Unit,
    onExportText: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { SimpleToolsTopBar(title = stringResource(R.string.scan_ocr_title), onNavigateBack = onNavigateBack) },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.scan_ocr_intro), style = MaterialTheme.typography.bodyMedium)

            if (state.running) {
                LinearProgressIndicator(
                    progress = { if (state.pageCount == 0) 0f else (state.currentPage - 1).coerceAtLeast(0).toFloat() / state.pageCount },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.scan_ocr_progress, state.currentPage, state.pageCount), style = MaterialTheme.typography.bodyMedium)
            } else if (!state.complete) {
                SimpleToolsPrimaryButton(
                    text = stringResource(if (state.recognizedCount > 0) R.string.scan_ocr_resume else R.string.scan_ocr_start),
                    onClick = onRecognize,
                    icon = ScanIcons.Document,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.failed) {
                Text(
                    stringResource(R.string.scan_ocr_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.recognizedCount == 0) {
                Text(stringResource(R.string.scan_ocr_empty), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val fullText = remember(state.texts) { OcrSearch.toPlainText(state.texts.map { it ?: EMPTY }) }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.scan_ocr_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.query.isBlank()) {
                SelectionContainer {
                    Text(
                        fullText.ifBlank { stringResource(R.string.scan_ocr_empty) },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (state.hits.isEmpty()) {
                Text(stringResource(R.string.scan_ocr_no_hit), style = MaterialTheme.typography.bodyMedium)
            } else {
                state.hits.forEach { hit ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            stringResource(R.string.scan_ocr_hit_page, hit.pageIndex + 1),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(hit.snippet, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SimpleToolsSecondaryButton(
                    text = stringResource(R.string.scan_ocr_copy),
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SimpleToolsSecondaryButton(
                    text = stringResource(R.string.scan_ocr_export_txt),
                    onClick = onExportText,
                    enabled = !state.exporting,
                    modifier = Modifier.weight(1f),
                )
                SimpleToolsPrimaryButton(
                    text = stringResource(R.string.scan_ocr_export_pdf),
                    onClick = onExportPdf,
                    enabled = !state.exporting,
                    icon = ScanIcons.Pdf,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val EMPTY = OcrPage(width = 1, height = 1, lines = emptyList())

private fun Context.copyText(text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("text", text))
}

private const val TEXT_MIME = "text/plain"
private const val PDF_MIME = "application/pdf"
