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
        context.systemService<CameraManager>().openCamera(cameraId, handler).first()
    }
}

@SuppressLint("MissingPermission")
private fun CameraManager.openCamera(id: String, handler: Handler) = callbackFlow<CameraDevice> {
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) = sendBlocking(camera)

        override fun onDisconnected(camera: CameraDevice) =
            cancel(CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED))

        override fun onError(camera: CameraDevice, error: Int) =
            cancel(CameraAccessException(errorCodeToAccessError(error), "Error code: $error"))

        private fun errorCodeToAccessError(error: Int) = when (error) {
            ERROR_CAMERA_IN_USE -> CameraAccessException.CAMERA_IN_USE
            ERROR_MAX_CAMERAS_IN_USE -> CameraAccessException.MAX_CAMERAS_IN_USE
            ERROR_CAMERA_DISABLED -> CameraAccessException.CAMERA_DISABLED
            else -> CameraAccessException.CAMERA_ERROR
        }
    }

    openCamera(id, callback, handler)
    awaitClose()
}
