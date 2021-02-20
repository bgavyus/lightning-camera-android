package io.github.bgavyus.lightningcamera.utilities

import io.github.bgavyus.lightningcamera.extensions.isPositive

fun validatePositive(count: Int) = validate(count.isPositive)

fun validate(value: Boolean) {
    if (!value) {
        throw RuntimeException()
    }
}
