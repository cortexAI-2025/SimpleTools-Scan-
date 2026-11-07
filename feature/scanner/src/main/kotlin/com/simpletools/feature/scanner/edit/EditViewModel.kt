package com.simpletools.feature.scanner.edit

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpletools.core.scanengine.Corner
import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.NormalizedPoint
import com.simpletools.core.scanengine.PageContrast
import com.simpletools.core.scanengine.PageDraft
import com.simpletools.core.scanengine.PixelSize
import com.simpletools.core.scanengine.Rotation
import com.simpletools.core.scanengine.ScanFilter
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.ScannerDependencies
import com.simpletools.feature.scanner.image.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditMode { Adjust, Preview }

/** Outcome of the last "detect the page" request. */
enum class DetectionResult { None, Found, NotFound }

data class EditUiState(
    val loading: Boolean = true,
    val draft: PageDraft? = null,
    /** Downsampled photo shown under the corner handles. */
    val photo: Bitmap? = null,
    val quad: CropQuad = CropQuad.default(),
    val rotation: Rotation = Rotation.None,
    val filter: ScanFilter = ScanFilter.Color,
    /** Contrast in percent; only changed by PRO users (see [proUnlocked]). */
    val contrast: Int = 0,
    val proUnlocked: Boolean = false,
    val mode: EditMode = EditMode.Adjust,
    /** The corrected page, rendered from the same code as the saved page. */
    val preview: Bitmap? = null,
    val renderingPreview: Boolean = false,
    val detecting: Boolean = false,
    val detection: DetectionResult = DetectionResult.None,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    /** True once the page is saved or discarded, or when there is nothing to edit: leave the screen. */
    val finished: Boolean = false,
) {
    val quadValid: Boolean get() = draft != null && quad.isValid(draft.sourceWidth, draft.sourceHeight)
    val isReplacing: Boolean get() = draft?.replacesPageId != null
}

/**
 * Edits the unfinished page of the document. The corners, rotation, filter and contrast are saved
 * with the document after each change, and the mode in the saved state, so the work survives the app
 * being closed or recreated.
 */
class EditViewModel(
    private val deps: ScannerDependencies,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val state = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = state.asStateFlow()
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val draft = deps.repository.current().draft
            if (draft == null) {
                state.update { it.copy(loading = false, finished = true) }
                return@launch
            }
            val photo = withContext(Dispatchers.IO) {
                deps.imageProcessor.decodeSampled(deps.repository.file(draft.sourceFile), ImageProcessor.PREVIEW_MAX_SIDE)
            }
            if (photo == null) {
                deps.repository.discardDraft()
                state.update { it.copy(loading = false, finished = true) }
                return@launch
            }
            val mode = savedState.get<String>(KEY_MODE)?.let(EditMode::valueOf) ?: EditMode.Adjust
            state.update {
                it.copy(
                    loading = false,
                    draft = draft,
                    photo = photo,
                    quad = draft.quad,
                    rotation = draft.rotation,
                    filter = draft.filter,
                    contrast = draft.contrast,
                    proUnlocked = deps.isProUnlocked(),
                    mode = mode,
                )
            }
            if (mode == EditMode.Preview && state.value.quadValid) renderPreview() else setMode(EditMode.Adjust)
        }
    }

    /** Live update while dragging; saved with [onCornerReleased]. */
    fun moveCorner(corner: Corner, point: NormalizedPoint) = state.update { it.copy(quad = it.quad.withCorner(corner, point)) }

    fun onCornerReleased() = persist()

    fun resetCorners() {
        state.update { it.copy(quad = CropQuad.default(), detection = DetectionResult.None) }
        persist()
    }

    /** Looks for the sheet of paper in the photo and places the corners on it. */
    fun detectEdges() {
        val draft = state.value.draft ?: return
        if (state.value.detecting) return
        state.update { it.copy(detecting = true, detection = DetectionResult.None) }
        viewModelScope.launch {
            val found = withContext(Dispatchers.Default) {
                runCatching { deps.imageProcessor.detectPage(deps.repository.file(draft.sourceFile)) }.getOrNull()
            }
            if (found != null) {
                state.update { it.copy(quad = found, detecting = false, detection = DetectionResult.Found) }
                persist()
                if (state.value.mode == EditMode.Preview) renderPreview()
            } else {
                state.update { it.copy(detecting = false, detection = DetectionResult.NotFound) }
            }
        }
    }

    fun showPreview() {
        if (!state.value.quadValid) return
        setMode(EditMode.Preview)
        renderPreview()
    }

    fun backToAdjust() = setMode(EditMode.Adjust)

    fun rotate() {
        state.update { it.copy(rotation = it.rotation.rotatedClockwise()) }
        persist()
        if (state.value.mode == EditMode.Preview) renderPreview()
    }

    /** Free: colour, grey or black and white. */
    fun setFilter(filter: ScanFilter) {
        if (state.value.filter == filter) return
        state.update { it.copy(filter = filter) }
        persist()
        if (state.value.mode == EditMode.Preview) renderPreview()
    }

    /** PRO only: the slider updates the value live; [onContrastReleased] saves it and refreshes the preview. */
    fun setContrast(percent: Int) {
        if (!state.value.proUnlocked) return
        state.update { it.copy(contrast = PageContrast.clamp(percent)) }
    }

    fun onContrastReleased() {
        persist()
        if (state.value.mode == EditMode.Preview) renderPreview()
    }

    fun detectionShown() = state.update { it.copy(detection = DetectionResult.None) }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving || !current.quadValid) return
        state.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            val target = deps.storage.newImageFile("page")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    deps.imageProcessor.savePage(
                        deps.repository.file(draft.sourceFile),
                        PixelSize(draft.sourceWidth, draft.sourceHeight),
                        current.quad,
                        current.rotation,
                        target,
                        current.filter,
                        current.contrast,
                    )
                }
            }
            result.onSuccess { size ->
                deps.repository.commitDraft(
                    ScanPage(
                        id = draft.replacesPageId ?: draft.id,
                        sourceFile = draft.sourceFile,
                        sourceWidth = draft.sourceWidth,
                        sourceHeight = draft.sourceHeight,
                        pageFile = target.name,
                        pageWidth = size.width,
                        pageHeight = size.height,
                        quad = current.quad,
                        rotation = current.rotation,
                        filter = current.filter,
                        contrast = current.contrast,
                    ),
                )
                state.update { it.copy(saving = false, finished = true) }
            }.onFailure {
                target.delete()
                state.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    fun saveErrorShown() = state.update { it.copy(saveFailed = false) }

    fun discard() {
        viewModelScope.launch {
            deps.repository.discardDraft()
            state.update { it.copy(finished = true) }
        }
    }

    private fun setMode(mode: EditMode) {
        savedState[KEY_MODE] = mode.name
        state.update { it.copy(mode = mode) }
    }

    private fun persist() {
        val s = state.value
        viewModelScope.launch { deps.repository.updateDraft(s.quad, s.rotation, s.filter, s.contrast) }
    }

    private fun renderPreview() {
        val s = state.value
        val draft = s.draft ?: return
        previewJob?.cancel()
        state.update { it.copy(renderingPreview = true) }
        previewJob = viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                runCatching {
                    deps.imageProcessor.renderPage(
                        deps.repository.file(draft.sourceFile),
                        PixelSize(draft.sourceWidth, draft.sourceHeight),
                        s.quad,
                        s.rotation,
                        ImageProcessor.PREVIEW_MAX_SIDE,
                        s.filter,
                        s.contrast,
                    )
                }.getOrNull()
            }
            state.update { it.copy(preview = bitmap ?: it.preview, renderingPreview = false) }
        }
    }

    companion object {
        private const val KEY_MODE = "edit_mode"

        fun factory(deps: ScannerDependencies) = viewModelFactory {
            initializer { EditViewModel(deps, createSavedStateHandle()) }
        }
    }
}
