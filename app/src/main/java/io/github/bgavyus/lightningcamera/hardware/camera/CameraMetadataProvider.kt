package io.github.bgavyus.lightningcamera.hardware.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.android.content.requireSystemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.fpsRanges
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.params.getOutputMaxFps
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.sensorOrientation
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.streamConfigurationMap
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.android.util.isSingular
import io.github.bgavyus.lightningcamera.extensions.getMaxBy
import io.github.bgavyus.lightningcamera.extensions.toRange
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import io.github.bgavyus.lightningcamera.utilities.Rotation
import javax.inject.Inject

class CameraMetadataProvider @Inject constructor(
    private val context: Context,
) {
    fun collect(): CameraMetadata {
        val manager: CameraManager = context.requireSystemService()

        val (id, characteristics, captureRate) = manager.cameraIdList
            .asSequence()
            .map { id ->
                val characteristics = manager.getCameraCharacteristics(id)

                val (fpsRange, isHighSpeed) = characteristics.streamConfigurationMap
                    .highSpeedVideoFpsRanges
                    .map { it to true }
                    .ifEmpty { characteristics.fpsRanges.map { it to false } }
                    .also { Logger.log("Frame rates for camera $id: ${it.joinToString()}") }
                    .asSequence()
                    .filter { (fpsRange, _) -> fpsRange.isSingular }
                    .getMaxBy { (fpsRange, _) -> fpsRange.upper }

                Triple(id, characteristics, FrameRate(fpsRange.upper, isHighSpeed))
            }
            .getMaxBy { (_, _, frameRate) -> frameRate.fps }

        val orientation = Rotation.fromDegrees(characteristics.sensorOrientation)
        val streamConfigurationMap = characteristics.streamConfigurationMap

        val frameSize = if (captureRate.isHighSpeed) {
            streamConfigurationMap.getHighSpeedVideoSizesFor(captureRate.fps.toRange())
                .also { Logger.log("High speed frame sizes: ${it.joinToString()}") }
                .asSequence()
        } else {
            val format = ImageFormat.PRIVATE

            streamConfigurationMap.getOutputSizes(format)
                .map { it to streamConfigurationMap.getOutputMaxFps(format, it) }
                .also { Logger.log("Normal speed capture configurations: ${it.joinToString()}") }
                .asSequence()
                .filter { (size, fps) -> fps == captureRate.fps && size.area <= maxFramePixels }
                .map { (size, _) -> size }
        }
            .getMaxBy(Size::area)

        return CameraMetadata(id, orientation, captureRate, frameSize)
            .also { Logger.log("Metadata collected: $it") }
    }

    companion object {
        const val maxFramePixels = 0x40_00_00
    }
}
