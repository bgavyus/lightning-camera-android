package io.github.bgavyus.lightningcamera.hardware.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import io.github.bgavyus.lightningcamera.extensions.android.content.requireSystemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.openCamera
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CameraConnectionFactory @Inject constructor(
    private val context: Context,
    private val handler: Handler,
) {
    suspend fun open(cameraId: String) = context.requireSystemService<CameraManager>()
        .openCamera(cameraId, handler)
        .first()

    companion object {
        val permissions = listOf(Manifest.permission.CAMERA)
    }
}
