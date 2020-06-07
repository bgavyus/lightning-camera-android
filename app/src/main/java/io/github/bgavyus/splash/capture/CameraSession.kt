package io.github.bgavyus.splash.capture

import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.graphics.ImageConsumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraSession private constructor(
    private val connection: CameraConnection,
    private val consumers: Iterable<ImageConsumer>
) : DeferScope() {
    companion object {
        suspend fun init(connection: CameraConnection, consumers: Iterable<ImageConsumer>) =
            CameraSession(connection, consumers).apply { init() }
    }

    private suspend fun init(): Unit = suspendCoroutine { continuation ->
        try {
            createCaptureSession(object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        startSession(session as CameraConstrainedHighSpeedCaptureSession)
                        defer(session::close)
                        continuation.resume(Unit)
                    } catch (error: CameraAccessException) {
                        val type = CameraErrorType.fromAccessException(error)
                        continuation.resumeWithException(CameraError(type))
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val type = CameraErrorType.ConfigureFailed
                    continuation.resumeWithException(CameraError(type))
                }
            })
        } catch (error: CameraAccessException) {
            val type = CameraErrorType.fromAccessException(error)
            continuation.resumeWithException(CameraError(type))
        }
    }

    private fun createCaptureSession(callback: CameraCaptureSession.StateCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val outputSurfaces = consumers.map { it.surface }

            @Suppress("DEPRECATION")
            connection.device.createConstrainedHighSpeedCaptureSession(
                outputSurfaces,
                callback,
                /* handler = */ null
            )
        } else {
            val outputConfigs = consumers.map { OutputConfiguration(it.surface) }

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_HIGH_SPEED,
                outputConfigs,
                { Handler().post(it) },
                callback
            )

            connection.device.createCaptureSession(sessionConfig)
        }
    }

    private fun startSession(session: CameraConstrainedHighSpeedCaptureSession) {
        session.run {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, connection.camera.fpsRange)
                consumers.forEach { addTarget(it.surface) }
            }

            val requests = createHighSpeedRequestList(builder.build())
            setRepeatingBurst(requests, /* listener = */ null, /* handler = */ null)
        }
    }
}
