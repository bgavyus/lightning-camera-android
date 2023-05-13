package io.github.bgavyus.lightningcamera.extensions

import java.util.Collections

fun <E> List<E>.rotate(distance: Int) = Collections.rotate(this, distance)
