package io.github.bgavyus.splash.camera

import android.view.Surface

interface CameraListener {
    fun onSurfacesNeeded(): List<Surface>
    fun onCameraStreamStarted()
    fun onCameraError(type: CameraErrorType)
}
