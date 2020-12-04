package io.github.bgavyus.lightningcamera.common.extensions

import android.graphics.Point
import android.util.Size

val Size.area get() = width * height
val Size.aspectRatio get() = width.toFloat() / height
val Size.center get() = Point(width / 2, height / 2)
