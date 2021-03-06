package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import io.github.bgavyus.lightningcamera.utilities.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val frameRate: FrameRate,
    val frameSize: Size,
)
