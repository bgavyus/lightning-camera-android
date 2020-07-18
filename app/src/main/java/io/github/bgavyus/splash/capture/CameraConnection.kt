package io.github.bgavyus.splash.capture

import android.annotation.SuppressLint
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

class CameraConnection private constructor(val camera: Camera) : DeferScope() {
    companion object {
        private val TAG = CameraConnection::class.simpleName

        suspend fun init(camera: Camera) = CameraConnection(camera).apply { init() }
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    lateinit var device: CameraDevice

    private suspend fun init() {
        // TODO: Propagate errors
        device = Camera.manager.openCamera(camera.id, handler)
            .first()
            .apply { defer(::close) }
    }
}

@SuppressLint("MissingPermission")
fun CameraManager.openCamera(id: String, handler: Handler): Flow<CameraDevice> = callbackFlow {
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
