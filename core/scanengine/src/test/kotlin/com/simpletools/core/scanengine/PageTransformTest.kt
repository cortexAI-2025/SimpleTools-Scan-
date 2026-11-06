package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTransformTest {

    private val skewed = CropQuad(
        topLeft = NormalizedPoint(0.125f, 0.125f),
        topRight = NormalizedPoint(0.875f, 0.075f),
        bottomRight = NormalizedPoint(0.9f, 0.9f),
        bottomLeft = NormalizedPoint(0.08f, 0.85f),
    )

    private fun assertMaps(t: PageTransform, x: Double, y: Double, ex: Double, ey: Double) {
        val p = t.map(x, y)
        assertEquals("x of ($x, $y)", ex, p.x, 0.05)
        assertEquals("y of ($x, $y)", ey, p.y, 0.05)
    }

    @Test
    fun `corners of the crop land on the corners of the output`() {
        val t = PageTransform.compute(skewed, 1200, 1600, Rotation.None, maxLongSide = 5000)
        val (w, h) = t.outputSize.width.toDouble() to t.outputSize.height.toDouble()
        val px = skewed.toPixels(1200, 1600)
        assertMaps(t, px[0].x, px[0].y, 0.0, 0.0)
        assertMaps(t, px[1].x, px[1].y, w, 0.0)
        assertMaps(t, px[2].x, px[2].y, w, h)
        assertMaps(t, px[3].x, px[3].y, 0.0, h)
    }

    @Test
    fun `output keeps the resolution of the longest edges`() {
        val t = PageTransform.compute(CropQuad.Full, 1000, 1400, Rotation.None, maxLongSide = 5000)
        assertEquals(PixelSize(1000, 1400), t.outputSize)
    }

    @Test
    fun `output is scaled down to the maximum long side, never up`() {
        val big = PageTransform.compute(CropQuad.Full, 3000, 4000, Rotation.None, maxLongSide = 2000)
        assertEquals(PixelSize(1500, 2000), big.outputSize)
        val small = PageTransform.compute(CropQuad.Full, 300, 400, Rotation.None, maxLongSide = 2000)
        assertEquals(PixelSize(300, 400), small.outputSize)
    }

    @Test
    fun `quarter turns rotate clockwise and swap the axes`() {
        val w = 1000.0
        val h = 1400.0
        val r1 = PageTransform.compute(CropQuad.Full, 1000, 1400, Rotation(1), maxLongSide = 5000)
        assertEquals(PixelSize(1400, 1000), r1.outputSize)
        // Top-left of the page goes to the top-right after a clockwise turn.
        assertMaps(r1, 0.0, 0.0, h, 0.0)
        assertMaps(r1, w, h, 0.0, w)

        val r2 = PageTransform.compute(CropQuad.Full, 1000, 1400, Rotation(2), maxLongSide = 5000)
        assertEquals(PixelSize(1000, 1400), r2.outputSize)
        assertMaps(r2, 0.0, 0.0, w, h)

        val r3 = PageTransform.compute(CropQuad.Full, 1000, 1400, Rotation(3), maxLongSide = 5000)
        assertEquals(PixelSize(1400, 1000), r3.outputSize)
        // Top-left goes to the bottom-left after three clockwise turns.
        assertMaps(r3, 0.0, 0.0, 0.0, w)
    }

    @Test
    fun `rotation cycles through four quarter turns`() {
        var r = Rotation.None
        repeat(4) { r = r.rotatedClockwise() }
        assertEquals(Rotation.None, r)
        assertEquals(270, Rotation(3).degrees)
    }

    @Test
    fun `crossed, flat or tiny quads are rejected`() {
        assertTrue(CropQuad.default().isValid(1000, 1000))
        assertTrue(skewed.isValid(1200, 1600))
        val crossed = CropQuad.default().let { it.copy(topRight = it.bottomRight, bottomRight = it.topRight) }
        assertFalse(crossed.isValid(1000, 1000))
        val flat = CropQuad(
            NormalizedPoint(0.1f, 0.5f), NormalizedPoint(0.5f, 0.5f),
            NormalizedPoint(0.9f, 0.5f), NormalizedPoint(0.3f, 0.5f),
        )
        assertFalse(flat.isValid(1000, 1000))
        val tiny = CropQuad(
            NormalizedPoint(0.5f, 0.5f), NormalizedPoint(0.52f, 0.5f),
            NormalizedPoint(0.52f, 0.52f), NormalizedPoint(0.5f, 0.52f),
        )
        assertFalse(tiny.isValid(1000, 1000))
        // Mirrored order (counter-clockwise) would flip the page.
        val mirrored = CropQuad(skewed.topRight, skewed.topLeft, skewed.bottomLeft, skewed.bottomRight)
        assertFalse(mirrored.isValid(1200, 1600))
    }

    @Test
    fun `moving a corner clamps it inside the photo`() {
        val q = CropQuad.default().withCorner(Corner.BottomRight, NormalizedPoint(1.4f, -0.2f))
        assertEquals(NormalizedPoint(1f, 0f), q[Corner.BottomRight])
    }
}
