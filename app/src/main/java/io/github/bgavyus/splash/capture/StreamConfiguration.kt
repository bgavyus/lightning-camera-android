package io.github.bgavyus.splash.capture

import android.util.Size
import io.github.bgavyus.splash.common.extensions.area

data class StreamConfiguration(
    val framesPerSecond: Int,
    val frameSize: Size
) {
    val pixelRate get() = framesPerSecond * frameSize.area
}
