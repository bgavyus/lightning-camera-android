package io.github.bgavyus.splash.capture

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CameraConnection(
    private val context: Context,
    private val cameraMetadata: CameraMetadata
) : DeferScope() {
    companion object {
        private val TAG = CameraConnection::class.simpleName
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    lateinit var device: CameraDevice

    suspend fun open() = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(CameraManager::class.java)
            ?: throw RuntimeException("Failed to get camera manager service")

        // TODO: Propagate errors
        device = manager.openCamera(cameraMetadata.id, handler)
            .first()
            .apply { defer(::close) }
    }
}

@SuppressLint("MissingPermission")
private fun CameraManager.openCamera(id: String, handler: Handler): Flow<CameraDevice> =
    callbackFlow {
        val callback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) = sendBlocking(camera)

            override fun onDisconnected(camera: CameraDevice) =
                cancel(CameraError(CameraErrorType.Disconnected))

            override fun onError(camera: CameraDevice, error: Int) =
                cancel(CameraError.fromStateError(error))

            private fun cancel(error: CameraError) =
                cancel(CancellationException(error.type.name, error))
        }

        try {
            openCamera(id, callback, handler)
        } catch (error: CameraAccessException) {
            throw CameraError.fromAccessException(error)
        }

        awaitClose()
    }
