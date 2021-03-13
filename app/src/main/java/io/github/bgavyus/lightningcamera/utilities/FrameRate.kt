package io.github.bgavyus.lightningcamera.utilities

data class FrameRate(val fps: Int) {
    val isHighSpeed get() = fps >= 120
}
