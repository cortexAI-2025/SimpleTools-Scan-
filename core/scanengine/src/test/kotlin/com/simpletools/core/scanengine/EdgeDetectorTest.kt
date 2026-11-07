package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class EdgeDetectorTest {

    /** A convex polygon (normalized corners, clockwise on screen) filled with [paper], on [background]. */
    private fun scene(
        background: Int,
        paper: Int,
        corners: List<Pair<Double, Double>>,
        w: Int = 320,
        h: Int = 240,
        withText: Boolean = true,
    ): GrayImage {
        val random = Random(42)
        val levels = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val px = (x + 0.5) / w
            val py = (y + 0.5) / h
            val inside = corners.indices.all { i ->
                val (ax, ay) = corners[i]
                val (bx, by) = corners[(i + 1) % corners.size]
                (bx - ax) * (py - ay) - (by - ay) * (px - ax) >= 0
            }
            var value = if (inside) paper else background
            // Printed lines: a dark band every 20 pixels inside the page, like text.
            if (inside && withText && y % 20 in 8..10 && x in 40..(w - 40)) value = background + 20
            levels[y * w + x] = (value + random.nextInt(-6, 7)).coerceIn(0, 255)
        }
        return GrayImage(w, h, levels)
    }

    private val page = listOf(0.20 to 0.15, 0.80 to 0.20, 0.85 to 0.85, 0.15 to 0.80)

    private fun assertCornersNear(expected: List<Pair<Double, Double>>, actual: CropQuad, tolerance: Double = 0.03) {
        val points = actual.corners
        expected.forEachIndexed { i, (ex, ey) ->
            assertEquals("x of corner $i", ex, points[i].x.toDouble(), tolerance)
            assertEquals("y of corner $i", ey, points[i].y.toDouble(), tolerance)
        }
    }

    @Test
    fun `finds a light page on a dark table`() {
        val quad = EdgeDetector.detect(scene(background = 40, paper = 225, corners = page))
        assertNotNull(quad)
        assertCornersNear(page, quad!!)
    }

    @Test
    fun `finds a dark page on a light table`() {
        val quad = EdgeDetector.detect(scene(background = 230, paper = 45, corners = page))
        assertNotNull(quad)
        assertCornersNear(page, quad!!)
    }

    @Test
    fun `finds a rotated and skewed page`() {
        val skewed = listOf(0.30 to 0.10, 0.92 to 0.28, 0.70 to 0.90, 0.08 to 0.66)
        val quad = EdgeDetector.detect(scene(background = 35, paper = 215, corners = skewed))
        assertNotNull(quad)
        assertCornersNear(skewed, quad!!, tolerance = 0.04)
    }

    @Test
    fun `finds the page even with a blank sheet`() {
        val quad = EdgeDetector.detect(scene(background = 40, paper = 225, corners = page, withText = false))
        assertNotNull(quad)
        assertCornersNear(page, quad!!)
    }

    @Test
    fun `a uniform image has no page`() {
        assertNull(EdgeDetector.detect(GrayImage(64, 48, IntArray(64 * 48) { 128 })))
    }

    @Test
    fun `a page filling the whole photo is not detected`() {
        val full = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0)
        assertNull(EdgeDetector.detect(scene(background = 40, paper = 225, corners = full)))
    }

    @Test
    fun `a small object is not a page`() {
        val tiny = listOf(0.45 to 0.45, 0.55 to 0.45, 0.55 to 0.55, 0.45 to 0.55)
        assertNull(EdgeDetector.detect(scene(background = 40, paper = 225, corners = tiny)))
    }

    @Test
    fun `detected corners keep the order that never mirrors the page`() {
        val quad = EdgeDetector.detect(scene(background = 40, paper = 225, corners = page))!!
        assertEquals(true, quad.isValid(320, 240))
        assertEquals(true, abs(quad.topLeft.x - quad.bottomLeft.x) < 0.1)
    }
}
