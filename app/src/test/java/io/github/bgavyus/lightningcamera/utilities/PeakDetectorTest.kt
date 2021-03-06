package io.github.bgavyus.lightningcamera.utilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeakDetectorTest {
    @Test
    fun getDetectingAndAdd() {
        val detector = PeakDetector(windowSize = 1, deviationThreshold = 0.1, detectionWeight = 1.0)
        assertTrue(detector.getDetectingAndAdd(1.0))
        assertFalse(detector.getDetectingAndAdd(1.0))
    }
}
