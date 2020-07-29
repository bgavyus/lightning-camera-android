package io.github.bgavyus.splash.capture

import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.graphics.ImageConsumer
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraSession(
    private val cameraMetadata: CameraMetadata,
    private val connection: CameraConnection,
    private val consumers: Iterable<ImageConsumer>
) : DeferScope() {
    companion object {
        private val TAG = CameraSession::class.simpleName
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    suspend fun open(): Unit = suspendCoroutine { continuation ->
        try {
            createCaptureSession(object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        startSession(session as CameraConstrainedHighSpeedCaptureSession)
                        defer(session::close)
                        continuation.resume(Unit)
                    } catch (error: CameraAccessException) {
                        continuation.resumeWithException(CameraError.fromAccessException(error))
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val type = CameraErrorType.ConfigureFailed
                    continuation.resumeWithException(CameraError(type))
                }
            })
        } catch (error: CameraAccessException) {
            continuation.resumeWithException(CameraError.fromAccessException(error))
        }
    }

    private fun createCaptureSession(callback: CameraCaptureSession.StateCallback) {
        val outputSurfaces = consumers.map { it.surface }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            connection.device.createConstrainedHighSpeedCaptureSession(
                outputSurfaces,
                callback,
                handler
            )
        } else {
            val outputConfigs = outputSurfaces.map { OutputConfiguration(it) }
            val executor = Executor { handler.post(it) }

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_HIGH_SPEED,
                outputConfigs,
                executor,
                callback
            )

            connection.device.createCaptureSession(sessionConfig)
        }
    }

    private fun startSession(session: CameraConstrainedHighSpeedCaptureSession) {
        session.run {
            val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, cameraMetadata.fpsRange)
                consumers.forEach { addTarget(it.surface) }
            }.build()

            val requests = createHighSpeedRequestList(captureRequest)
            setRepeatingBurst(requests, /* listener = */ null, handler)
        }
    }
}
