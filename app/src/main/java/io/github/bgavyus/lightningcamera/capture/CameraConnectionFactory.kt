package io.github.bgavyus.lightningcamera.capture

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.extensions.openCamera
import io.github.bgavyus.lightningcamera.extensions.systemService
import kotlinx.coroutines.Dispatchers
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
