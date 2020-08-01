package io.github.bgavyus.splash.capture

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import android.util.Size
import dagger.hilt.android.qualifiers.ActivityContext
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.area
import io.github.bgavyus.splash.common.middle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraMetadata(
    private val context: Context
) {
    companion object {
        private val TAG = CameraMetadata::class.simpleName
    }

    lateinit var id: String
    lateinit var orientation: Rotation
    lateinit var fpsRange: Range<Int>
    lateinit var videoSize: Size

    suspend fun collect() = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(CameraManager::class.java)
            ?: throw RuntimeException("Failed to get camera manager service")

        try {
            id = manager.highSpeedCameraId()
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
            videoSize = config.getHighSpeedVideoSizesFor(fpsRange).minBy { it.area }
                ?.also { Log.d(TAG, "Size: $it") }
                ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)
        } catch (error: CameraAccessException) {
            throw CameraError.fromAccessException(error)
        }
    }
}

private fun CameraManager.highSpeedCameraId() = cameraIdList.firstOrNull {
    val characteristics = getCameraCharacteristics(it)
    val capabilities =
        characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
            ?: return@firstOrNull false

    return@firstOrNull CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
} ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)
