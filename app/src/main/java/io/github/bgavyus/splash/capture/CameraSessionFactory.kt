package io.github.bgavyus.splash.capture

import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Range
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraSessionFactory : DeferScope() {
    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    suspend fun create(device: CameraDevice, surfaces: List<Surface>, framesPerSecond: Int) =
        device.createConstrainedHighSpeedCaptureSession(surfaces, handler).apply {
            val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, framesPerSecond.toRange())
                surfaces.forEach { addTarget(it) }
            }.build()

            setRepeatingBurst(createHighSpeedRequestList(captureRequest), null, handler)
        }
}

private suspend fun CameraDevice.createConstrainedHighSpeedCaptureSession(
    surfaces: List<Surface>,
    handler: Handler
): CameraConstrainedHighSpeedCaptureSession = suspendCoroutine { continuation ->
    val callback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            continuation.resume(session as CameraConstrainedHighSpeedCaptureSession)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            continuation.resumeWithException(CameraException(CameraExceptionType.ConfigureFailed))
        }
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_HIGH_SPEED,
                surfaces.map { OutputConfiguration(it) },
                handler::post,
                callback
            )

            createCaptureSession(sessionConfig)
        } else {
            @Suppress("DEPRECATION")
            createConstrainedHighSpeedCaptureSession(
                surfaces,
                callback,
                handler
            )
        }
    } catch (error: CameraAccessException) {
        throw CameraException.fromAccessException(error)
    }
}

private fun <T : Comparable<T>> T.toRange() = Range(this, this)
