package io.github.bgavyus.splash.common

import android.view.Surface

enum class Rotation(val surfaceRotation: Int) {
    Natural(Surface.ROTATION_0),
    Right(Surface.ROTATION_90),
    UpsideDown(Surface.ROTATION_180),
    Left(Surface.ROTATION_270);

    companion object {
        private const val FULL_CYCLE_DEGREES = 360
        private val rotations = values()
        private fun fromIndex(index: Int) = rotations[Math.floorMod(index, rotations.size)]

        fun fromSurfaceRotation(surfaceRotation: Int) =
            rotations.first { it.surfaceRotation == surfaceRotation }

        fun fromDegrees(degrees: Int) = fromIndex(degrees * rotations.size / FULL_CYCLE_DEGREES)
    }

    val degrees: Int get() = ordinal * FULL_CYCLE_DEGREES / rotations.size

    operator fun unaryMinus() = fromIndex(-ordinal)
    operator fun plus(other: Rotation) = fromIndex(ordinal + other.ordinal)
    operator fun minus(other: Rotation) = fromIndex(ordinal - other.ordinal)
}
