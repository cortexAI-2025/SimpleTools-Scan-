package com.simpletools.core.scanengine

/** An 8-bit grayscale image, row-major, one level (0..255) per pixel. */
class GrayImage(val width: Int, val height: Int, val levels: IntArray) {
    init {
        require(width > 0 && height > 0) { "Empty image" }
        require(levels.size == width * height) { "Expected ${width * height} levels, got ${levels.size}" }
    }
}

/**
 * Finds the four corners of a sheet of paper in a photo, on the device, without any library.
 *
 * Method (deliberately simple, so it is predictable and easy to test):
 * 1. a 3×3 blur removes the sensor noise;
 * 2. Otsu's threshold splits the photo into a bright and a dark class; each class is tried as "the
 *    page" (a dark page on a light table is as valid as a light page on a dark table);
 * 3. a closing (radius 3) bridges the dark gaps made by printed lines, then a light opening removes
 *    specks; the largest connected region that does not touch three or four borders of the photo is
 *    kept (that excludes the table itself);
 * 4. the corners are the extreme points of the region: top-left has the smallest x+y, top-right the
 *    largest x−y, bottom-right the largest x+y, bottom-left the smallest x−y.
 *
 * It returns null when there is no convincing page (uniform background, page filling the photo,
 * page too small); the crop then stays at its default position and the user adjusts it by hand.
 * Call on a small image (about 320 px on the longest side): cost grows with the pixel count.
 */
object EdgeDetector {

    /** A region smaller than this share of the photo is not a page. */
    const val MIN_PAGE_FRACTION = 0.10

    /** Half-width (in pixels of the working image) of the closing that bridges printed lines. */
    private const val CLOSING_RADIUS = 3

    /** A region covering almost the whole photo is the background or the photo itself, not a page. */
    const val MAX_PAGE_FRACTION = 0.97

    fun detect(image: GrayImage): CropQuad? {
        val w = image.width
        val h = image.height
        val smooth = boxBlur(image.levels, w, h)
        val histogram = IntArray(256)
        for (level in smooth) histogram[level]++
        val threshold = OtsuThreshold.compute(histogram)

        var best: Candidate? = null
        for (bright in booleanArrayOf(true, false)) {
            val mask = BooleanArray(w * h) { i -> (smooth[i] > threshold) == bright }
            val candidate = largestPageRegion(cleanMask(mask, w, h), w, h) ?: continue
            if (best == null || candidate.area > best.area) best = candidate
        }
        val found = best ?: return null
        val quad = CropQuad(
            topLeft = normalized(found.topLeft, w, h),
            topRight = normalized(found.topRight, w, h),
            bottomRight = normalized(found.bottomRight, w, h),
            bottomLeft = normalized(found.bottomLeft, w, h),
        )
        return quad.takeIf { it.isValid(w, h) }
    }

    private class Candidate(
        val area: Int,
        val topLeft: IntPoint,
        val topRight: IntPoint,
        val bottomRight: IntPoint,
        val bottomLeft: IntPoint,
    )

    private data class IntPoint(val x: Int, val y: Int)

    private fun normalized(p: IntPoint, w: Int, h: Int) = NormalizedPoint(p.x.toFloat() / w, p.y.toFloat() / h).clamped()

    /** The biggest 4-connected region of [mask] that is page-sized and touches at most two borders. */
    private fun largestPageRegion(mask: BooleanArray, w: Int, h: Int): Candidate? {
        val total = w * h
        val visited = BooleanArray(total)
        val queue = IntArray(total)
        val minArea = MIN_PAGE_FRACTION * total
        val maxArea = MAX_PAGE_FRACTION * total
        var best: Candidate? = null

        for (start in 0 until total) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true

            var minSum = Int.MAX_VALUE
            var maxSum = Int.MIN_VALUE
            var minDiff = Int.MAX_VALUE
            var maxDiff = Int.MIN_VALUE
            var tl = IntPoint(0, 0)
            var br = IntPoint(0, 0)
            var tr = IntPoint(0, 0)
            var bl = IntPoint(0, 0)
            var borders = 0

            while (head < tail) {
                val p = queue[head++]
                val x = p % w
                val y = p / w
                val sum = x + y
                val diff = x - y
                if (sum < minSum) { minSum = sum; tl = IntPoint(x, y) }
                if (sum > maxSum) { maxSum = sum; br = IntPoint(x, y) }
                if (diff > maxDiff) { maxDiff = diff; tr = IntPoint(x, y) }
                if (diff < minDiff) { minDiff = diff; bl = IntPoint(x, y) }
                if (x == 0) borders = borders or LEFT
                if (x == w - 1) borders = borders or RIGHT
                if (y == 0) borders = borders or TOP
                if (y == h - 1) borders = borders or BOTTOM

                if (x > 0) enqueue(p - 1, mask, visited, queue, tail).also { tail = it }
                if (x < w - 1) enqueue(p + 1, mask, visited, queue, tail).also { tail = it }
                if (y > 0) enqueue(p - w, mask, visited, queue, tail).also { tail = it }
                if (y < h - 1) enqueue(p + w, mask, visited, queue, tail).also { tail = it }
            }

            val area = tail.toDouble()
            if (area < minArea || area > maxArea) continue
            if (Integer.bitCount(borders) > 2) continue
            if (best == null || tail > best.area) {
                best = Candidate(tail, tl, tr, br, bl)
            }
        }
        return best
    }

    /** Marks [q] for the flood fill; returns the new queue tail. */
    private fun enqueue(q: Int, mask: BooleanArray, visited: BooleanArray, queue: IntArray, tail: Int): Int {
        if (!mask[q] || visited[q]) return tail
        visited[q] = true
        queue[tail] = q
        return tail + 1
    }

    private const val LEFT = 1
    private const val RIGHT = 2
    private const val TOP = 4
    private const val BOTTOM = 8

    /** 3×3 box blur with the edges replicated. */
    private fun boxBlur(levels: IntArray, w: Int, h: Int): IntArray {
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                for (dy in -1..1) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    for (dx in -1..1) {
                        val xx = (x + dx).coerceIn(0, w - 1)
                        sum += levels[yy * w + xx]
                        count++
                    }
                }
                out[y * w + x] = sum / count
            }
        }
        return out
    }

    /**
     * Cleans the page mask: a closing (dilate then erode, radius [CLOSING_RADIUS]) bridges the dark
     * gaps made by printed lines, then an opening (erode then dilate, radius 1) removes specks.
     */
    private fun cleanMask(mask: BooleanArray, w: Int, h: Int): BooleanArray {
        val closed = morph(morph(mask, w, h, CLOSING_RADIUS, dilate = true), w, h, CLOSING_RADIUS, dilate = false)
        return morph(morph(closed, w, h, 1, dilate = false), w, h, 1, dilate = true)
    }

    /** Square structuring element of [radius]: dilation (any neighbour set) or erosion (all neighbours set). */
    private fun morph(mask: BooleanArray, w: Int, h: Int, radius: Int, dilate: Boolean): BooleanArray {
        val out = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var result = !dilate
            loop@ for (dy in -radius..radius) {
                val yy = y + dy
                for (dx in -radius..radius) {
                    val xx = x + dx
                    val inBounds = xx in 0 until w && yy in 0 until h
                    // Outside the photo: nothing to dilate with, and erosion ignores the border
                    // so that a region touching the photo edge is not shrunk by the cleanup.
                    if (dilate && inBounds && mask[yy * w + xx]) { result = true; break@loop }
                    if (!dilate && inBounds && !mask[yy * w + xx]) { result = false; break@loop }
                }
            }
            out[y * w + x] = result
        }
        return out
    }
}
