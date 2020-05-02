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
        private val rotationsCount = rotations.count()
        private val degreesPerRotation = FULL_CYCLE_DEGREES / rotationsCount

        fun fromSurfaceRotation(surfaceRotation: Int): Rotation {
            return rotations.first { it.surfaceRotation == surfaceRotation }
        }

        fun fromDegrees(degrees: Int): Rotation {
            return fromOrdinal(degrees / degreesPerRotation)
        }

        private fun fromOrdinal(index: Int): Rotation {
            return rotations[floorMod(index, rotationsCount)]
        }
    }

    val degrees: Int get() = ordinal * degreesPerRotation

    operator fun unaryMinus(): Rotation {
        return fromOrdinal(
            -ordinal
        )
    }

    operator fun plus(other: Rotation): Rotation {
        return fromOrdinal(
            ordinal + other.ordinal
        )
    }

    operator fun minus(other: Rotation): Rotation {
        return fromOrdinal(
            ordinal - other.ordinal
        )
    }
}
