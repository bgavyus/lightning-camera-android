package io.github.bgavyus.lightningcamera.extensions

val Int.isPositive get() = this > 0

infix fun Int.floorMod(other: Int) = Math.floorMod(this, other)
