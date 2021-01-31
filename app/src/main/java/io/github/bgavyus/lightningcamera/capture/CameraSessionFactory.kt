package io.github.bgavyus.lightningcamera.capture

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Hertz
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.createCaptureSession
import io.github.bgavyus.lightningcamera.extensions.toRange

class CameraSessionFactory : DeferScope() {
    companion object {
        const val infinityFocus = 0f
    }

    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    suspend fun create(
        device: CameraDevice,
        surfaces: List<Surface>,
        frameRate: Hertz,
    ): CameraCaptureSession {
        val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, frameRate.value.toRange())
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, infinityFocus)
            surfaces.forEach(::addTarget)
        }.build()

        return device.createCaptureSession(frameRate.isHighSpeed, surfaces, handler).apply {
            if (frameRate.isHighSpeed) {
                val highSpeedCaptureSession = this as CameraConstrainedHighSpeedCaptureSession
                val requests = highSpeedCaptureSession.createHighSpeedRequestList(captureRequest)
                setRepeatingBurst(requests, null, handler)
            } else {
                setRepeatingRequest(captureRequest, null, handler)
            }
        }
    }
}
