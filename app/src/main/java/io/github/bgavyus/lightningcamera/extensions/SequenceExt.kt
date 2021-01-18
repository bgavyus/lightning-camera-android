package io.github.bgavyus.lightningcamera.extensions

fun <T, R : Comparable<R>> Sequence<T>.getMaxBy(selector: (T) -> R) =
    maxByOrNull(selector) ?: throw NoSuchElementException()
