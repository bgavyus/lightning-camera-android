package io.github.bgavyus.lightningcamera.common

import android.view.Surface

enum class Rotation {
    Natural,
    Right,
    UpsideDown,
    Left;

    companion object {
        private const val FULL_CYCLE_DEGREES = 360
        private val rotations = values()

        private fun fromIndex(index: Int) = rotations[Math.floorMod(index, rotations.size)]

        fun fromSurfaceRotation(surfaceRotation: Int) = when (surfaceRotation) {
            Surface.ROTATION_0 -> Natural
            Surface.ROTATION_90 -> Right
            Surface.ROTATION_180 -> UpsideDown
            Surface.ROTATION_270 -> Left
            else -> throw IllegalArgumentException()
        }

        fun fromDegrees(degrees: Int) = fromIndex(degrees * rotations.size / FULL_CYCLE_DEGREES)
    }

    val degrees: Int get() = ordinal * FULL_CYCLE_DEGREES / rotations.size

    operator fun unaryMinus() = fromIndex(-ordinal)
    operator fun plus(other: Rotation) = fromIndex(ordinal + other.ordinal)
    operator fun minus(other: Rotation) = fromIndex(ordinal - other.ordinal)
}
