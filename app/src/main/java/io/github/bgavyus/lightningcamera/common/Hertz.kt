package io.github.bgavyus.lightningcamera.common

inline class Hertz(val value: Int) {
    val isHighSpeed get() = value >= 120
}
