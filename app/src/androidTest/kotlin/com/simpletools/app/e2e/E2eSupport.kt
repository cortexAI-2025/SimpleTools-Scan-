package com.simpletools.app.e2e

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.simpletools.app.SimpleToolsApplication
import com.simpletools.core.scanengine.Corner
import com.simpletools.feature.scanner.edit.CROP_IMAGE_TAG
import com.simpletools.feature.scanner.edit.cornerTag
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anyOf
import org.junit.Assert.assertEquals
import java.io.File
import java.io.FileOutputStream

/** Helpers shared by the end-to-end tests. Outputs (PDF, screenshots) go to files/e2e-output. */
object E2e {

    val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val context: Context get() = instrumentation.targetContext
    val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    /** Size of the synthetic photos and where the paper sheet sits in them (a skewed quad). */
    const val PHOTO_W = 1200
    const val PHOTO_H = 1600
    val PAPER = mapOf(
        Corner.TopLeft to Offset(170f, 210f),
        Corner.TopRight to Offset(1040f, 130f),
        Corner.BottomRight to Offset(1100f, 1490f),
        Corner.BottomLeft to Offset(110f, 1400f),
    )

    val outputDir: File get() = File(context.filesDir, "e2e-output").apply { mkdirs() }

    fun resetDocument() = runBlocking {
        (context.applicationContext as SimpleToolsApplication).container.scanner.repository.clear()
    }

    /** The CI turns airplane mode on and passes `-e requireAirplane true`. */
    fun assertAirplaneModeIfRequired() {
        if (InstrumentationRegistry.getArguments().getString("requireAirplane") == "true") {
            assertEquals("airplane mode", 1, Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))
        }
    }

    fun screenshot(name: String) {
        runCatching { device.takeScreenshot(File(outputDir, "$name.png")) }
    }

    /**
     * A photo of a sheet of paper lying askew on a dark table: the sheet has a colored band at
     * its top, a big [label] and lines of text, so that order, rotation, perspective correction
     * and legibility can be checked in the PDF.
     */
    fun syntheticPhoto(label: String, band: Int): Uri {
        val pageW = 850
        val pageH = 1100
        val page = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        Canvas(page).apply {
            drawColor(Color.WHITE)
            drawRect(0f, 0f, pageW.toFloat(), pageH * 0.15f, Paint().apply { color = band })
            val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 120f; isFakeBoldText = true }
            drawText(label, 70f, 380f, big)
            val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 38f }
            for (i in 0 until 12) drawText("SimpleTools Scan - line ${i + 1} of $label", 70f, 480f + i * 50f, small)
        }
        val photo = Bitmap.createBitmap(PHOTO_W, PHOTO_H, Bitmap.Config.ARGB_8888)
        Canvas(photo).apply {
            drawColor(Color.rgb(48, 44, 40))
            val src = floatArrayOf(0f, 0f, pageW.toFloat(), 0f, pageW.toFloat(), pageH.toFloat(), 0f, pageH.toFloat())
            val dst = Corner.entries.flatMap { listOf(PAPER.getValue(it).x, PAPER.getValue(it).y) }.toFloatArray()
            val m = Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
            drawBitmap(page, m, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val file = File(context.cacheDir, "e2e-$label.jpg")
        FileOutputStream(file).use { photo.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return Uri.fromFile(file)
    }

    /** The next photo picker request returns [uri] (no permission involved, like the real picker). */
    fun stubPhotoPicker(uri: Uri) {
        intending(anyOf(hasAction(MediaStore.ACTION_PICK_IMAGES), hasAction(Intent.ACTION_OPEN_DOCUMENT), hasAction(Intent.ACTION_GET_CONTENT)))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))
    }

    /** The next "save as" request returns [uri], or is cancelled when null. */
    fun stubSaveAs(uri: Uri?) {
        val result = if (uri == null) {
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        } else {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri))
        }
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(result)
    }

    fun ComposeTestRule.waitFor(matcher: SemanticsMatcher, timeoutMillis: Long = 20_000) {
        waitUntil(timeoutMillis) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    fun ComposeTestRule.waitForText(text: String, timeoutMillis: Long = 20_000) = waitFor(hasText(text, substring = true), timeoutMillis)

    fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = 20_000) = waitFor(hasTestTag(tag), timeoutMillis)

    /** Drags a crop handle so that it lands on [target], given in pixels of the synthetic photo. */
    fun ComposeTestRule.dragCorner(corner: Corner, target: Offset) {
        waitForIdle()
        val image = onNodeWithTag(CROP_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val handle = onNodeWithTag(cornerTag(corner)).fetchSemanticsNode().boundsInRoot
        val targetInRoot = Offset(
            image.left + target.x / PHOTO_W * image.width,
            image.top + target.y / PHOTO_H * image.height,
        )
        val delta = targetInRoot - handle.center
        onNodeWithTag(cornerTag(corner)).performTouchInput {
            swipe(start = center, end = center + delta, durationMillis = 600)
        }
        waitForIdle()
    }

    /** Position of a handle center, in pixels of the synthetic photo. */
    fun ComposeTestRule.cornerPosition(corner: Corner): Offset {
        val image = onNodeWithTag(CROP_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val handle = onNodeWithTag(cornerTag(corner)).fetchSemanticsNode().boundsInRoot
        return Offset(
            (handle.center.x - image.left) / image.width * PHOTO_W,
            (handle.center.y - image.top) / image.height * PHOTO_H,
        )
    }

    /** Renders every page of [pdf] like a reader would, saves them as PNG and returns them. */
    fun renderPdf(pdf: File, prefix: String): List<Bitmap> {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                return (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(File(outputDir, "$prefix-page${index + 1}.png")).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap
                    }
                }
            }
        }
    }

    fun decode(file: File): Bitmap = BitmapFactory.decodeFile(file.path)

    /** Mean color of a small area, coordinates as fractions of the bitmap. */
    fun Bitmap.meanColor(fx: Float, fy: Float, radius: Int = 6): Int {
        val cx = (fx * width).toInt().coerceIn(radius, width - radius - 1)
        val cy = (fy * height).toInt().coerceIn(radius, height - radius - 1)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (x in cx - radius..cx + radius) for (y in cy - radius..cy + radius) {
            val c = getPixel(x, y)
            r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
        }
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    fun Int.isReddish() = Color.red(this) > 150 && Color.green(this) < 110 && Color.blue(this) < 110
    fun Int.isBluish() = Color.blue(this) > 150 && Color.red(this) < 110 && Color.green(this) < 140

    /** Share of dark pixels in a region: text is there and has not been washed out. */
    fun Bitmap.darkRatio(left: Float, top: Float, right: Float, bottom: Float): Float {
        var dark = 0
        var total = 0
        for (x in (left * width).toInt() until (right * width).toInt() step 2) {
            for (y in (top * height).toInt() until (bottom * height).toInt() step 2) {
                val c = getPixel(x, y)
                if (Color.red(c) + Color.green(c) + Color.blue(c) < 3 * 90) dark++
                total++
            }
        }
        return dark.toFloat() / total
    }
}
