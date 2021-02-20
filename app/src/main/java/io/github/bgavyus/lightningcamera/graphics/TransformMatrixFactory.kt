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
import io.github.bgavyus.lightningcamera.utilities.Degrees

object TransformMatrixFactory {
    fun create(rotation: Degrees, inputSize: Size, outputSize: Size) = Matrix().apply {
        val outputCenter = outputSize.center.toPointF()
        postRotate(-rotation.value.toFloat(), outputCenter)

        val inputRatio = inputSize.aspectRatio
        val outputRatio = outputSize.aspectRatio

        val (widthScale, heightScale) = if (rotation.isLandscape) {
            if (inputRatio > outputRatio) {
                outputRatio to inputRatio.reciprocal
            } else {
                inputRatio to outputRatio.reciprocal
            }
        } else {
            if (inputRatio.reciprocal > outputRatio) {
                1 to inputRatio * outputRatio
            } else {
                inputRatio.reciprocal * outputRatio.reciprocal to 1
            }
        }

        val scale = SizeF(widthScale.toFloat(), heightScale.toFloat())
        postScale(scale, outputCenter)
    }
}
