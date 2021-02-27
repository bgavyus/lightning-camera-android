package io.github.bgavyus.lightningcamera.utilities

// TODO: Convert to value class
data class Hertz(val value: Int) {
    val isHighSpeed get() = value >= 120
}
