package io.github.bgavyus.lightningcamera.utilities

import android.view.Surface
import io.github.bgavyus.lightningcamera.extensions.floorMod

// TODO: Convert to value class
class Degrees(val value: Int) {
    companion object {
        fun fromSurfaceRotation(surfaceRotation: Int) = Degrees(when (surfaceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalArgumentException()
        })
    }

    operator fun minus(other: Degrees) = Degrees(value - other.value)
    val isLandscape get() = value floorMod 180 == 90
    val normalized get() = Degrees(value floorMod 360)
}
