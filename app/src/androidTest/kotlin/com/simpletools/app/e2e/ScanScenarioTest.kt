package com.simpletools.app.e2e

import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.simpletools.app.MainActivity
import com.simpletools.app.e2e.E2e.darkRatio
import com.simpletools.app.e2e.E2e.dragCorner
import com.simpletools.app.e2e.E2e.isBluish
import com.simpletools.app.e2e.E2e.isReddish
import com.simpletools.app.e2e.E2e.meanColor
import com.simpletools.app.e2e.E2e.waitFor
import com.simpletools.app.e2e.E2e.waitForTag
import com.simpletools.app.e2e.E2e.waitForText
import com.simpletools.core.scanengine.Corner
import com.simpletools.feature.scanner.capture.SHUTTER_TAG
import com.simpletools.feature.scanner.document.FILE_NAME_TAG
import com.simpletools.feature.scanner.document.PAGE_LIST_TAG
import com.simpletools.feature.scanner.edit.CORRECTED_PREVIEW_TAG
import com.simpletools.feature.scanner.edit.CROP_IMAGE_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The V1 journey, offline: three pages (one from the camera, two photos of paper lying askew),
 * corners adjusted, one page rotated, order changed, PDF exported, then the PDF is opened
 * with Android's own PDF renderer to check the number, order and content of the pages.
 */
@RunWith(AndroidJUnit4::class)
class ScanScenarioTest {

    @get:Rule(order = 0)
    val camera: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        E2e.assertAirplaneModeIfRequired()
        E2e.resetDocument()
        Intents.init()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        Intents.release()
        scenario.close()
    }

    @Test
    fun threePagesCroppedRotatedReorderedAndExportedOffline() {
        compose.waitForText("Scan your first page")
        E2e.screenshot("01-empty")

        // Page 1: the real camera of the device (emulated camera on CI).
        compose.onNodeWithText("Scan a page").performClick()
        compose.waitFor(hasTestTag(SHUTTER_TAG) and isEnabled(), timeoutMillis = 45_000)
        E2e.screenshot("02-camera")
        compose.onNodeWithTag(SHUTTER_TAG).performClick()
        compose.waitForTag(CROP_IMAGE_TAG, timeoutMillis = 45_000)
        compose.dragCorner(Corner.TopLeft, androidx.compose.ui.geometry.Offset(150f, 150f))
        E2e.screenshot("03-camera-crop")
        compose.onNodeWithText("Preview").performClick()
        compose.waitForTag(CORRECTED_PREVIEW_TAG)
        compose.onNodeWithText("Add to document").performClick()
        compose.waitForText("1 page")

        // Pages 2 and 3: paper photographed at an angle, corrected with the four corners.
        addSkewedPage(label = "PAGE B", band = Color.RED, rotate = false)
        compose.waitForText("2 pages")
        addSkewedPage(label = "PAGE C", band = Color.BLUE, rotate = true)
        compose.waitForText("3 pages")
        E2e.screenshot("06-three-pages")

        // Order A, B, C → C, A, B.
        compose.onNodeWithTag(PAGE_LIST_TAG).performScrollToNode(hasContentDescription("Move page 3 up"))
        compose.onNodeWithContentDescription("Move page 3 up").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PAGE_LIST_TAG).performScrollToNode(hasContentDescription("Move page 2 up"))
        compose.onNodeWithContentDescription("Move page 2 up").performClick()
        compose.waitForIdle()
        E2e.screenshot("07-reordered")

        // Export with a custom name to a file chosen in the (stubbed) system "save as" screen.
        val pdf = File(E2e.outputDir, "scenario.pdf").apply { delete() }
        E2e.stubSaveAs(Uri.fromFile(pdf))
        compose.onNodeWithText("Export PDF").performClick()
        compose.onNodeWithTag(FILE_NAME_TAG).performTextReplacement("Relevé été 2026")
        compose.onNodeWithText("Save to device").performClick()
        compose.waitForText("PDF saved: Relevé été 2026.pdf", timeoutMillis = 30_000)
        E2e.screenshot("08-saved")

        // Check the PDF like a reader would.
        val pages = E2e.renderPdf(pdf, "scenario")
        assertEquals("number of pages", 3, pages.size)
        val (first, second, third) = pages

        // 1st = page C, turned clockwise: landscape, blue band now on the right edge.
        assertTrue("page 1 is landscape", first.width > first.height)
        assertTrue("page 1 blue band on the right", first.meanColor(0.96f, 0.5f).isBluish())
        assertFalse("page 1 no band on top", first.meanColor(0.5f, 0.03f).isBluish())
        // 2nd = the camera page: neither red nor blue band.
        assertFalse(second.meanColor(0.5f, 0.05f).isReddish())
        assertFalse(second.meanColor(0.5f, 0.05f).isBluish())
        // 3rd = page B, upright: red band across the top, perspective corrected (both top corners red).
        assertTrue("page 3 red band", third.meanColor(0.5f, 0.06f).isReddish())
        assertTrue("page 3 top left red", third.meanColor(0.06f, 0.06f).isReddish())
        assertTrue("page 3 top right red", third.meanColor(0.94f, 0.06f).isReddish())
        // Legible: dark text on a white page, nothing washed out or blank.
        val body = third.meanColor(0.9f, 0.95f)
        assertTrue("page 3 white paper", Color.red(body) > 200 && Color.green(body) > 200 && Color.blue(body) > 200)
        assertTrue("page 3 has text", third.darkRatio(0.05f, 0.25f, 0.95f, 0.9f) > 0.02f)
        assertTrue("page 1 has text", first.darkRatio(0.1f, 0.05f, 0.85f, 0.95f) > 0.02f)

        // The document is still there after the export.
        compose.onNodeWithText("3 pages").assertExists()
    }

    private fun addSkewedPage(label: String, band: Int, rotate: Boolean) {
        E2e.stubPhotoPicker(E2e.syntheticPhoto(label, band))
        compose.onNodeWithText("Add a page").performClick()
        compose.clickImportPhoto()
        compose.waitForTag(CROP_IMAGE_TAG, timeoutMillis = 30_000)
        Corner.entries.forEach { compose.dragCorner(it, E2e.PAPER.getValue(it)) }
        E2e.screenshot("04-crop-$label")
        compose.onNodeWithText("Preview").performClick()
        compose.waitForTag(CORRECTED_PREVIEW_TAG)
        if (rotate) {
            compose.onNodeWithText("Rotate").performClick()
            compose.waitForIdle()
            Thread.sleep(500)
            compose.waitForIdle()
        }
        E2e.screenshot("05-preview-$label")
        compose.onNodeWithText("Add to document").performClick()
    }
}
