package io.github.bgavyus.lightningcamera.extensions

operator fun Int.contains(mask: Int) = this and mask == mask

val Int.isHighSpeed get() = this >= 120
