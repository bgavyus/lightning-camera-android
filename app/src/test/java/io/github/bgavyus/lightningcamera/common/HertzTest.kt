package io.github.bgavyus.lightningcamera.common

import io.github.bgavyus.lightningcamera.utilities.Hertz
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HertzTest {
    @Test
    fun testIsHighSpeed() {
        assertFalse(Hertz(30).isHighSpeed)
        assertFalse(Hertz(60).isHighSpeed)
        assertTrue(Hertz(120).isHighSpeed)
        assertTrue(Hertz(240).isHighSpeed)
    }
}
