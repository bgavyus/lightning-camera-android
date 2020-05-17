package io.github.bgavyus.splash.sensors

import android.view.Surface

interface CameraListener {
    fun onSurfacesNeeded(): List<Surface>
    fun onCameraError(type: CameraErrorType)
}
