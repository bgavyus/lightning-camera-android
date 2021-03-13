package io.github.bgavyus.lightningcamera.extensions

val StackTraceElement.fileNameWithoutExtension get() = fileName.substringBeforeLast(".")
