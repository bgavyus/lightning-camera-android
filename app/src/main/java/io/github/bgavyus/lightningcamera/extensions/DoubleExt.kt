package io.github.bgavyus.lightningcamera.extensions

import kotlin.math.absoluteValue

infix fun Double.distance(other: Double) = (this - other).absoluteValue
