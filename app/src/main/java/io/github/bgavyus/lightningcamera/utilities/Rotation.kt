package io.github.bgavyus.lightningcamera.utilities

import android.view.Surface
import io.github.bgavyus.lightningcamera.extensions.floorMod

enum class Rotation {
    Natural,
    Right,
    UpsideDown,
    Left,
    ;

    val degrees get() = ordinal * fullCycleDegrees / entries.size
    val isLandscape get() = this == Right || this == Left

    operator fun minus(other: Rotation) = fromIndex(ordinal - other.ordinal)

    companion object {
        private const val fullCycleDegrees = 360

        private fun fromIndex(index: Int) = entries[index floorMod entries.size]

        fun fromSurfaceRotation(surfaceRotation: Int) = when (surfaceRotation) {
            Surface.ROTATION_0 -> Natural
            Surface.ROTATION_90 -> Right
            Surface.ROTATION_180 -> UpsideDown
            Surface.ROTATION_270 -> Left
            else -> throw IllegalArgumentException()
        }

        fun fromDegrees(degrees: Int) = fromIndex(degrees * entries.size / fullCycleDegrees)
    }
}
