package io.github.bgavyus.lightningcamera.extensions

val Int.isPositive get() = this > 0

infix fun Int.containsFlags(mask: Int) = this and mask == mask

infix fun Int.floorMod(other: Int) = Math.floorMod(this, other)

// TODO: move to inline class: Hertz
val Int.isHighSpeed get() = this >= 120
