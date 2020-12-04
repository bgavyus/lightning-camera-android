package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.common.extensions.area

data class StreamConfiguration(
    val framesPerSecond: Int,
    val frameSize: Size
) {
    val pixelRate get() = framesPerSecond * frameSize.area
}
