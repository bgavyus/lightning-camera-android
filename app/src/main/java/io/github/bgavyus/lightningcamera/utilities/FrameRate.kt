package io.github.bgavyus.lightningcamera.utilities

data class FrameRate(val hertz: Int) {
    val isHighSpeed get() = hertz >= 120
}
