package io.github.bgavyus.lightningcamera.capture

import android.Manifest
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
import io.github.bgavyus.lightningcamera.permissions.PermissionMissingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CameraConnectionFactory(private val context: Context) : DeferScope() {
    companion object {
        val permissions = listOf(Manifest.permission.CAMERA)
    }

    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    suspend fun open(cameraId: String) = withContext(Dispatchers.IO) {
        try {
            context.systemService<CameraManager>().openCamera(cameraId, handler)
                .first()
        } catch (exception: CameraAccessException) {
            throw CameraException.fromAccessException(exception)
        } catch (_: SecurityException) {
            throw PermissionMissingException()
        }
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

    openCamera(id, callback, handler)
    awaitClose()
}
