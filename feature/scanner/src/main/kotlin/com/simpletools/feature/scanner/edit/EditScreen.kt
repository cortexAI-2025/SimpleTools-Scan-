package com.simpletools.feature.scanner.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsSecondaryButton
import com.simpletools.core.designsystem.component.SimpleToolsTextButton
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.scanengine.PageContrast
import com.simpletools.core.scanengine.ScanFilter
import com.simpletools.core.ui.LoadingState
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.feature.scanner.R
import com.simpletools.feature.scanner.ScanIcons
import kotlin.math.roundToInt

private const val CONTRAST_SLIDER_TAG = "scan_contrast_slider"

@Composable
internal fun EditRoute(viewModel: EditViewModel, onFinished: () -> Unit, onOpenPro: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    val saveError = stringResource(R.string.scan_save_page_failed)
    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbar.showSnackbar(saveError)
            viewModel.saveErrorShown()
        }
    }
    val notFound = stringResource(R.string.scan_detect_not_found)
    LaunchedEffect(state.detection) {
        if (state.detection == DetectionResult.NotFound) {
            snackbar.showSnackbar(notFound)
            viewModel.detectionShown()
        }
    }

    val back: () -> Unit = {
        if (!state.saving) {
            if (state.mode == EditMode.Preview) {
                viewModel.backToAdjust()
            } else {
                confirmLeave = true
            }
        }
    }
    BackHandler(enabled = !state.finished, onBack = back)

    EditScreen(
        state = state,
        snackbarHostState = snackbar,
        onNavigateBack = back,
        onCornerMoved = viewModel::moveCorner,
        onCornerReleased = viewModel::onCornerReleased,
        onReset = viewModel::resetCorners,
        onDetect = viewModel::detectEdges,
        onShowPreview = viewModel::showPreview,
        onAdjust = viewModel::backToAdjust,
        onRotate = viewModel::rotate,
        onSave = viewModel::save,
        onFilterSelected = viewModel::setFilter,
        onContrastChanged = viewModel::setContrast,
        onContrastReleased = viewModel::onContrastReleased,
        onOpenPro = onOpenPro,
    )

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.scan_leave_title)) },
            text = { Text(stringResource(R.string.scan_leave_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.discard()
                }) { Text(stringResource(R.string.scan_leave_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.scan_keep_editing)) }
            },
        )
    }
}

@Composable
internal fun EditScreen(
    state: EditUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onCornerMoved: (com.simpletools.core.scanengine.Corner, com.simpletools.core.scanengine.NormalizedPoint) -> Unit,
    onCornerReleased: () -> Unit,
    onReset: () -> Unit,
    onDetect: () -> Unit,
    onShowPreview: () -> Unit,
    onAdjust: () -> Unit,
    onRotate: () -> Unit,
    onSave: () -> Unit,
    onFilterSelected: (ScanFilter) -> Unit,
    onContrastChanged: (Int) -> Unit,
    onContrastReleased: () -> Unit,
    onOpenPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = state.mode == EditMode.Preview
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleToolsTopBar(
                title = stringResource(if (preview) R.string.scan_preview_title else R.string.scan_crop_title),
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            if (!state.loading && state.draft != null) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (preview) {
                            PreviewActions(
                                state = state,
                                onRotate = onRotate,
                                onAdjust = onAdjust,
                                onSave = onSave,
                                onFilterSelected = onFilterSelected,
                                onContrastChanged = onContrastChanged,
                                onContrastReleased = onContrastReleased,
                                onOpenPro = onOpenPro,
                            )
                        } else {
                            AdjustActions(state, onReset, onDetect, onShowPreview)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            val photo = state.photo
            when {
                state.loading || photo == null -> LoadingState()
                preview -> {
                    val bitmap = state.preview
                    if (bitmap != null) {
                        Image(
                            bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                            contentDescription = stringResource(R.string.scan_corrected_preview),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(Spacing.md).testTag(CORRECTED_PREVIEW_TAG),
                        )
                    }
                    if (state.renderingPreview || bitmap == null) CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                else -> CropEditor(
                    photo = remember(photo) { photo.asImageBitmap() },
                    quad = state.quad,
                    onCornerMoved = onCornerMoved,
                    onCornerReleased = onCornerReleased,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
            if (state.saving) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.scan_saving_page), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustActions(state: EditUiState, onReset: () -> Unit, onDetect: () -> Unit, onShowPreview: () -> Unit) {
    Text(
        stringResource(if (state.quadValid) R.string.scan_crop_hint else R.string.scan_crop_invalid),
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.quadValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        SimpleToolsTextButton(stringResource(R.string.scan_reset_corners), onReset)
        SimpleToolsSecondaryButton(
            text = stringResource(R.string.scan_detect_edges),
            onClick = onDetect,
            enabled = !state.detecting && !state.saving,
            icon = ScanIcons.Crop,
            modifier = Modifier.weight(1f),
        )
    }
    SimpleToolsPrimaryButton(
        text = stringResource(R.string.scan_show_preview),
        onClick = onShowPreview,
        enabled = state.quadValid,
        icon = ScanIcons.Crop,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PreviewActions(
    state: EditUiState,
    onRotate: () -> Unit,
    onAdjust: () -> Unit,
    onSave: () -> Unit,
    onFilterSelected: (ScanFilter) -> Unit,
    onContrastChanged: (Int) -> Unit,
    onContrastReleased: () -> Unit,
    onOpenPro: () -> Unit,
) {
    // Free: the page filter. The same treatment is saved, so the preview is exactly the result.
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            ScanFilter.Color to R.string.scan_filter_color,
            ScanFilter.Grayscale to R.string.scan_filter_grayscale,
            ScanFilter.BlackAndWhite to R.string.scan_filter_black_white,
        ).forEach { (filter, label) ->
            FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterSelected(filter) },
                enabled = !state.saving,
                label = { Text(stringResource(label)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    // PRO: contrast. Without PRO the control is shown and leads to the offer, never silently disabled.
    if (state.proUnlocked) {
        Column {
            Text(
                stringResource(R.string.scan_contrast_value, state.contrast),
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = state.contrast.toFloat(),
                onValueChange = { onContrastChanged(it.roundToInt()) },
                onValueChangeFinished = onContrastReleased,
                valueRange = PageContrast.MIN.toFloat()..PageContrast.MAX.toFloat(),
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().testTag(CONTRAST_SLIDER_TAG),
            )
        }
    } else {
        SimpleToolsTextButton(stringResource(R.string.scan_contrast_pro), onOpenPro)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SimpleToolsSecondaryButton(
            text = stringResource(R.string.scan_rotate),
            onClick = onRotate,
            enabled = !state.saving,
            icon = ScanIcons.RotateRight,
            modifier = Modifier.weight(1f),
        )
        SimpleToolsSecondaryButton(
            text = stringResource(R.string.scan_adjust_corners),
            onClick = onAdjust,
            enabled = !state.saving,
            icon = ScanIcons.Crop,
            modifier = Modifier.weight(1f),
        )
    }
    SimpleToolsPrimaryButton(
        text = stringResource(if (state.isReplacing) R.string.scan_replace_page else R.string.scan_save_page),
        onClick = onSave,
        enabled = !state.saving && state.preview != null,
        icon = ScanIcons.Save,
    )
}
