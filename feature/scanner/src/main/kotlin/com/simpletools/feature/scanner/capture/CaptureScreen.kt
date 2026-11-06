package com.simpletools.feature.scanner.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpletools.core.designsystem.component.SimpleToolsPrimaryButton
import com.simpletools.core.designsystem.component.SimpleToolsSecondaryButton
import com.simpletools.core.designsystem.theme.MaxContentWidth
import com.simpletools.core.designsystem.theme.Spacing
import com.simpletools.core.permissions.AppPermission
import com.simpletools.core.permissions.PermissionStatus
import com.simpletools.core.permissions.rememberPermissionController
import com.simpletools.core.ui.SimpleToolsTopBar
import com.simpletools.feature.scanner.R
import com.simpletools.feature.scanner.ScanIcons

/** Takes the photo of one page. The camera permission is asked here, when the user wants to capture. */
@Composable
internal fun CaptureRoute(
    viewModel: CaptureViewModel,
    isRetake: Boolean,
    onPhotoReady: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberPermissionController(AppPermission.CAMERA)
    var camera by remember { mutableStateOf<CameraHandle?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoImported(uri)
    }
    val importPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(uiState.readyToCrop) {
        if (uiState.readyToCrop) {
            viewModel.navigationDone()
            onPhotoReady()
        }
    }
    val message = uiState.message?.let {
        stringResource(if (it == CaptureMessage.CaptureFailed) R.string.scan_capture_failed else R.string.scan_import_failed)
    }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    CaptureScreen(
        isRetake = isRetake,
        permissionStatus = permission.status,
        uiState = uiState,
        cameraReady = camera?.isReady == true,
        hasFlash = camera?.hasFlash == true,
        snackbarHostState = snackbar,
        onRequestCamera = permission::request,
        onOpenAppSettings = permission::openAppSettings,
        onImportPhoto = importPhoto,
        onToggleTorch = viewModel::toggleTorch,
        onShutter = {
            val handle = camera
            if (handle != null && handle.isReady && !uiState.processing) {
                viewModel.onCaptureStarted()
                handle.takePicture(viewModel.newRawFile(), viewModel::onPhotoTaken, viewModel::onCaptureFailed)
            }
        },
        onNavigateBack = onNavigateBack,
        cameraContent = {
            CameraPreview(
                torchEnabled = uiState.torchEnabled,
                onReady = { camera = it },
                onUnavailable = {
                    camera = null
                    viewModel.onCameraUnavailable()
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
internal fun CaptureScreen(
    isRetake: Boolean,
    permissionStatus: PermissionStatus,
    uiState: CaptureUiState,
    cameraReady: Boolean,
    hasFlash: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestCamera: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onImportPhoto: () -> Unit,
    onToggleTorch: () -> Unit,
    onShutter: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    cameraContent: @Composable () -> Unit,
) {
    val granted = permissionStatus == PermissionStatus.Granted
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleToolsTopBar(
                title = stringResource(if (isRetake) R.string.scan_retake_title else R.string.scan_capture_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    if (granted && hasFlash && !uiState.cameraUnavailable) {
                        IconButton(onClick = onToggleTorch) {
                            Icon(
                                painter = painterResource(if (uiState.torchEnabled) ScanIcons.FlashOff else ScanIcons.FlashOn),
                                contentDescription = stringResource(if (uiState.torchEnabled) R.string.scan_torch_off else R.string.scan_torch_on),
                            )
                        }
                    }
                    if (granted && !uiState.cameraUnavailable) {
                        IconButton(onClick = onImportPhoto, enabled = !uiState.processing) {
                            Icon(painterResource(ScanIcons.Image), contentDescription = stringResource(R.string.scan_import_photo))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !granted -> CameraPermissionContent(permissionStatus, onRequestCamera, onOpenAppSettings, onImportPhoto)
                uiState.cameraUnavailable -> CenteredColumn {
                    Text(
                        stringResource(R.string.scan_camera_unavailable),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    SimpleToolsSecondaryButton(stringResource(R.string.scan_import_photo), onImportPhoto, icon = ScanIcons.Image)
                }
                else -> {
                    Box(Modifier.fillMaxSize().background(Color.Black)) { cameraContent() }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.md),
                    ) {
                        Text(
                            stringResource(R.string.scan_capture_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        )
                    }
                    FilledIconButton(
                        onClick = onShutter,
                        enabled = cameraReady && !uiState.processing,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(Spacing.lg)
                            .size(76.dp)
                            .testTag(SHUTTER_TAG),
                    ) {
                        Icon(
                            painterResource(ScanIcons.Camera),
                            contentDescription = stringResource(R.string.scan_shutter),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            if (uiState.processing) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.scan_processing_photo), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionContent(
    status: PermissionStatus,
    onRequestCamera: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onImportPhoto: () -> Unit,
) {
    CenteredColumn {
        Icon(
            painterResource(ScanIcons.Camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            stringResource(AppPermission.CAMERA.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.scan_camera_rationale), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        when (status) {
            PermissionStatus.PermanentlyDenied -> {
                Text(
                    stringResource(R.string.scan_camera_permanently_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(PERMISSION_MESSAGE_TAG),
                )
                SimpleToolsPrimaryButton(stringResource(R.string.scan_open_settings), onOpenAppSettings)
            }
            PermissionStatus.Denied -> {
                Text(
                    stringResource(R.string.scan_camera_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(PERMISSION_MESSAGE_TAG),
                )
                SimpleToolsPrimaryButton(stringResource(R.string.scan_allow_camera), onRequestCamera)
            }
            else -> SimpleToolsPrimaryButton(stringResource(R.string.scan_allow_camera), onRequestCamera)
        }
        SimpleToolsSecondaryButton(stringResource(R.string.scan_import_photo), onImportPhoto, icon = ScanIcons.Image)
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    // Scrollable so every option stays reachable on small screens, in landscape and with large fonts.
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) { content() }
    }
}

const val SHUTTER_TAG = "scan_shutter"
const val PERMISSION_MESSAGE_TAG = "scan_permission_message"
