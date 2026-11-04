package com.simpletools.core.scanengine

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** A point in image coordinates normalized to 0..1 (x to the right, y downwards). */
@Serializable
data class NormalizedPoint(val x: Float, val y: Float) {
    fun clamped(): NormalizedPoint = NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

/** Clockwise quarter turns applied to the corrected page: 0, 1 (90°), 2 (180°) or 3 (270°). */
@Serializable
@JvmInline
value class Rotation(val quarterTurns: Int) {
    init {
        require(quarterTurns in 0..3) { "quarterTurns must be in 0..3, was $quarterTurns" }
    }

    fun rotatedClockwise(): Rotation = Rotation((quarterTurns + 1) % 4)

    val degrees: Int get() = quarterTurns * 90

    /** True when the width and height of the page are swapped. */
    val swapsAxes: Boolean get() = quarterTurns % 2 == 1

    companion object {
        val None = Rotation(0)
    }
}

/**
 * The four corners of the page inside the photo, in normalized image coordinates.
 * The order is fixed (top-left, top-right, bottom-right, bottom-left) so that the page is
 * never mirrored, whatever the user does with the handles.
 */
@Serializable
data class CropQuad(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint,
) {
    val corners: List<NormalizedPoint> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun withCorner(corner: Corner, point: NormalizedPoint): CropQuad {
        val p = point.clamped()
        return when (corner) {
            Corner.TopLeft -> copy(topLeft = p)
            Corner.TopRight -> copy(topRight = p)
            Corner.BottomRight -> copy(bottomRight = p)
            Corner.BottomLeft -> copy(bottomLeft = p)
        }
    }

    operator fun get(corner: Corner): NormalizedPoint = corners[corner.ordinal]

    /**
     * A usable crop is a convex quadrilateral (corners not crossed) with a visible area.
     * A crossed or flat quad would produce a mirrored or degenerate page.
     */
    fun isValid(imageWidth: Int, imageHeight: Int): Boolean {
        val pts = toPixels(imageWidth, imageHeight)
        var sign = 0
        for (i in 0 until 4) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(cross) < 1e-9) return false
            val s = if (cross > 0) 1 else -1
            if (sign == 0) sign = s else if (s != sign) return false
        }
        // Clockwise on screen (y down) gives a positive cross product.
        if (sign < 0) return false
        val area = abs(shoelace(pts))
        return area >= MIN_AREA_FRACTION * imageWidth.toDouble() * imageHeight
    }

    fun toPixels(imageWidth: Int, imageHeight: Int): List<PixelPoint> =
        corners.map { PixelPoint(it.x.toDouble() * imageWidth, it.y.toDouble() * imageHeight) }

    companion object {
        /** Below 1% of the photo, the crop is almost certainly a mistake. */
        const val MIN_AREA_FRACTION = 0.01

        /** Default crop: the whole photo with a small margin so every handle is easy to grab. */
        fun default(inset: Float = 0.06f): CropQuad = CropQuad(
            topLeft = NormalizedPoint(inset, inset),
            topRight = NormalizedPoint(1f - inset, inset),
            bottomRight = NormalizedPoint(1f - inset, 1f - inset),
            bottomLeft = NormalizedPoint(inset, 1f - inset),
        )

        val Full: CropQuad = default(inset = 0f)

        private fun shoelace(p: List<PixelPoint>): Double {
            var sum = 0.0
            for (i in p.indices) {
                val a = p[i]
                val b = p[(i + 1) % p.size]
                sum += a.x * b.y - b.x * a.y
            }
            return sum / 2.0
        }
    }
}

enum class Corner { TopLeft, TopRight, BottomRight, BottomLeft }

data class PixelPoint(val x: Double, val y: Double)

data class PixelSize(val width: Int, val height: Int) {
    val longSide: Int get() = max(width, height)
}

/**
 * Everything needed to draw the corrected page: a 3×3 projective matrix (row-major, the layout
 * of `android.graphics.Matrix.setValues`) that maps source photo pixels to output pixels,
 * already including the rotation, and the output size.
 */
class PageTransform(val matrix: FloatArray, val outputSize: PixelSize) {

    fun map(x: Double, y: Double): PixelPoint {
        val m = matrix
        val w = m[6] * x + m[7] * y + m[8]
        return PixelPoint((m[0] * x + m[1] * y + m[2]) / w, (m[3] * x + m[4] * y + m[5]) / w)
    }

    companion object {
        /**
         * @param imageWidth size in pixels of the photo the quad refers to.
         * @param maxLongSide the output is scaled down so that its longest side fits; it is never
         *   enlarged beyond the photo's own resolution, which would only blur and weigh more.
         */
        fun compute(
            quad: CropQuad,
            imageWidth: Int,
            imageHeight: Int,
            rotation: Rotation,
            maxLongSide: Int,
        ): PageTransform {
            require(imageWidth > 0 && imageHeight > 0) { "Empty image" }
            require(maxLongSide > 0)
            val src = quad.toPixels(imageWidth, imageHeight)
            val (tl, tr, br, bl) = src
            // Natural size of the page = longest opposite edges, so no side loses resolution.
            val naturalWidth = max(distance(tl, tr), distance(bl, br))
            val naturalHeight = max(distance(tl, bl), distance(tr, br))
            val scale = minOf(1.0, maxLongSide / max(naturalWidth, naturalHeight))
            val w = max(1, (naturalWidth * scale).roundToInt())
            val h = max(1, (naturalHeight * scale).roundToInt())

            val dst = listOf(
                PixelPoint(0.0, 0.0),
                PixelPoint(w.toDouble(), 0.0),
                PixelPoint(w.toDouble(), h.toDouble()),
                PixelPoint(0.0, h.toDouble()),
            )
            val homography = Homography.fromQuads(src, dst)
            val rotated = multiply(rotationMatrix(rotation, w, h), homography)
            val size = if (rotation.swapsAxes) PixelSize(h, w) else PixelSize(w, h)
            return PageTransform(normalize(rotated).map { it.toFloat() }.toFloatArray(), size)
        }

        /** Maps the unrotated page (w × h) onto the rotated output, clockwise. */
        private fun rotationMatrix(rotation: Rotation, w: Int, h: Int): DoubleArray = when (rotation.quarterTurns) {
            0 -> doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
            // (x, y) → (h − y, x)
            1 -> doubleArrayOf(0.0, -1.0, h.toDouble(), 1.0, 0.0, 0.0, 0.0, 0.0, 1.0)
            // (x, y) → (w − x, h − y)
            2 -> doubleArrayOf(-1.0, 0.0, w.toDouble(), 0.0, -1.0, h.toDouble(), 0.0, 0.0, 1.0)
            // (x, y) → (y, w − x)
            else -> doubleArrayOf(0.0, 1.0, 0.0, -1.0, 0.0, w.toDouble(), 0.0, 0.0, 1.0)
        }

        private fun distance(a: PixelPoint, b: PixelPoint) = hypot(a.x - b.x, a.y - b.y)

        private fun multiply(a: DoubleArray, b: DoubleArray): DoubleArray {
            val r = DoubleArray(9)
            for (row in 0 until 3) for (col in 0 until 3) {
                var sum = 0.0
                for (k in 0 until 3) sum += a[row * 3 + k] * b[k * 3 + col]
                r[row * 3 + col] = sum
            }
            return r
        }

        private fun normalize(m: DoubleArray): DoubleArray {
            val d = m[8]
            return if (abs(d) > 1e-12) DoubleArray(9) { m[it] / d } else m
        }
    }
}

/** Projective transform between two quadrilaterals (direct linear transform, 8 unknowns). */
internal object Homography {

    fun fromQuads(src: List<PixelPoint>, dst: List<PixelPoint>): DoubleArray {
        require(src.size == 4 && dst.size == 4)
        val a = Array(8) { DoubleArray(9) }
        for (i in 0 until 4) {
            val (x, y) = src[i]
            val (u, v) = dst[i]
            a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y, u)
            a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y, v)
        }
        val h = solve(a)
        return doubleArrayOf(h[0], h[1], h[2], h[3], h[4], h[5], h[6], h[7], 1.0)
    }

    /** Gaussian elimination with partial pivoting on an 8×9 augmented matrix. */
    private fun solve(m: Array<DoubleArray>): DoubleArray {
        val n = 8
        for (col in 0 until n) {
            var pivot = col
            for (row in col + 1 until n) if (abs(m[row][col]) > abs(m[pivot][col])) pivot = row
            require(abs(m[pivot][col]) > 1e-12) { "Degenerate quadrilateral" }
            val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
            for (row in 0 until n) {
                if (row == col) continue
                val factor = m[row][col] / m[col][col]
                if (factor == 0.0) continue
                for (k in col..n) m[row][k] -= factor * m[col][k]
            }
        }
        return DoubleArray(n) { m[it][n] / m[it][it] }
    }
}
