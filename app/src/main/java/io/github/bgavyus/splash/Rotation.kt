package io.github.bgavyus.splash

import android.content.Context
import android.view.Surface
import android.view.WindowManager
import java.lang.RuntimeException

enum class Rotation(val surfaceRotation: Int) {
	Natural(Surface.ROTATION_0),
	Right(Surface.ROTATION_90),
	UpsideDown(Surface.ROTATION_180),
	Left(Surface.ROTATION_270);

	companion object {
		private val rotations = values()
		private val rotationsCount = rotations.count()
		private val degreesPerRotation = 360 / rotationsCount

		fun fromSurfaceRotation(surfaceRotation: Int): Rotation {
			return rotations.first { it.surfaceRotation == surfaceRotation }
		}

		private fun fromOrdinal(index: Int): Rotation {
			return rotations[Math.floorMod(index, rotationsCount)]
		}

		fun fromDegrees(degrees: Int): Rotation {
			return fromOrdinal(degrees / degreesPerRotation)
		}
	}

	val degrees: Int
		get() = ordinal * degreesPerRotation

	operator fun unaryMinus(): Rotation {
		return fromOrdinal(-ordinal)
	}

	operator fun plus(other: Rotation): Rotation {
		return fromOrdinal(ordinal + other.ordinal)
	}

	operator fun minus(other: Rotation): Rotation {
		return fromOrdinal(ordinal - other.ordinal)
	}
}

val Context.displayRotation: Rotation
	get() = getSystemService(WindowManager::class.java)?.let {
		Rotation.fromSurfaceRotation(it.defaultDisplay.rotation)
	} ?: throw RuntimeException("Failed to get display rotation")

val Context.deviceOrientation: Rotation
	get() = -displayRotation
