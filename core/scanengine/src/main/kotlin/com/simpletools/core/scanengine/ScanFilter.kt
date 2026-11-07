package com.simpletools.core.scanengine

import kotlinx.serialization.Serializable

/**
 * Colour treatment of a page, applied when the corrected page is rendered.
 * [Color] is the default and the only one stored by documents created before filters existed.
 */
@Serializable
enum class ScanFilter {
    Color,
    Grayscale,
    BlackAndWhite,
}

/**
 * Contrast adjustment of a page, in percent from [MIN] to [MAX]. 0 leaves the page unchanged.
 * It is a SimpleTools Scan PRO setting; free documents keep 0.
 */
object PageContrast {
    const val MIN = -50
    const val MAX = 50

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)

    /** Linear gain applied around mid-grey: +50 % is a gain of 1.5, -50 % a gain of 0.5. */
    fun gain(percent: Int): Float = 1f + clamp(percent) / 100f
}

/**
 * Otsu's method: the grey level that best separates a page's ink from its paper, from the
 * 256-bin histogram of the page. Used by black-and-white rendering and by edge detection.
 */
object OtsuThreshold {

    /**
     * Returns the threshold `t`: pixels with a value ≤ t are one class, the others the other.
     * A histogram with a single occupied level returns that level.
     */
    fun compute(histogram: IntArray): Int {
        require(histogram.size == 256) { "A grey-level histogram has 256 bins" }
        val total = histogram.sum().toDouble()
        if (total == 0.0) return 127
        var sumAll = 0.0
        for (level in 0 until 256) sumAll += level.toDouble() * histogram[level]

        var weightBelow = 0.0
        var sumBelow = 0.0
        var bestLevel = 0
        var bestVariance = -1.0
        for (level in 0 until 256) {
            weightBelow += histogram[level]
            if (weightBelow == 0.0) continue
            val weightAbove = total - weightBelow
            if (weightAbove == 0.0) break
            sumBelow += level.toDouble() * histogram[level]
            val meanBelow = sumBelow / weightBelow
            val meanAbove = (sumAll - sumBelow) / weightAbove
            val variance = weightBelow * weightAbove * (meanBelow - meanAbove) * (meanBelow - meanAbove)
            if (variance > bestVariance) {
                bestVariance = variance
                bestLevel = level
            }
        }
        return bestLevel
    }
}
