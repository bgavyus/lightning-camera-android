package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.common.Degrees

data class CameraMetadata(
    val id: String,
    val orientation: Degrees,
    val framesPerSecond: Int,
    val frameSize: Size,
)
