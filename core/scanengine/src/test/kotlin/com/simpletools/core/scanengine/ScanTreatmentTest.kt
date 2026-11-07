package com.simpletools.core.scanengine

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanTreatmentTest {

    @Test
    fun `luminance follows the perceptual weights`() {
        assertEquals(255, ScanTreatment.luminance(255, 255, 255))
        assertEquals(0, ScanTreatment.luminance(0, 0, 0))
        assertEquals(76, ScanTreatment.luminance(255, 0, 0))
        assertEquals(150, ScanTreatment.luminance(0, 255, 0))
        assertEquals(29, ScanTreatment.luminance(0, 0, 255))
    }

    @Test
    fun `contrast leaves levels unchanged at gain one and clamps at the ends`() {
        assertEquals(200, ScanTreatment.adjustLevel(200, 1f))
        assertEquals(255, ScanTreatment.adjustLevel(250, 1.5f))
        assertEquals(0, ScanTreatment.adjustLevel(5, 1.5f))
        assertEquals(128, ScanTreatment.adjustLevel(128, 0.5f))
    }

    @Test
    fun `black and white is pure black or pure white`() {
        assertEquals(255, ScanTreatment.binarize(200, threshold = 130))
        assertEquals(0, ScanTreatment.binarize(100, threshold = 130))
        assertEquals(0xFF808080.toInt(), ScanTreatment.argbGray(128))
    }
}
