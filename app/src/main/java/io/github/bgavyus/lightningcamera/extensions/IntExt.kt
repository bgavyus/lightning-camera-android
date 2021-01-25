package io.github.bgavyus.lightningcamera.extensions

operator fun Int.contains(mask: Int) = this and mask == mask

val Int.isPositive get() = this > 0

val Int.isHighSpeed get() = this >= 120
