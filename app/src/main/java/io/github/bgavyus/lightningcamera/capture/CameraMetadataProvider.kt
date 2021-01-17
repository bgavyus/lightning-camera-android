package io.github.bgavyus.lightningcamera.capture

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
import android.util.Range
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.common.extensions.area
import io.github.bgavyus.lightningcamera.common.extensions.systemService
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

        return CameraMetadata(id, characteristics.orientation, framesPerSecond, frameSize)
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

        return CameraMetadata(id, characteristics.orientation, framesPerSecond, frameSize)
    }
}

private val CameraCharacteristics.supportsHighSpeed
    get() = REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities

private val CameraCharacteristics.capabilities
    get() = requireGet(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

private val CameraCharacteristics.streamConfigurationMap
    get() = requireGet(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

private val CameraCharacteristics.orientation
    get() = Rotation.fromDegrees(requireGet(CameraCharacteristics.SENSOR_ORIENTATION))

private val CameraCharacteristics.fpsRanges
    get() = requireGet(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

private fun <T> CameraCharacteristics.requireGet(key: CameraCharacteristics.Key<T>) =
    get(key) ?: throw RuntimeException()

private fun <T, R : Comparable<R>> Sequence<T>.requireMaxBy(selector: (T) -> R) =
    maxByOrNull(selector) ?: throw RuntimeException()

private val <T : Comparable<T>> Range<T>.isSingular get() = lower == upper
