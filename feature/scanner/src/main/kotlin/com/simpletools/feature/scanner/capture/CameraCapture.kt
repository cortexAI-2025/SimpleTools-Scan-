package com.simpletools.feature.scanner.capture

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/** Handle on the live camera, to take a photo and drive the flashlight. */
@Stable
class CameraHandle internal constructor(private val controller: LifecycleCameraController, private val context: android.content.Context) {
    var isReady by mutableStateOf(false)
        internal set
    var hasFlash by mutableStateOf(false)
        internal set

    fun setTorch(enabled: Boolean) {
        if (hasFlash) controller.enableTorch(enabled)
    }

    /** Saves a full-resolution photo to [target]; callbacks run on the main thread. */
    fun takePicture(target: File, onSaved: (File) -> Unit, onError: () -> Unit) {
        val options = ImageCapture.OutputFileOptions.Builder(target).build()
        controller.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(target)
                override fun onError(exception: ImageCaptureException) {
                    target.delete()
                    onError()
                }
            },
        )
    }
}

/**
 * Back camera preview, bound to the screen lifecycle: the camera is released as soon as the
 * screen is left or the app goes to the background. Tap to focus and pinch to zoom are built in.
 */
@Composable
internal fun CameraPreview(
    torchEnabled: Boolean,
    onReady: (CameraHandle) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            // 4:3 photos around 12 MP: sharp enough for small print, light enough to process.
            imageCaptureResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(4032, 3024),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    ),
                )
                .build()
        }
    }
    val handle = remember { CameraHandle(controller, context) }
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        var disposed = false
        try {
            controller.bindToLifecycle(lifecycleOwner)
            previewView.controller = controller
            controller.initializationFuture.addListener(
                {
                    if (disposed) return@addListener
                    val ok = runCatching { controller.initializationFuture.get() }.isSuccess &&
                        runCatching { controller.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
                    if (ok) {
                        handle.isReady = true
                        currentOnReady(handle)
                    } else {
                        currentOnUnavailable()
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        } catch (_: Exception) {
            currentOnUnavailable()
        }
        onDispose {
            disposed = true
            handle.isReady = false
            previewView.controller = null
            controller.unbind()
        }
    }

    // The flash unit is known once the camera is open.
    LaunchedEffect(handle.isReady) {
        if (handle.isReady) handle.hasFlash = controller.cameraInfo?.hasFlashUnit() == true
    }
    LaunchedEffect(handle.isReady, torchEnabled) {
        if (handle.isReady) handle.setTorch(torchEnabled)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
