package io.github.bgavyus.lightningcamera.graphics

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Rational
import android.util.Size
import android.util.SizeF
import androidx.core.graphics.toPointF
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.extensions.android.util.aspectRatio
import io.github.bgavyus.lightningcamera.extensions.android.util.center
import io.github.bgavyus.lightningcamera.logging.Logger

object TransformMatrixFactory {
    fun create(rotation: Degrees, inputSize: Size, outputSize: Size) = Matrix().apply {
        val frameCenter = outputSize.center.toPointF()
        postRotate(-rotation.value.toFloat(), frameCenter)

        val inputRatio = inputSize.aspectRatio
        val outputRatio = outputSize.aspectRatio

        val (widthScale, heightScale) = if (rotation.isLandscape) {
            if (inputRatio > outputRatio) {
                Logger.log("Adding horizontal bars")
                outputRatio to inputRatio.reciprocal
            } else {
                Logger.log("Adding vertical bars")
                inputRatio to outputRatio.reciprocal
            }
        } else {
            if (inputRatio.reciprocal > outputRatio) {
                Logger.log("Adding horizontal bars")
                1 to inputRatio * outputRatio
            } else {
                Logger.log("Adding vertical bars")
                inputRatio.reciprocal * outputRatio.reciprocal to 1
            }
        }

        val scale = SizeF(widthScale.toFloat(), heightScale.toFloat())
        postScale(scale, frameCenter)
        Logger.log("Matrix Created: $outputSize, $inputSize, $rotation -> $this")
    }
}

operator fun Rational.times(other: Rational) =
    Rational(numerator * other.numerator, denominator * other.denominator)

fun Matrix.postRotate(degrees: Float, pivot: PointF) = postRotate(degrees, pivot.x, pivot.y)

fun Matrix.postScale(scale: SizeF, pivot: PointF) =
    postScale(scale.width, scale.height, pivot.x, pivot.y)

val Rational.reciprocal get() = Rational(denominator, numerator)
