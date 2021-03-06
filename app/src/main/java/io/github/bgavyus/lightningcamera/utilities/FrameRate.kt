package io.github.bgavyus.lightningcamera.utilities

// TODO: Convert to value class
data class FrameRate(val hertz: Int) {
    val isHighSpeed get() = hertz >= 120
}
