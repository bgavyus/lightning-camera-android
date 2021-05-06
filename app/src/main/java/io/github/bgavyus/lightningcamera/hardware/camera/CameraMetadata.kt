package io.github.bgavyus.lightningcamera.hardware.camera

import android.util.Size
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import io.github.bgavyus.lightningcamera.utilities.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val captureRate: FrameRate,
    val frameSize: Size,
) {
    val previewRate get() = if (captureRate.isHighSpeed) FrameRate(30) else captureRate
}
