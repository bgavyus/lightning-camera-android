package io.github.bgavyus.lightningcamera.common

import io.github.bgavyus.lightningcamera.utilities.Degrees
import org.junit.Assert.*
import org.junit.Test

class DegreesTest {
    @Test
    fun isLandscape() {
        assertFalse(Degrees(-360).isLandscape)
        assertTrue(Degrees(-270).isLandscape)
        assertFalse(Degrees(-180).isLandscape)
        assertTrue(Degrees(-90).isLandscape)
        assertFalse(Degrees(0).isLandscape)
        assertTrue(Degrees(90).isLandscape)
        assertFalse(Degrees(180).isLandscape)
        assertTrue(Degrees(270).isLandscape)
        assertFalse(Degrees(360).isLandscape)
    }

    @Test
    fun normalized() {
        assertEquals(0, Degrees(-360).normalized.value)
        assertEquals(90, Degrees(-270).normalized.value)
        assertEquals(180, Degrees(-180).normalized.value)
        assertEquals(270, Degrees(-90).normalized.value)
        assertEquals(0, Degrees(0).normalized.value)
        assertEquals(90, Degrees(90).normalized.value)
        assertEquals(180, Degrees(180).normalized.value)
        assertEquals(270, Degrees(270).normalized.value)
        assertEquals(0, Degrees(360).normalized.value)
    }

    @Test
    fun minus() {
        assertEquals(Degrees(180), Degrees(270) - Degrees(90))
        assertEquals(Degrees(-90), Degrees(0) - Degrees(90))
        assertEquals(Degrees(0), Degrees(180) - Degrees(180))
        assertEquals(Degrees(-180), Degrees(180) - Degrees(360))
    }
}
