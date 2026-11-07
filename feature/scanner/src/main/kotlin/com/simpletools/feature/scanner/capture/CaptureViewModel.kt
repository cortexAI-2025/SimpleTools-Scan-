package com.simpletools.feature.scanner.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.PageDraft
import com.simpletools.feature.scanner.ScannerDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

data class CaptureUiState(
    val processing: Boolean = false,
    val torchEnabled: Boolean = false,
    val cameraUnavailable: Boolean = false,
    val message: CaptureMessage? = null,
    /** Set once the photo is ready to crop: the screen navigates to the crop editor. */
    val readyToCrop: Boolean = false,
)

enum class CaptureMessage { CaptureFailed, ImportFailed }

class CaptureViewModel(
    private val deps: ScannerDependencies,
    private val replacePageId: String?,
) : ViewModel() {

    private val state = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = state.asStateFlow()

    fun newRawFile(): File = deps.storage.newImageFile("raw")

    fun onCaptureStarted() = state.update { it.copy(processing = true, message = null) }

    fun onCaptureFailed() = state.update { it.copy(processing = false, message = CaptureMessage.CaptureFailed) }

    /** Photo taken by the camera: made upright, then opened in the crop editor. */
    fun onPhotoTaken(raw: File) = process(CaptureMessage.CaptureFailed, cleanup = raw) { raw.inputStream() }

    /** Photo picked with the system photo picker (no permission needed). */
    fun onPhotoImported(uri: Uri) {
        state.update { it.copy(processing = true, message = null) }
        process(CaptureMessage.ImportFailed, cleanup = null) {
            deps.appContext.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        }
    }

    fun onCameraUnavailable() = state.update { it.copy(cameraUnavailable = true) }

    fun toggleTorch() = state.update { it.copy(torchEnabled = !it.torchEnabled) }

    fun messageShown() = state.update { it.copy(message = null) }

    fun navigationDone() = state.update { it.copy(readyToCrop = false) }

    private fun process(failure: CaptureMessage, cleanup: File?, open: () -> InputStream) {
        state.update { it.copy(processing = true) }
        viewModelScope.launch {
            val target = deps.storage.newImageFile("photo")
            val result = withContext(Dispatchers.IO) {
                try {
                    runCatching { deps.imageProcessor.importPhoto(open, target) }
                } finally {
                    cleanup?.delete()
                }
            }
            result.onSuccess { size ->
                // Free: the page is located in the photo; the default crop is kept when nothing is found.
                val detected = withContext(Dispatchers.Default) {
                    runCatching { deps.imageProcessor.detectPage(target) }.getOrNull()
                }
                deps.repository.startDraft(
                    PageDraft(
                        id = deps.storage.newId(),
                        sourceFile = target.name,
                        sourceWidth = size.width,
                        sourceHeight = size.height,
                        quad = detected ?: CropQuad.default(),
                        replacesPageId = replacePageId,
                        ownsSource = true,
                    ),
                )
                state.update { it.copy(processing = false, readyToCrop = true, torchEnabled = false) }
            }.onFailure {
                target.delete()
                state.update { it.copy(processing = false, message = failure) }
            }
        }
    }

    companion object {
        fun factory(deps: ScannerDependencies, replacePageId: String?) = viewModelFactory {
            initializer { CaptureViewModel(deps, replacePageId) }
        }
    }
}
