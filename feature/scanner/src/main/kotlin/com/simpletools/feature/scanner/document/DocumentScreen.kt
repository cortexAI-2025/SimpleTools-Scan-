package com.simpletools.feature.scanner.document

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsSecondaryButton
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.scanengine.PdfFileName
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.core.ui.EmptyState
import com.simpletools.core.ui.LoadingState
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.core.ui.rememberAppLabel
import com.simpletools.core.designsystem.icon.SimpleToolsIcons
import com.simpletools.feature.scanner.R
import com.simpletools.feature.scanner.ScanIcons
import com.simpletools.feature.scanner.image.ImageProcessor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DocumentRoute(
    viewModel: DocumentViewModel,
    imageProcessor: ImageProcessor,
    onScanPage: () -> Unit,
    onRetakePage: (String) -> Unit,
    onOpenDraft: () -> Unit,
    onOpenSettings: () -> Unit,
    isProUnlocked: () -> Boolean = { false },
    onOpenOcr: () -> Unit = {},
    onOpenPro: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var exportDialog by rememberSaveable { mutableStateOf(false) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var pendingSaveName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSearchable by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    val defaultName = stringResource(R.string.scan_default_file_name, SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.ROOT).format(Date()))

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF_MIME)) { uri ->
        val name = pendingSaveName ?: return@rememberLauncherForActivityResult
        pendingSaveName = null
        if (uri == null) viewModel.onSaveCancelled() else viewModel.saveTo(uri, name, searchable = pendingSearchable)
    }
    val startExport: (ExportAction, String) -> Unit = { action, name ->
        if (action == ExportAction.Save || action == ExportAction.SaveSearchable) {
            pendingSaveName = name
            pendingSearchable = action == ExportAction.SaveSearchable
            saveLauncher.launch(name)
        } else {
            viewModel.exportForApps(action, name)
        }
    }

    val savedFormat = stringResource(R.string.scan_export_saved)
    val cancelledMessage = stringResource(R.string.scan_export_cancelled)
    val noAppMessage = stringResource(R.string.scan_no_app)
    val chooserTitle = stringResource(R.string.scan_share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DocumentEvent.Saved -> snackbar.showSnackbar(savedFormat.format(event.fileName))
                DocumentEvent.SaveCancelled -> snackbar.showSnackbar(cancelledMessage)
                is DocumentEvent.Share -> if (!context.sharePdf(event, chooserTitle)) snackbar.showSnackbar(noAppMessage)
                is DocumentEvent.OpenInPdf -> if (!context.openInPdf(event)) snackbar.showSnackbar(noAppMessage)
                DocumentEvent.OpenDraft -> onOpenDraft()
            }
        }
    }

    DocumentScreen(
        state = state,
        snackbarHostState = snackbar,
        thumbnail = { page, description, modifier ->
            PageThumbnail(viewModel.pageFile(page), imageProcessor, description, modifier)
        },
        onScanPage = onScanPage,
        onExport = {
            fileName = defaultName
            exportDialog = true
        },
        onMove = viewModel::movePage,
        onEdit = viewModel::editPage,
        onRetake = onRetakePage,
        onDelete = { confirmDelete = it },
        onNewDocument = { confirmNew = true },
        onContinueDraft = onOpenDraft,
        onDiscardDraft = viewModel::discardDraft,
        onOpenSettings = onOpenSettings,
        onOpenOcr = {
            // PRO: the recognition screen when unlocked, the PRO offer otherwise.
            if (isProUnlocked()) onOpenOcr() else onOpenPro()
        },
    )

    if (exportDialog) {
        ExportDialog(
            fileName = fileName,
            pageCount = state.pages.size,
            canOpenInPdf = remember { viewModel.pdfAppPackage() != null },
            onFileNameChange = { fileName = it },
            onAction = { action ->
                exportDialog = false
                if (action == ExportAction.SaveSearchable && !isProUnlocked()) {
                    onOpenPro()
                } else {
                    startExport(action, PdfFileName.withExtension(fileName, defaultName))
                }
            },
            onDismiss = { exportDialog = false },
        )
    }
    when (val export = state.export) {
        is ExportState.Running -> ExportProgressDialog(export)
        is ExportState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismissExportError,
            title = { Text(stringResource(R.string.scan_export_failed_title)) },
            text = { Text(stringResource(R.string.scan_export_failed_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissExportError()
                    startExport(export.action, export.fileName)
                }) { Text(stringResource(R.string.scan_try_again)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissExportError) { Text(stringResource(R.string.scan_close)) } },
            modifier = Modifier.testTag(EXPORT_ERROR_TAG),
        )
        ExportState.Idle -> Unit
    }
    confirmDelete?.let { pageId ->
        val number = state.pages.indexOfFirst { it.id == pageId } + 1
        if (number == 0) {
            confirmDelete = null
        } else {
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text(stringResource(R.string.scan_delete_title, number)) },
                text = { Text(stringResource(R.string.scan_delete_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = null
                        viewModel.deletePage(pageId)
                    }) { Text(stringResource(R.string.scan_delete_confirm)) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.scan_cancel)) } },
            )
        }
        Unit
    }
    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text(stringResource(R.string.scan_new_document_title)) },
            text = { Text(stringResource(R.string.scan_new_document_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmNew = false
                    viewModel.newDocument()
                }) { Text(stringResource(R.string.scan_new_document_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text(stringResource(R.string.scan_cancel)) } },
        )
    }
}

@Composable
internal fun DocumentScreen(
    state: DocumentUiState,
    snackbarHostState: SnackbarHostState,
    thumbnail: @Composable (ScanPage, String, Modifier) -> Unit,
    onScanPage: () -> Unit,
    onExport: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onEdit: (String) -> Unit,
    onRetake: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNewDocument: () -> Unit,
    onContinueDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOcr: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasPages = state.pages.isNotEmpty()
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleToolsTopBar(
                title = rememberAppLabel(),
                actions = {
                    if (hasPages) {
                        IconButton(onClick = onOpenOcr) {
                            Icon(painterResource(ScanIcons.Document), contentDescription = stringResource(R.string.scan_ocr_action))
                        }
                    }
                    if (hasPages || state.hasDraft) {
                        IconButton(onClick = onNewDocument) {
                            Icon(painterResource(ScanIcons.ClearAll), contentDescription = stringResource(R.string.scan_new_document))
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(SimpleToolsIcons.Settings), contentDescription = stringResource(R.string.scan_settings))
                    }
                },
            )
        },
        bottomBar = {
            if (hasPages) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        SimpleToolsSecondaryButton(
                            text = stringResource(R.string.scan_add_page),
                            onClick = onScanPage,
                            icon = ScanIcons.Add,
                            modifier = Modifier.weight(1f),
                        )
                        SimpleToolsPrimaryButton(
                            text = stringResource(R.string.scan_export_pdf),
                            onClick = onExport,
                            icon = ScanIcons.Pdf,
                            enabled = state.export !is ExportState.Running,
                            modifier = Modifier.weight(1f).testTag(EXPORT_BUTTON_TAG),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            !hasPages && !state.hasDraft -> EmptyState(
                title = stringResource(R.string.scan_empty_title),
                message = stringResource(R.string.scan_empty_message),
                icon = ScanIcons.Document,
                actionLabel = stringResource(R.string.scan_scan_page),
                onAction = onScanPage,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag(PAGE_LIST_TAG),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (state.hasDraft) {
                    item(key = "draft") { DraftCard(onContinueDraft, onDiscardDraft) }
                }
                if (hasPages) {
                    item(key = "count") {
                        Text(
                            pluralStringResource(R.plurals.scan_page_count, state.pages.size, state.pages.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                } else {
                    item(key = "scan") { SimpleToolsPrimaryButton(stringResource(R.string.scan_scan_page), onScanPage, icon = ScanIcons.Camera) }
                }
                itemsIndexed(state.pages, key = { _, page -> page.id }) { index, page ->
                    PageRow(
                        page = page,
                        index = index,
                        count = state.pages.size,
                        thumbnail = thumbnail,
                        onMove = onMove,
                        onEdit = onEdit,
                        onRetake = onRetake,
                        onDelete = onDelete,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftCard(onContinue: () -> Unit, onDiscard: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().testTag(DRAFT_CARD_TAG),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.scan_draft_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.scan_draft_message), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.scan_draft_discard)) }
                TextButton(onClick = onContinue) { Text(stringResource(R.string.scan_draft_continue)) }
            }
        }
    }
}

@Composable
private fun PageRow(
    page: ScanPage,
    index: Int,
    count: Int,
    thumbnail: @Composable (ScanPage, String, Modifier) -> Unit,
    onMove: (Int, Int) -> Unit,
    onEdit: (String) -> Unit,
    onRetake: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val number = index + 1
    Card(modifier.fillMaxWidth().testTag(pageTag(number))) {
        Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            thumbnail(
                page,
                stringResource(R.string.scan_page_image, number),
                Modifier.width(96.dp).aspectRatio(page.pageWidth.toFloat() / page.pageHeight.coerceAtLeast(1)),
            )
            Column(Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(stringResource(R.string.scan_page_label, number), style = MaterialTheme.typography.titleMedium)
                Row {
                    PageAction(ScanIcons.ArrowUp, stringResource(R.string.scan_move_up, number), enabled = index > 0) { onMove(index, index - 1) }
                    PageAction(ScanIcons.ArrowDown, stringResource(R.string.scan_move_down, number), enabled = index < count - 1) { onMove(index, index + 1) }
                }
                Row {
                    PageAction(ScanIcons.Crop, stringResource(R.string.scan_edit_crop, number)) { onEdit(page.id) }
                    PageAction(ScanIcons.Retake, stringResource(R.string.scan_retake, number)) { onRetake(page.id) }
                    PageAction(ScanIcons.Delete, stringResource(R.string.scan_delete, number)) { onDelete(page.id) }
                }
            }
        }
    }
}

@Composable
private fun PageAction(icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(icon), contentDescription = description)
    }
}

@Composable
private fun ExportDialog(
    fileName: String,
    pageCount: Int,
    canOpenInPdf: Boolean,
    onFileNameChange: (String) -> Unit,
    onAction: (ExportAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val valid = PdfFileName.isValid(fileName)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scan_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    label = { Text(stringResource(R.string.scan_file_name)) },
                    suffix = { Text(PdfFileName.EXTENSION) },
                    singleLine = true,
                    isError = !valid,
                    supportingText = {
                        Text(
                            if (valid) pluralStringResource(R.plurals.scan_page_count, pageCount, pageCount)
                            else stringResource(R.string.scan_file_name_invalid),
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag(FILE_NAME_TAG),
                )
                SimpleToolsPrimaryButton(
                    stringResource(R.string.scan_save_to_device),
                    { onAction(ExportAction.Save) },
                    enabled = valid,
                    icon = ScanIcons.Save,
                )
                SimpleToolsSecondaryButton(
                    stringResource(R.string.scan_save_searchable),
                    { onAction(ExportAction.SaveSearchable) },
                    enabled = valid,
                    icon = ScanIcons.Pdf,
                )
                SimpleToolsSecondaryButton(
                    stringResource(R.string.scan_share),
                    { onAction(ExportAction.Share) },
                    enabled = valid,
                    icon = ScanIcons.Share,
                )
                if (canOpenInPdf) {
                    SimpleToolsSecondaryButton(
                        stringResource(R.string.scan_open_in_pdf),
                        { onAction(ExportAction.OpenInPdf) },
                        enabled = valid,
                        icon = ScanIcons.Pdf,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.scan_cancel)) } },
    )
}

@Composable
private fun ExportProgressDialog(state: ExportState.Running) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.scan_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.scan_exporting, (state.done + 1).coerceAtMost(state.total), state.total))
                LinearProgressIndicator(
                    progress = { if (state.total == 0) 0f else state.done.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
    )
}

private fun Context.sharePdf(event: DocumentEvent.Share, chooserTitle: String): Boolean {
    val send = Intent(Intent.ACTION_SEND)
        .setType(PDF_MIME)
        .putExtra(Intent.EXTRA_STREAM, event.uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, event.uri)
    return try {
        startActivity(Intent.createChooser(send, chooserTitle))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun Context.openInPdf(event: DocumentEvent.OpenInPdf): Boolean {
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(event.uri, PDF_MIME)
        .setPackage(event.packageName)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(view)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private const val PDF_MIME = "application/pdf"
const val PAGE_LIST_TAG = "scan_page_list"
const val EXPORT_BUTTON_TAG = "scan_export_button"
const val EXPORT_ERROR_TAG = "scan_export_error"
const val FILE_NAME_TAG = "scan_file_name"
const val DRAFT_CARD_TAG = "scan_draft_card"

fun pageTag(number: Int) = "scan_page_$number"
