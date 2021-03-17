package io.github.bgavyus.lightningcamera.hardware.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.*
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.fpsRanges
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.params.getOutputMaxFps
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.sensorOrientation
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.streamConfigurationMap
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.android.util.isSingular
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import io.github.bgavyus.lightningcamera.utilities.Rotation
import javax.inject.Inject

class CameraMetadataProvider @Inject constructor(
    private val context: Context,
) {
    fun collect(): CameraMetadata {
        val manager = context.systemService<CameraManager>()

        val (id, characteristics, frameRate) = manager.cameraIdList
            .asSequence()
            .map { id ->
                val characteristics = manager.getCameraCharacteristics(id)

                val fps = characteristics.streamConfigurationMap
                    .highSpeedVideoFpsRanges
                    .ifEmpty(characteristics::fpsRanges)
                    .asSequence()
                    .filter(Range<Int>::isSingular)
                    .maxOf(Range<Int>::getUpper)

                Triple(id, characteristics, FrameRate(fps))
            }
            .getMaxBy { (_, _, frameRate) -> frameRate.fps }

        val orientation = Rotation.fromDegrees(characteristics.sensorOrientation)
        val streamConfigurationMap = characteristics.streamConfigurationMap

        val frameSize = if (frameRate.isHighSpeed) {
            streamConfigurationMap.getHighSpeedVideoSizesFor(frameRate.fps.toRange())
                .asSequence()
        } else {
            val format = ImageFormat.PRIVATE

            streamConfigurationMap.getOutputSizes(format)
                .asSequence()
                .filter { streamConfigurationMap.getOutputMaxFps(format, it) == frameRate.fps }
        }
            .getMaxBy(Size::area)

        return CameraMetadata(id, orientation, frameRate, frameSize)
            .also { Logger.log("Metadata collected: $it") }
    }
}
