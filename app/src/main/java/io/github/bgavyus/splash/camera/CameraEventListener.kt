package io.github.bgavyus.splash.camera

import android.view.Surface

interface CameraEventListener {
	fun onCameraSurfacesNeeded(): List<Surface>
	fun onCameraStreaming()
	fun onCameraError(type: CameraErrorType)
}
