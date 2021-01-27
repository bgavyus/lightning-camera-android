package io.github.bgavyus.lightningcamera.extensions

val Throwable.callerStackTraceElement: StackTraceElement get() = stackTrace[1]
