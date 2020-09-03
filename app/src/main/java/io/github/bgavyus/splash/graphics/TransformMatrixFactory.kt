package io.github.bgavyus.splash.graphics

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Size
import androidx.core.graphics.toPointF
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.extensions.aspectRatio
import io.github.bgavyus.splash.common.extensions.center

class TransformMatrixFactory {
    companion object {
        fun create(frameSize: Size, bufferSize: Size, rotation: Rotation) = Matrix().apply {
            val scale = if (rotation.isLandscape) {
                if (frameSize.aspectRatio > 1) {
                    PointF(bufferSize.aspectRatio, 1 / frameSize.aspectRatio)
                } else {
                    PointF(frameSize.aspectRatio, 1 / bufferSize.aspectRatio)
                }
            } else {
                if (frameSize.aspectRatio > 1) {
                    PointF(1 / bufferSize.aspectRatio / frameSize.aspectRatio, 1f)
                } else {
                    PointF(1f, bufferSize.aspectRatio * frameSize.aspectRatio)
                }
            }

            val frameCenter = frameSize.center.toPointF()
            postRotate(-rotation.degrees.toFloat(), frameCenter.x, frameCenter.y)
            postScale(scale.x, scale.y, frameCenter.x, frameCenter.y)
        }
    }
}

private val Rotation.isLandscape get() = equals(Rotation.Right) || equals(Rotation.Left)
