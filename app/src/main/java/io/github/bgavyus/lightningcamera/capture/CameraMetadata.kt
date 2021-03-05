package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.utilities.Rotation
import io.github.bgavyus.lightningcamera.utilities.Hertz

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val frameRate: Hertz,
    val frameSize: Size,
)
