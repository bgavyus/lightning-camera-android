package io.github.bgavyus.lightningcamera.extensions

val Float.normalized get() = if (this == -0f) 0f else this
