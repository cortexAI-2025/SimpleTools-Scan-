package com.simpletools.app.e2e

import android.graphics.Color
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.simpletools.app.e2e.E2e.waitFor
import com.simpletools.app.e2e.E2e.waitForTag
import com.simpletools.app.e2e.E2e.waitForText
import com.simpletools.feature.scanner.edit.CORRECTED_PREVIEW_TAG
import com.simpletools.feature.scanner.edit.CROP_IMAGE_TAG

/** On the permission screen the import is a button; with the camera, a top bar icon. */
fun ComposeTestRule.clickImportPhoto() {
    waitFor(hasContentDescription("Import a photo") or hasText("Import a photo"))
    if (onAllNodes(hasContentDescription("Import a photo")).fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithContentDescription("Import a photo").performClick()
    } else {
        onNodeWithText("Import a photo").performClick()
    }
}

/** Adds pages by importing synthetic photos (default corners), from the document screen. */
fun ComposeTestRule.importPages(count: Int, startNumber: Int = 1) {
    val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA)
    for (i in 0 until count) {
        val number = startNumber + i
        E2e.stubPhotoPicker(E2e.syntheticPhoto("IMPORT $number", colors[i % colors.size]))
        val add = if (number == 1) "Scan a page" else "Add a page"
        onNodeWithText(add).performClick()
        clickImportPhoto()
        waitForTag(CROP_IMAGE_TAG, 30_000)
        onNodeWithText("Preview").performClick()
        waitForTag(CORRECTED_PREVIEW_TAG)
        onNodeWithText("Add to document").performClick()
        waitForText(if (number == 1) "1 page" else "$number pages")
    }
}
