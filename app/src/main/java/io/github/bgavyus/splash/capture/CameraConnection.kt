package io.github.bgavyus.splash.capture

import android.annotation.SuppressLint
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.Deferrer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraConnection private constructor(val camera: Camera) : Deferrer() {
    companion object {
        suspend fun init(camera: Camera) = CameraConnection(camera).apply { init() }
    }

    lateinit var device: CameraDevice

    @SuppressLint("MissingPermission")
    private suspend fun init(): Unit = suspendCoroutine { continuation ->
        val cameraManager = App.context.getSystemService(CameraManager::class.java)
            ?: throw CameraError(CameraErrorType.Generic)

        try {
            cameraManager.openCamera(camera.id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    this@CameraConnection.device = device
                        .apply { defer(::close) }

                    continuation.resume(Unit)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    val type = CameraErrorType.Disconnected
                    continuation.resumeWithException(CameraError(type))
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val type = CameraErrorType.fromStateError(error)
                    continuation.resumeWithException(CameraError(type))
                }
            }, /* handler = */ null)
        } catch (error: CameraAccessException) {
            throw CameraError(CameraErrorType.fromAccessException(error))
        }
    }
}
