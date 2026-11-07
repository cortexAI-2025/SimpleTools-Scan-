package com.simpletools.core.scanengine

import kotlin.math.max

/**
 * One word of the invisible text layer of a searchable PDF, in PDF points (origin bottom-left).
 * The word is drawn with render mode 3 (invisible): it changes what can be selected and searched,
 * never what the page looks like.
 *
 * @param baseline vertical position of the text baseline.
 * @param horizontalScale width of the word as a percentage of Helvetica's natural width, so that the
 *   selection of the word covers the word in the image.
 */
data class PdfTextRun(
    val text: String,
    val x: Double,
    val baseline: Double,
    val fontSize: Double,
    val horizontalScale: Double,
)

/**
 * The text layer uses the standard Helvetica font with WinAnsi encoding, which every PDF reader
 * knows without embedding a font. Characters outside WinAnsi are written as "?".
 */
object PdfTextLayer {

    /** Glyph advance widths of Helvetica (1/1000 em), for codes 0..255 in WinAnsi (0 = not used). */
    private val WIDTHS = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 552,
        400, 549, 333, 333, 333, 576, 537, 333, 333, 333, 365, 556, 834, 834, 834, 611,
        667, 667, 667, 667, 667, 667, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278,
        722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 549, 611, 556, 556, 556, 556, 500, 556, 500,
    )

    /** [text] with every character outside WinAnsi (Latin-1 range) replaced by "?". */
    fun sanitize(text: String): String = buildString(text.length) {
        for (ch in text) {
            val code = ch.code
            append(if ((code in 0x20..0x7E) || (code in 0xA0..0xFF)) ch else '?')
        }
    }

    /** WinAnsi bytes of [text]; one byte per character after [sanitize]. */
    fun encode(text: String): ByteArray = sanitize(text).toByteArray(Charsets.ISO_8859_1)

    /** Helvetica width of [bytes] at font size 1, in 1/1000 em. */
    fun widthOf(bytes: ByteArray): Int = bytes.sumOf { WIDTHS[it.toInt() and 0xFF] }

    /**
     * Maps the words of [page] (boxes in pixels of the page JPEG) onto the A4 [layout] the image is
     * drawn with. Words with no usable size are skipped.
     */
    fun runsFor(page: OcrPage, layout: PdfPageLayout): List<PdfTextRun> {
        if (page.width <= 0 || page.height <= 0) return emptyList()
        val sx = layout.imageWidth / page.width
        val sy = layout.imageHeight / page.height
        val runs = mutableListOf<PdfTextRun>()
        for (line in page.lines) for (word in line.words) {
            val text = sanitize(word.text.trim())
            if (text.isEmpty()) continue
            val boxWidth = (word.right - word.left) * sx
            val boxHeight = (word.bottom - word.top) * sy
            if (boxWidth <= 0 || boxHeight <= 0) continue
            val fontSize = max(1.0, boxHeight)
            val bytes = encode(text)
            val natural = widthOf(bytes) / 1000.0 * fontSize
            if (natural <= 0) continue
            runs += PdfTextRun(
                text = text,
                x = layout.imageX + word.left * sx,
                baseline = layout.imageY + (page.height - word.bottom) * sy,
                fontSize = fontSize,
                horizontalScale = 100.0 * boxWidth / natural,
            )
        }
        return runs
    }
}
