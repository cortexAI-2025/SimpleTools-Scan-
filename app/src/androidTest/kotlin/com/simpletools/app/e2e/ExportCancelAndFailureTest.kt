package com.simpletools.app.e2e

import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.app.MainActivity
import com.simpletools.app.e2e.E2e.waitForTag
import com.simpletools.app.e2e.E2e.waitForText
import com.simpletools.feature.scanner.document.EXPORT_ERROR_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Cancelling the "save as" screen and a failing destination never lose the pages. */
@RunWith(AndroidJUnit4::class)
class ExportCancelAndFailureTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        E2e.assertAirplaneModeIfRequired()
        E2e.resetDocument()
        Intents.init()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForText("Scan your first page")
        compose.importPages(2)
    }

    @After
    fun tearDown() {
        Intents.release()
        scenario.close()
    }

    @Test
    fun cancellingTheSaveKeepsThePages() {
        E2e.stubSaveAs(null)
        compose.onNodeWithText("Export PDF").performClick()
        compose.onNodeWithText("Save to device").performClick()
        compose.waitForText("PDF not saved. Your pages are still here.")
        E2e.screenshot("export-cancelled")
        compose.onNodeWithText("2 pages").assertExists()
    }

    @Test
    fun aFailedExportCanBeRetriedWithThePagesIntact() {
        // A destination that cannot be written (folder does not exist).
        val broken = File(E2e.context.filesDir, "missing-folder/never/out.pdf")
        E2e.stubSaveAs(Uri.fromFile(broken))
        compose.onNodeWithText("Export PDF").performClick()
        compose.onNodeWithText("Save to device").performClick()
        compose.waitForTag(EXPORT_ERROR_TAG, 30_000)
        compose.onNodeWithText("Export failed").assertExists()
        E2e.screenshot("export-failed")
        assertFalse(broken.exists())

        val good = File(E2e.outputDir, "retry.pdf").apply { delete() }
        E2e.stubSaveAs(Uri.fromFile(good))
        compose.onNodeWithText("Try again").performClick()
        compose.waitForText("PDF saved", 30_000)
        assertEquals(2, E2e.renderPdf(good, "retry").size)
        compose.onNodeWithText("2 pages").assertExists()
    }
}
