package com.simpletools.app.e2e

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.simpletools.app.MainActivity
import com.simpletools.app.e2e.E2e.waitFor
import com.simpletools.app.e2e.E2e.waitForText
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Runs on a fresh install, before the camera was ever granted. The user refuses the camera
 * twice in the real system dialog: the app explains, then offers the settings, and the
 * document stays usable through photo import.
 */
@RunWith(AndroidJUnit4::class)
class CameraPermissionDeniedTest {

    @get:Rule
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

    private fun refuseInSystemDialog(step: String) {
        val deny = Pattern.compile(".*permission_deny.*button")
        var button: androidx.test.uiautomator.UiObject2? = null
        for (attempt in 0 until 4) {
            // A freshly booted emulator may show "System UI / Launcher isn't responding" over everything.
            E2e.device.findObject(By.res("android:id/aerr_wait"))?.click()
            button = E2e.device.wait(Until.findObject(By.res(deny)), 8_000)
                ?: E2e.device.findObject(By.text(Pattern.compile("(?i)don.t allow|deny|ne pas autoriser|refuser")))
            if (button != null) break
        }
        if (button == null) {
            // Diagnostics in the failure message: CI artifacts may not be reachable.
            val dump = java.io.ByteArrayOutputStream().also { E2e.device.dumpWindowHierarchy(it) }.toString()
            val texts = Regex("text=\"([^\"]+)\"").findAll(dump).map { it.groupValues[1] }.toList()
            val ids = Regex("resource-id=\"([^\"]+)\"").findAll(dump).map { it.groupValues[1] }.filter { it.isNotEmpty() }.toList()
            java.io.File(E2e.outputDir, "perm-$step-hierarchy.xml").writeText(dump)
            assertTrue("system permission dialog not found ($step). package=${E2e.device.currentPackageName} texts=$texts ids=$ids", false)
        }
        E2e.screenshot("perm-$step-dialog")
        button!!.click()
        E2e.device.waitForIdle()
    }

    @Test
    fun refusingTheCameraKeepsTheAppUsable() {
        compose.waitForText("Scan your first page")
        compose.onNodeWithText("Scan a page").performClick()
        compose.waitForText("Allow camera")
        E2e.screenshot("perm-1-rationale")

        compose.onNodeWithText("Allow camera").performClick()
        refuseInSystemDialog("first")
        compose.waitFor(hasText("Camera access was refused", substring = true))
        E2e.screenshot("perm-2-denied")

        compose.onNodeWithText("Allow camera").performClick()
        refuseInSystemDialog("second")
        compose.waitFor(hasText("Camera access is turned off", substring = true))
        compose.onNodeWithText("Open settings").assertExists()
        E2e.screenshot("perm-3-permanently-denied")

        // Without the camera, a page can still be added from a photo.
        compose.onNodeWithContentDescription("Back").performClick()
        compose.importPages(1)
        E2e.screenshot("perm-4-imported")
    }
}
