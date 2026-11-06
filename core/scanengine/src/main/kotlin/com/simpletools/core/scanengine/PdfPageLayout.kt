package com.simpletools.core.scanengine

import kotlin.math.min

/** Where an image sits on a PDF page, in PDF points (1/72 inch, origin bottom-left). */
data class PdfPageLayout(
    val pageWidth: Double,
    val pageHeight: Double,
    val imageX: Double,
    val imageY: Double,
    val imageWidth: Double,
    val imageHeight: Double,
) {
    companion object {
        const val A4_WIDTH = 595.28
        const val A4_HEIGHT = 841.89

        /**
         * A4, portrait or landscape like the image, with the image scaled to fit and centered:
         * pages print correctly and open at a consistent size in every reader.
         */
        fun fitA4(imageWidthPx: Int, imageHeightPx: Int): PdfPageLayout {
            require(imageWidthPx > 0 && imageHeightPx > 0)
            val landscape = imageWidthPx > imageHeightPx
            val pw = if (landscape) A4_HEIGHT else A4_WIDTH
            val ph = if (landscape) A4_WIDTH else A4_HEIGHT
            val scale = min(pw / imageWidthPx, ph / imageHeightPx)
            val w = imageWidthPx * scale
            val h = imageHeightPx * scale
            return PdfPageLayout(pw, ph, (pw - w) / 2, (ph - h) / 2, w, h)
        }
    }
}
