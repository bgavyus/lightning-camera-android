package io.github.bgavyus.lightningcamera.utilities

inline class Hertz(val value: Int) {
    val isHighSpeed get() = value >= 120
}
