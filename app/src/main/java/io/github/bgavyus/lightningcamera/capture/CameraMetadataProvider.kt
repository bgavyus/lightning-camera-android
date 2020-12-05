package io.github.bgavyus.lightningcamera.capture

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.common.extensions.area
import io.github.bgavyus.lightningcamera.common.extensions.systemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CameraMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun highSpeed() = withContext(Dispatchers.IO) {
        val manager = context.systemService<CameraManager>()

        try {
            val (id, characteristics) = manager.cameraIdList
                .asSequence()
                .map { id -> Pair(id, manager.getCameraCharacteristics(id)) }
                .find { (_, characteristics) ->
                    characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
                        ?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)
                        ?: false
                } ?: throw CameraException(CameraExceptionType.HighSpeedNotAvailable)

            val orientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]
                ?.let { Rotation.fromDegrees(it) }
                ?: throw CameraException(CameraExceptionType.Unknown)

            val configMap = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: throw CameraException(CameraExceptionType.Unknown)

            val (framesPerSecond, frameSize) = configMap.highSpeedVideoFpsRanges
                .asSequence()
                .filter { it.lower == it.upper }
                .flatMap { fpsRange ->
                    configMap.getHighSpeedVideoSizesFor(fpsRange)
                        .asSequence()
                        .map { frameSize -> Pair(fpsRange.upper, frameSize) }
                }
                .maxByOrNull { (framesPerSecond, frameSize) -> framesPerSecond * frameSize.area }
                ?: throw CameraException(CameraExceptionType.HighSpeedNotAvailable)

            CameraMetadata(id, orientation, framesPerSecond, frameSize)
        } catch (error: CameraAccessException) {
            throw CameraException.fromAccessException(error)
        }
    }
}
