package io.github.bgavyus.splash.camera

import android.view.Surface

interface CameraListener {
    fun onSurfacesNeeded(): List<Surface>
    fun onStreaming()
    fun onCameraError(type: CameraErrorType)
}
