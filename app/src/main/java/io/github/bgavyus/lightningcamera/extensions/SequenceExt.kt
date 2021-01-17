package io.github.bgavyus.lightningcamera.extensions

fun <T, R : Comparable<R>> Sequence<T>.requireMaxBy(selector: (T) -> R) =
    maxByOrNull(selector) ?: throw RuntimeException()
