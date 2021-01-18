package io.github.bgavyus.lightningcamera.extensions.android.util

import android.graphics.Point
import android.util.Size

val Size.area get() = width * height
val Size.aspectRatio get() = width.toFloat() / height
val Size.isWide get() = aspectRatio == 16f / 9
val Size.center get() = Point(width / 2, height / 2)
