package io.github.bgavyus.splash.capture

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import android.util.Size
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.area
import io.github.bgavyus.splash.common.middle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Camera private constructor() {
    companion object {
        private val TAG = CameraMetadata::class.simpleName

        suspend fun init() = withContext(Dispatchers.IO) { Camera() }

        val manager by lazy {
            App.context.getSystemService(CameraManager::class.java)
                ?: throw RuntimeException("Failed to get camera manager service")
        }
    }

    val id: String
    val orientation: Rotation
    val fpsRange: Range<Int>
    val size: Size

    init {
        try {
            id = manager.cameraIdList.firstOrNull {
                val characteristics = manager.getCameraCharacteristics(it)
                val capabilities =
                    characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
                        ?: return@firstOrNull false

                return@firstOrNull CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
            } ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

            val characteristics = manager.getCameraCharacteristics(id)

            val config = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: throw CameraError(CameraErrorType.Unknown)

            orientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]
                ?.let { Rotation.fromDegrees(it) }
                ?.also { Log.d(TAG, "Orientation: $it") }
                ?: throw CameraError(CameraErrorType.Unknown)

            fpsRange = config.highSpeedVideoFpsRanges.maxBy { it.middle }
                ?.also { Log.d(TAG, "FPS Range: $it") }
                ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

            // TODO: Choose video size based on performance
            size = config.getHighSpeedVideoSizesFor(fpsRange).minBy { it.area }
                ?.also { Log.d(TAG, "Size: $it") }
                ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)
        } catch (error: CameraAccessException) {
            throw CameraError(CameraErrorType.fromAccessException(error))
        }
    }
}
