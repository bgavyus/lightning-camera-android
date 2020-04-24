package io.github.bgavyus.splash.camera

import android.annotation.SuppressLint
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Range
import android.util.Size
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.ReleaseStack
import io.github.bgavyus.splash.common.Rotation


@SuppressLint("MissingPermission")
class HighSpeedCamera(val listener: CameraListener) :
    CameraCaptureSession.CaptureCallback() {
    companion object {
        private val TAG = HighSpeedCamera::class.simpleName
    }

    private val releaseStack = ReleaseStack()

    private val cameraManager: CameraManager =
        App.shared.getSystemService(CameraManager::class.java)
            ?: throw CameraError(CameraErrorType.Generic)

    private val handler = Handler(App.shared.mainLooper)
    private val cameraId: String

    val sensorOrientation: Rotation
    val fpsRange: Range<Int>
    val videoSize: Size

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities =
                    characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
                        ?: return@firstOrNull false

                return@firstOrNull CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
            } ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val config = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: throw CameraError(CameraErrorType.Generic)

            sensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]
                ?.let { Rotation.fromDegrees(it) }
                ?.also { Log.d(TAG, "Camera Orientation: $it") }
                ?: throw CameraError(CameraErrorType.Generic)

            fpsRange = config.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }
                ?.also { Log.d(TAG, "FPS Range: $it") }
                ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

            videoSize = config.getHighSpeedVideoSizesFor(fpsRange).maxBy { it.width * it.height }
                ?.also { Log.d(TAG, "Video Size: $it") }
                ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)
        } catch (error: CameraAccessException) {
            throw CameraError(accessExceptionToErrorType(error))
        }
    }

    fun startStreaming() {
        try {
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, handler)
        } catch (error: CameraAccessException) {
            throw CameraError(accessExceptionToErrorType(error))
        }
    }

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened")
            releaseStack.push(camera::close)
            val surfaces = listener.onSurfacesNeeded()

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    camera.createConstrainedHighSpeedCaptureSession(
                        surfaces,
                        cameraCaptureSessionStateCallback,
                        handler
                    )
                } else {
                    camera.createCaptureSession(
                        SessionConfiguration(
                            SessionConfiguration.SESSION_HIGH_SPEED,
                            surfaces.map(::OutputConfiguration),
                            { handler.post(it) },
                            cameraCaptureSessionStateCallback
                        )
                    )
                }
            } catch (error: CameraAccessException) {
                onError(accessExceptionToErrorType(error))
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected")
            onError(CameraErrorType.Disconnected)
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "CameraDevice.onError(error = $error)")

            onError(
                when (error) {
                    ERROR_CAMERA_IN_USE -> CameraErrorType.InUse
                    ERROR_MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
                    ERROR_CAMERA_DISABLED -> CameraErrorType.Disabled
                    ERROR_CAMERA_DEVICE -> CameraErrorType.Device
                    else -> CameraErrorType.Generic
                }
            )
        }
    }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigured")
            startCaptureSession(cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession)
            listener.onCameraStreamStarted()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
            onError(CameraErrorType.ConfigureFailed)
        }
    }

    private fun startCaptureSession(captureSession: CameraConstrainedHighSpeedCaptureSession) {
        val surfaces = listener.onSurfacesNeeded()

        try {
            captureSession.run {
                setRepeatingBurst(
                    createHighSpeedRequestList(
                        device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                            surfaces.forEach(::addTarget)
                        }.build()
                    ), null, null
                )

                releaseStack.push(::close)
            }
        } catch (error: CameraAccessException) {
            onError(accessExceptionToErrorType(error))
        } catch (_: IllegalStateException) {
            onError(CameraErrorType.Generic)
        }
    }

    private fun accessExceptionToErrorType(error: CameraAccessException): CameraErrorType {
        return when (error.reason) {
            CameraAccessException.CAMERA_IN_USE -> CameraErrorType.InUse
            CameraAccessException.MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
            CameraAccessException.CAMERA_DISABLED -> CameraErrorType.Disabled
            CameraAccessException.CAMERA_ERROR -> CameraErrorType.Device
            else -> CameraErrorType.Generic
        }
    }

    private fun onError(type: CameraErrorType) {
        release()
        listener.onCameraError(type)
    }

    fun release() {
        releaseStack.release()
    }
}
