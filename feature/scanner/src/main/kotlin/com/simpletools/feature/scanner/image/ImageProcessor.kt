package com.simpletools.feature.scanner.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.PageTransform
import com.simpletools.core.scanengine.PixelSize
import com.simpletools.core.scanengine.Rotation
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Image work of the scanner, on the device only. Large photos are always decoded with a
 * sample size first, so memory stays bounded whatever the camera resolution.
 * Call from a background thread.
 */
class ImageProcessor(
    /** Longest side kept from the photo: enough for a sharp A4 page, a fraction of a 50 MP photo. */
    private val maxSourceSide: Int = MAX_SOURCE_SIDE,
) {

    /**
     * Turns a photo (camera file or imported image) into an upright JPEG in [target]:
     * the EXIF orientation is applied to the pixels and the size is bounded.
     */
    @Throws(IOException::class)
    fun importPhoto(open: () -> InputStream, target: File): PixelSize {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unreadable image")

        val orientation = runCatching {
            open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSourceSide) sample *= 2
        val decoded = open().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Unreadable image")

        val matrix = orientationMatrix(orientation)
        val scale = min(1f, maxSourceSide.toFloat() / max(decoded.width, decoded.height))
        if (scale < 1f) matrix.postScale(scale, scale)
        val upright = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
        try {
            writeJpeg(upright, target, SOURCE_QUALITY)
            return PixelSize(upright.width, upright.height)
        } finally {
            upright.recycle()
        }
    }

    /** A downsampled bitmap whose longest side is close to (and at least half of) [maxSide]. */
    fun decodeSampled(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /**
     * Draws the page inside [quad] of the photo, straightened (perspective correction) and
     * turned by [rotation], with its longest side at most [maxLongSide].
     */
    @Throws(IOException::class)
    fun renderPage(source: File, sourceSize: PixelSize, quad: CropQuad, rotation: Rotation, maxLongSide: Int): Bitmap {
        // Decode only the resolution the output needs.
        val full = PageTransform.compute(quad, sourceSize.width, sourceSize.height, rotation, maxLongSide)
        val natural = PageTransform.compute(quad, sourceSize.width, sourceSize.height, rotation, Int.MAX_VALUE)
        val reduction = natural.outputSize.longSide.toFloat() / full.outputSize.longSide
        var sample = 1
        while (sample * 2 <= reduction) sample *= 2

        val bitmap = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Unreadable photo")
        try {
            val transform = PageTransform.compute(quad, bitmap.width, bitmap.height, rotation, maxLongSide)
            val size = transform.outputSize
            val output = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)
            val matrix = Matrix().apply { setValues(transform.matrix) }
            canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            return output
        } finally {
            bitmap.recycle()
        }
    }

    /** Renders and saves the final page; returns its size. */
    @Throws(IOException::class)
    fun savePage(source: File, sourceSize: PixelSize, quad: CropQuad, rotation: Rotation, target: File): PixelSize {
        val page = renderPage(source, sourceSize, quad, rotation, PAGE_MAX_SIDE)
        try {
            writeJpeg(page, target, PAGE_QUALITY)
            return PixelSize(page.width, page.height)
        } finally {
            page.recycle()
        }
    }

    private fun writeJpeg(bitmap: Bitmap, target: File, quality: Int) {
        target.parentFile?.mkdirs()
        val tmp = File(target.path + ".tmp")
        try {
            FileOutputStream(tmp).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) throw IOException("Could not encode image")
                out.fd.sync()
            }
            if (!tmp.renameTo(target)) throw IOException("Could not save image")
        } finally {
            tmp.delete()
        }
    }

    private fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }

    companion object {
        const val MAX_SOURCE_SIDE = 3200

        /** ≈ A4 at 300 dpi: sharp text when printed, ~0.5–1 MB per page. */
        const val PAGE_MAX_SIDE = 3508
        const val PREVIEW_MAX_SIDE = 1600
        private const val SOURCE_QUALITY = 92
        private const val PAGE_QUALITY = 85
    }
}
