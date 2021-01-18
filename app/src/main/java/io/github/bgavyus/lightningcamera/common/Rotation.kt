package io.github.bgavyus.lightningcamera.common

import android.view.Surface

enum class Rotation {
    Natural,
    Right,
    UpsideDown,
    Left,
    ;

    companion object {
        private const val fullCycleDegrees = 360
        private val rotations = values()

        private fun fromIndex(index: Int) = rotations[Math.floorMod(index, rotations.size)]

        fun fromSurfaceRotation(surfaceRotation: Int) = when (surfaceRotation) {
            Surface.ROTATION_0 -> Natural
            Surface.ROTATION_90 -> Right
            Surface.ROTATION_180 -> UpsideDown
            Surface.ROTATION_270 -> Left
            else -> throw IllegalArgumentException()
        }

        fun fromDegrees(degrees: Int) = fromIndex(degrees * rotations.size / fullCycleDegrees)
    }

    val degrees: Int get() = ordinal * fullCycleDegrees / rotations.size
    val isLandscape get() = this == Right || this == Left

    operator fun minus(other: Rotation) = fromIndex(ordinal - other.ordinal)
}
