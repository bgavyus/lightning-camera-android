package io.github.bgavyus.lightningcamera.extensions.android.util

import android.graphics.Point
import android.util.Rational
import android.util.Size

val Size.area get() = width * height
val Size.aspectRatio get() = Rational(width, height)
val Size.has16To9AspectRatio get() = aspectRatio == Rational(16, 9)
val Size.center get() = Point(width / 2, height / 2)
