package io.github.bgavyus.splash.capture

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.common.extensions.systemService
import io.github.bgavyus.splash.permissions.PermissionMissingException
import io.github.bgavyus.splash.permissions.PermissionGroup
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
    private val cameraId: String
) : DeferScope() {
    companion object {
        private val TAG = CameraConnection::class.simpleName
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    lateinit var device: CameraDevice

    suspend fun open() = withContext(Dispatchers.IO) {
        device = context.systemService(CameraManager::class).openCamera(cameraId, handler)
            .first()
            .apply { defer(::close) }
    }
}

@SuppressLint("MissingPermission")
private fun CameraManager.openCamera(
    id: String,
    handler: Handler
): Flow<CameraDevice> = callbackFlow {
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) = sendBlocking(camera)

        override fun onDisconnected(camera: CameraDevice) =
            cancel(CameraException(CameraExceptionType.Disconnected))

        override fun onError(camera: CameraDevice, error: Int) =
            cancel(CameraException.fromStateError(error))

        private fun cancel(exception: CameraException) =
            cancel(CancellationException(CameraException::class.java.simpleName, exception))
    }

    try {
        openCamera(id, callback, handler)
    } catch (exception: CameraAccessException) {
        throw CameraException.fromAccessException(exception)
    } catch (_: SecurityException) {
        throw PermissionMissingException(PermissionGroup.Camera)
    }

    awaitClose()
}
