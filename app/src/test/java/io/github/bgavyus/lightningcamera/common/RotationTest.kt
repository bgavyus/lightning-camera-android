package io.github.bgavyus.lightningcamera.common

import io.github.bgavyus.lightningcamera.utilities.Rotation
import org.junit.Assert.*
import org.junit.Test

class RotationTest {
    @Test
    fun isLandscape() {
        assertFalse(Rotation.Natural.isLandscape)
        assertTrue(Rotation.Right.isLandscape)
        assertFalse(Rotation.UpsideDown.isLandscape)
        assertTrue(Rotation.Left.isLandscape)
    }

    @Test
    fun minus() {
        assertEquals(Rotation.UpsideDown, Rotation.Left - Rotation.Right)
        assertEquals(Rotation.Left, Rotation.Natural - Rotation.Right)
        assertEquals(Rotation.Natural, Rotation.UpsideDown - Rotation.UpsideDown)
        assertEquals(Rotation.UpsideDown, Rotation.UpsideDown - Rotation.Natural)
    }
}
