package com.simpletools.core.scanengine

import kotlin.math.roundToInt

/**
 * Per-pixel rules of the page filters, free of Android so that they are unit-tested on the JVM.
 * The Android side applies them to every pixel of the corrected page, in the same way for the
 * preview and for the saved page.
 */
object ScanTreatment {

    /** Perceived brightness of an 8-bit RGB pixel (ITU-R BT.601 weights), 0..255. */
    fun luminance(red: Int, green: Int, blue: Int): Int =
        (0.299 * red + 0.587 * green + 0.114 * blue).roundToInt().coerceIn(0, 255)

    /** Contrast around mid-grey: [gain] 1 leaves the level unchanged, above 1 spreads the levels. */
    fun adjustLevel(level: Int, gain: Float): Int =
        ((level - MID) * gain + MID).roundToInt().coerceIn(0, 255)

    /**
     * Black-and-white: a level above [threshold] is paper (white), otherwise ink (black).
     * Returns 255 or 0.
     */
    fun binarize(level: Int, threshold: Int): Int = if (level > threshold) 255 else 0

    /** Packs a grey level into an opaque ARGB pixel. */
    fun argbGray(level: Int): Int = (0xFF shl 24) or (level shl 16) or (level shl 8) or level

    private const val MID = 128
}
