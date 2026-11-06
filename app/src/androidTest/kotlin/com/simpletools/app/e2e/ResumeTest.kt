package com.simpletools.app.e2e

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpletools.app.MainActivity
import com.simpletools.app.e2e.E2e.cornerPosition
import com.simpletools.app.e2e.E2e.dragCorner
import com.simpletools.app.e2e.E2e.waitFor
import com.simpletools.app.e2e.E2e.waitForTag
import com.simpletools.app.e2e.E2e.waitForText
import com.simpletools.core.scanengine.Corner
import com.simpletools.feature.scanner.document.DRAFT_CARD_TAG
import com.simpletools.feature.scanner.edit.CORRECTED_PREVIEW_TAG
import com.simpletools.feature.scanner.edit.CROP_IMAGE_TAG
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Color

/**
 * Resuming the document. [leaveWorkInProgress] then [resumeAfterForceStop] run as two separate
 * instrumentations with `am force-stop` in between (scripts/run-e2e.sh): the process really dies.
 */
@RunWith(AndroidJUnit4::class)
class ResumeTest {

    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val corner = Offset(300f, 360f)

    @Before
    fun setUp() {
        E2e.assertAirplaneModeIfRequired()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        scenario.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun startCropOfThirdPage() {
        E2e.stubPhotoPicker(E2e.syntheticPhoto("DRAFT", Color.BLUE))
        compose.onNodeWithText("Add a page").performClick()
        compose.clickImportPhoto()
        compose.waitForTag(CROP_IMAGE_TAG, 30_000)
        compose.dragCorner(Corner.TopLeft, corner)
    }

    private fun assertCornerRestored() {
        val p = compose.cornerPosition(Corner.TopLeft)
        assertEquals(corner.x, p.x, 15f)
        assertEquals(corner.y, p.y, 15f)
    }

    /** Android recreates the screen (rotation, low memory): pages, crop and mode are kept. */
    @Test
    fun recreatingTheScreenKeepsTheCrop() {
        E2e.resetDocument()
        launch()
        compose.waitForText("Scan your first page")
        compose.importPages(2)
        startCropOfThirdPage()

        scenario.recreate()
        compose.waitForTag(CROP_IMAGE_TAG)
        assertCornerRestored()

        compose.onNodeWithText("Preview").performClick()
        compose.waitForTag(CORRECTED_PREVIEW_TAG)
        scenario.recreate()
        compose.waitForTag(CORRECTED_PREVIEW_TAG)
        E2e.screenshot("resume-recreated-preview")
        compose.onNodeWithText("Add to document").performClick()
        compose.waitForText("3 pages")
    }

    @Test
    fun leaveWorkInProgress() {
        E2e.resetDocument()
        launch()
        compose.waitForText("Scan your first page")
        compose.importPages(2)
        startCropOfThirdPage()
        E2e.screenshot("resume-1-before-kill")
    }

    @Test
    fun resumeAfterForceStop() {
        launch()
        compose.waitForText("2 pages")
        compose.waitForTag(DRAFT_CARD_TAG)
        E2e.screenshot("resume-2-after-restart")
        compose.onNodeWithText("Continue").performClick()
        compose.waitForTag(CROP_IMAGE_TAG)
        assertCornerRestored()
        compose.onNodeWithText("Preview").performClick()
        compose.waitForTag(CORRECTED_PREVIEW_TAG)
        compose.onNodeWithText("Add to document").performClick()
        compose.waitForText("3 pages")
        E2e.screenshot("resume-3-completed")
    }
}
