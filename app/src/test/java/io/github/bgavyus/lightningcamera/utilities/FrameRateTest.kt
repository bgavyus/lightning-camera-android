package io.github.bgavyus.lightningcamera.utilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameRateTest {
    @Test
    fun testIsHighSpeed() {
        assertFalse(FrameRate(30).isHighSpeed)
        assertFalse(FrameRate(60).isHighSpeed)
        assertTrue(FrameRate(120).isHighSpeed)
        assertTrue(FrameRate(240).isHighSpeed)
    }
}
