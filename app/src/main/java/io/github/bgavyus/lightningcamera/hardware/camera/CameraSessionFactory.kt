package io.github.bgavyus.lightningcamera.hardware.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.Surface
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.createCaptureSession
import io.github.bgavyus.lightningcamera.extensions.toRange
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import javax.inject.Inject

class CameraSessionFactory @Inject constructor(
    private val handler: Handler,
) {
    companion object {
        const val infinityFocus = 0f
    }

    suspend fun create(
        device: CameraDevice,
        surfaces: List<Surface>,
        frameRate: FrameRate,
    ): CameraCaptureSession {
        val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, frameRate.fps.toRange())
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, infinityFocus)
            surfaces.forEach(::addTarget)
        }.build()

        return device.createCaptureSession(
            frameRate.isHighSpeed,
            surfaces,
            handler,
            Dispatchers.IO.asExecutor()
        ).apply {
            if (frameRate.isHighSpeed) {
                val highSpeedCaptureSession = this as CameraConstrainedHighSpeedCaptureSession
                val requests = highSpeedCaptureSession.createHighSpeedRequestList(captureRequest)
                setRepeatingBurst(requests, null, null)
            } else {
                setRepeatingRequest(captureRequest, null, null)
            }
        }
    }
}
