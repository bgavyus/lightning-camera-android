package io.github.bgavyus.splash.capture

import android.hardware.camera2.CameraAccessException

enum class CameraErrorType {
    HighSpeedNotAvailable,
    ConfigureFailed,
    Disconnected,
    InUse,
    MaxInUse,
    Disabled,
    Device,
    Generic;

    companion object {
        fun fromAccessException(error: CameraAccessException): CameraErrorType {
            return when (error.reason) {
                CameraAccessException.CAMERA_IN_USE -> InUse
                CameraAccessException.MAX_CAMERAS_IN_USE -> MaxInUse
                CameraAccessException.CAMERA_DISABLED -> Disabled
                CameraAccessException.CAMERA_ERROR -> Device
                else -> Generic
            }
        }
    }
}
