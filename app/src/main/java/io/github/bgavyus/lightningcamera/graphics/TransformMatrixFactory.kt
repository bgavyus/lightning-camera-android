package io.github.bgavyus.lightningcamera.graphics

import android.graphics.Matrix
import android.util.Size
import android.util.SizeF
import androidx.core.graphics.toPointF
import io.github.bgavyus.lightningcamera.extensions.android.graphics.postRotate
import io.github.bgavyus.lightningcamera.extensions.android.graphics.postScale
import io.github.bgavyus.lightningcamera.extensions.android.util.aspectRatio
import io.github.bgavyus.lightningcamera.extensions.android.util.center
import io.github.bgavyus.lightningcamera.extensions.android.util.reciprocal
import io.github.bgavyus.lightningcamera.extensions.android.util.times
import io.github.bgavyus.lightningcamera.utilities.Rotation

object TransformMatrixFactory {
    fun create(
        rotation: Rotation,
        inputSize: Size,
        outputSize: Size,
        fill: Boolean,
    ) = Matrix().apply {
        val outputCenter = outputSize.center.toPointF()
        postRotate(-rotation.degrees.toFloat(), outputCenter)

        val inputRatio = inputSize.aspectRatio
        val outputRatio = outputSize.aspectRatio

        val (widthScale, heightScale) = if (rotation.isLandscape) {
            if (inputRatio > outputRatio != fill) {
                outputRatio to inputRatio.reciprocal
            } else {
                inputRatio to outputRatio.reciprocal
            }
        } else {
            if (inputRatio.reciprocal > outputRatio != fill) {
                1 to inputRatio * outputRatio
            } else {
                inputRatio.reciprocal * outputRatio.reciprocal to 1
            }
        }

        val scale = SizeF(widthScale.toFloat(), heightScale.toFloat())
        postScale(scale, outputCenter)
    }
}
