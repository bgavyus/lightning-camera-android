package io.github.bgavyus.lightningcamera.capture

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.common.Hertz
import io.github.bgavyus.lightningcamera.extensions.*
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.fpsRanges
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.sensorOrientation
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.streamConfigurationMap
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.android.util.has16To9AspectRatio
import io.github.bgavyus.lightningcamera.extensions.android.util.isSingular
import io.github.bgavyus.lightningcamera.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CameraMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun collect() = withContext(Dispatchers.IO) {
        val manager = context.systemService<CameraManager>()

        val (id, characteristics, framesPerSecond) = manager.cameraIdList
            .asSequence()
            .map { id ->
                val characteristics = manager.getCameraCharacteristics(id)

                val framesPerSecond = characteristics.streamConfigurationMap
                    .highSpeedVideoFpsRanges
                    .ifEmpty(characteristics::fpsRanges)
                    .asSequence()
                    .filter(Range<Int>::isSingular)
                    .maxOf(Range<Int>::getUpper)

                Triple(id, characteristics, framesPerSecond)
            }
            .getMaxBy { (_, _, framesPerSecond) -> framesPerSecond }

        val orientation = Degrees(characteristics.sensorOrientation)
        val frameRate = Hertz(framesPerSecond)
        val streamConfigurationMap = characteristics.streamConfigurationMap

        val frameSize = if (frameRate.isHighSpeed) {
            streamConfigurationMap.getHighSpeedVideoSizesFor(framesPerSecond.toRange())
        } else {
            streamConfigurationMap.getOutputSizes(ImageFormat.PRIVATE)
        }
            .asSequence()
            .filter(Size::has16To9AspectRatio)
            .getMaxBy(Size::area)

        CameraMetadata(id, orientation, frameRate, frameSize)
            .also { Logger.log("Metadata collected: $it") }
    }
}
