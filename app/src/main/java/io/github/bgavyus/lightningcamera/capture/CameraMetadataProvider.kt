package io.github.bgavyus.lightningcamera.capture

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CameraMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun collect() = withContext(Dispatchers.IO) {
        val manager = context.systemService<CameraManager>()

        val pairs = manager.cameraIdList
            .map { id -> Pair(id, manager.getCameraCharacteristics(id)) }

        (collectHighSpeed(pairs) ?: collectRegularSpeed(pairs))
            .also { Logger.info("Metadata collected: $it") }
    }

    private fun collectHighSpeed(cameras: Iterable<Pair<String, CameraCharacteristics>>): CameraMetadata? {
        val (id, characteristics) = cameras
            .find { (_, characteristics) -> characteristics.supportsHighSpeed }
            ?: return null

        val streamConfigurationMap = characteristics.streamConfigurationMap

        val (framesPerSecond, frameSize) = streamConfigurationMap.highSpeedVideoFpsRanges
            .asSequence()
            .filter(Range<Int>::isSingular)
            .flatMap { fpsRange ->
                streamConfigurationMap.getHighSpeedVideoSizesFor(fpsRange)
                    .asSequence()
                    .map { frameSize -> Pair(fpsRange.upper, frameSize) }
            }
            .requireMaxBy { (framesPerSecond, frameSize) -> framesPerSecond * frameSize.area }

        return CameraMetadata(id, characteristics.sensorOrientation, framesPerSecond, frameSize)
    }

    private fun collectRegularSpeed(cameras: Iterable<Pair<String, CameraCharacteristics>>): CameraMetadata {
        val (id, characteristics, framesPerSecond) = cameras
            .asSequence()
            .map { (id, characteristics) ->
                val framesPerSecond = characteristics.fpsRanges
                    .asSequence()
                    .filter(Range<Int>::isSingular)
                    .maxOf(Range<Int>::getUpper)

                Triple(id, characteristics, framesPerSecond)
            }
            .requireMaxBy { (_, _, framesPerSecond) -> framesPerSecond }

        val frameSize = characteristics.streamConfigurationMap
            .getOutputSizes(SurfaceTexture::class.java)
            .asSequence()
            .requireMaxBy(Size::area)

        return CameraMetadata(id, characteristics.sensorOrientation, framesPerSecond, frameSize)
    }
}
