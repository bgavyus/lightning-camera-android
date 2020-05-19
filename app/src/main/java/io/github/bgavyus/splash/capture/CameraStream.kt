package io.github.bgavyus.splash.capture

import android.annotation.SuppressLint
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Log
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.graphics.ImageConsumer

@SuppressLint("MissingPermission")
class CameraStream(
    private val camera: Camera,
    private val consumers: Iterable<ImageConsumer>,
    private val listener: CameraListener
) : AutoCloseable {
    companion object {
        private val TAG = CameraStream::class.simpleName
    }

    private val closeStack = CloseStack()
    private val handler = Handler(App.context.mainLooper)

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened")
            closeStack.push(camera)

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    camera.createConstrainedHighSpeedCaptureSession(
                        consumers.map { it.surface },
                        cameraCaptureSessionStateCallback,
                        handler
                    )
                } else {
                    camera.createCaptureSession(
                        SessionConfiguration(
                            SessionConfiguration.SESSION_HIGH_SPEED,
                            consumers.map { OutputConfiguration(it.surface) },
                            { handler.post(it) },
                            cameraCaptureSessionStateCallback
                        )
                    )
                }
            } catch (error: CameraAccessException) {
                onError(CameraErrorType.fromAccessException(error))
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

    init {
        val cameraManager = App.context.getSystemService(CameraManager::class.java)
            ?: throw CameraError(CameraErrorType.Generic)

        try {
            cameraManager.openCamera(camera.id, cameraDeviceStateCallback, handler)
        } catch (error: CameraAccessException) {
            throw CameraError(CameraErrorType.fromAccessException(error))
        }
    }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigured")
            startCaptureSession(cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession)
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
            onError(CameraErrorType.ConfigureFailed)
        }
    }

    private fun startCaptureSession(captureSession: CameraConstrainedHighSpeedCaptureSession) {
        try {
            captureSession.run {
                val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, camera.fpsRange)
                    consumers.forEach { addTarget(it.surface) }
                }

                val requests = createHighSpeedRequestList(builder.build())
                setRepeatingBurst(requests, /* listener = */ null, /* handler = */ null)
                closeStack.push(this)
            }
        } catch (error: CameraAccessException) {
            onError(CameraErrorType.fromAccessException(error))
        } catch (_: IllegalStateException) {
            onError(CameraErrorType.Generic)
        }
    }

    private fun onError(type: CameraErrorType) = listener.onCameraError(type)
    override fun close() = closeStack.close()
}
