package com.simpletools.feature.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.core.designsystem.theme.SimpleToolsTheme
import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.ScanPage
import com.simpletools.feature.scanner.document.DocumentScreen
import com.simpletools.feature.scanner.document.DocumentUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Tall screen: the lazy list composes every page row.
@Config(qualifiers = "w411dp-h2000dp")
@RunWith(AndroidJUnit4::class)
class DocumentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moves = mutableListOf<Pair<Int, Int>>()
    private val deleted = mutableListOf<String>()
    private val retaken = mutableListOf<String>()
    private var exported = 0

    private fun page(id: String) = ScanPage(id, "$id.jpg", 10, 14, "$id-p.jpg", 9, 13, CropQuad.default())

    private fun show(state: DocumentUiState) = composeRule.setContent {
        SimpleToolsTheme {
            DocumentScreen(
                state = state,
                snackbarHostState = SnackbarHostState(),
                thumbnail = { _, _, modifier -> Box(modifier) },
                onScanPage = {},
                onExport = { exported++ },
                onMove = { from, to -> moves += from to to },
                onEdit = {},
                onRetake = { retaken += it },
                onDelete = { deleted += it },
                onNewDocument = {},
                onContinueDraft = {},
                onDiscardDraft = {},
                onOpenSettings = {},
            )
        }
    }

    @Test
    fun `empty document invites to scan the first page`() {
        show(DocumentUiState(loading = false))
        composeRule.onNodeWithText("Scan your first page").assertExists()
        composeRule.onNodeWithText("Export PDF").assertDoesNotExist()
    }

    @Test
    fun `pages can be reordered, retaken, deleted and exported`() {
        show(DocumentUiState(loading = false, pages = listOf(page("a"), page("b"), page("c"))))

        composeRule.onNodeWithText("3 pages").assertExists()
        composeRule.onNodeWithContentDescription("Move page 1 up").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move page 3 down").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move page 3 up").performClick()
        composeRule.onNodeWithContentDescription("Retake page 2").performClick()
        composeRule.onNodeWithContentDescription("Delete page 1").performClick()
        composeRule.onNodeWithText("Export PDF").performClick()

        assertEquals(listOf(2 to 1), moves)
        assertEquals(listOf("b"), retaken)
        assertEquals(listOf("a"), deleted)
        assertEquals(1, exported)
    }

    @Test
    fun `an unfinished page is offered after reopening the app`() {
        show(DocumentUiState(loading = false, pages = listOf(page("a")), hasDraft = true))
        composeRule.onNodeWithText("Unfinished page").assertExists()
        composeRule.onNodeWithText("Continue").assertExists()
    }
}
