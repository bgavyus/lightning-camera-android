package io.github.bgavyus.lightningcamera.capture

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.common.extensions.cancel
import io.github.bgavyus.lightningcamera.common.extensions.systemService
import io.github.bgavyus.lightningcamera.permissions.PermissionGroup
import io.github.bgavyus.lightningcamera.permissions.PermissionMissingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CameraConnectionFactory(private val context: Context) : DeferScope() {
    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    suspend fun open(cameraId: String) = withContext(Dispatchers.IO) {
        context.systemService<CameraManager>().openCamera(cameraId, handler)
            .first()
    }
}

@SuppressLint("MissingPermission")
private fun CameraManager.openCamera(id: String, handler: Handler) = callbackFlow<CameraDevice> {
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) = sendBlocking(camera)

        override fun onDisconnected(camera: CameraDevice) =
            cancel(CameraException(CameraExceptionType.Disconnected))

        override fun onError(camera: CameraDevice, error: Int) =
            cancel(CameraException.fromStateError(error))
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
