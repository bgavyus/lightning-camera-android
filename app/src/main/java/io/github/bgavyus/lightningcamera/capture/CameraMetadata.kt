package io.github.bgavyus.lightningcamera.capture

import android.util.Size
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.common.Hertz

data class CameraMetadata(
    val id: String,
    val orientation: Degrees,
    val frameRate: Hertz,
    val frameSize: Size,
)
