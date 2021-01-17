package io.github.bgavyus.lightningcamera.capture

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Range
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraSessionFactory : DeferScope() {
    companion object {
        const val infinityFocus = 0f
        const val highSpeedMinimalFps = 120
    }

    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    suspend fun create(
        device: CameraDevice,
        surfaces: List<Surface>,
        framesPerSecond: Int,
    ): CameraCaptureSession {
        val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, framesPerSecond.toRange())
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, infinityFocus)
            surfaces.forEach(::addTarget)
        }.build()

        val isHighSpeed = framesPerSecond >= highSpeedMinimalFps

        return device.createCaptureSession(isHighSpeed, surfaces, handler).apply {
            if (isHighSpeed) {
                val highSpeedCaptureSession = this as CameraConstrainedHighSpeedCaptureSession
                val requests = highSpeedCaptureSession.createHighSpeedRequestList(captureRequest)
                setRepeatingBurst(requests, null, handler)
            } else {
                setRepeatingRequest(captureRequest, null, handler)
            }
        }
    }
}

private suspend fun CameraDevice.createCaptureSession(
    isHighSpeed: Boolean,
    surfaces: List<Surface>,
    handler: Handler,
): CameraCaptureSession = suspendCoroutine { continuation ->
    val callback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) =
            continuation.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) =
            continuation.resumeWithException(RuntimeException())
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        if (isHighSpeed) {
            createConstrainedHighSpeedCaptureSession(surfaces, callback, handler)
        } else {
            createCaptureSession(surfaces, callback, handler)
        }
    } else {
        val sessionType = if (isHighSpeed) {
            SessionConfiguration.SESSION_HIGH_SPEED
        } else {
            SessionConfiguration.SESSION_REGULAR
        }

        val sessionConfig = SessionConfiguration(
            sessionType,
            surfaces.map(::OutputConfiguration),
            handler::post,
            callback,
        )

        createCaptureSession(sessionConfig)
    }
}

private fun <T : Comparable<T>> T.toRange() = Range(this, this)
