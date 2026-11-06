package com.simpletools.feature.scanner

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.permissions.PermissionStatus
import com.simpletools.feature.scanner.capture.CaptureScreen
import com.simpletools.feature.scanner.capture.CaptureUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var requests = 0
    private var settings = 0
    private var imports = 0

    private fun show(status: PermissionStatus) = composeRule.setContent {
        SimpleToolsTheme {
            CaptureScreen(
                isRetake = false,
                permissionStatus = status,
                uiState = CaptureUiState(),
                cameraReady = false,
                hasFlash = false,
                snackbarHostState = SnackbarHostState(),
                onRequestCamera = { requests++ },
                onOpenAppSettings = { settings++ },
                onImportPhoto = { imports++ },
                onToggleTorch = {},
                onShutter = {},
                onNavigateBack = {},
                cameraContent = {},
            )
        }
    }

    @Test
    fun `explains the camera before asking`() {
        show(PermissionStatus.NotRequested)
        composeRule.onNodeWithText("Allow camera").performClick()
        assertEquals(1, requests)
    }

    @Test
    fun `after a refusal the user can ask again or import a photo`() {
        show(PermissionStatus.Denied)
        composeRule.onNodeWithText("Camera access was refused. You can allow it, or import a photo instead.").assertExists()
        composeRule.onNodeWithText("Import a photo").performClick()
        assertEquals(1, imports)
    }

    @Test
    fun `after a permanent refusal the app settings are offered`() {
        show(PermissionStatus.PermanentlyDenied)
        composeRule.onNodeWithText("Open settings").performClick()
        assertEquals(1, settings)
        composeRule.onNodeWithText("Allow camera").assertDoesNotExist()
    }
}
