package io.github.bgavyus.lightningcamera.extensions.android.graphics

import android.graphics.Matrix
import android.graphics.PointF
import android.util.SizeF

fun Matrix.postRotate(degrees: Float, center: PointF) = postRotate(degrees, center.x, center.y)

fun Matrix.postScale(scale: SizeF, center: PointF) =
    postScale(scale.width, scale.height, center.x, center.y)
