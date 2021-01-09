package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.common.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val framesPerSecond: Int,
    val frameSize: Size,
)
