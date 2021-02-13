package io.github.bgavyus.lightningcamera.capture

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.openCamera
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CameraConnectionFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val handler: Handler,
) {
    companion object {
        val permissions = listOf(Manifest.permission.CAMERA)
    }

    suspend fun open(cameraId: String) =
        context.systemService<CameraManager>().openCamera(cameraId, handler).first()
}
