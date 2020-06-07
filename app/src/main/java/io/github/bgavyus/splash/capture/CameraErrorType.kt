package io.github.bgavyus.splash.capture

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice

enum class CameraErrorType {
    HighSpeedNotAvailable,
    ConfigureFailed,
    Disconnected,
    InUse,
    MaxInUse,
    Disabled,
    Device,
    Service,
    Unknown;

    companion object {
        fun fromAccessException(exception: CameraAccessException) = when (exception.reason) {
            CameraAccessException.CAMERA_IN_USE -> InUse
            CameraAccessException.MAX_CAMERAS_IN_USE -> MaxInUse
            CameraAccessException.CAMERA_DISABLED -> Disabled
            CameraAccessException.CAMERA_ERROR -> Device
            else -> Unknown
        }

        fun fromStateError(error: Int) = when (error) {
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> InUse
            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> MaxInUse
            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> Disabled
            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> Device
            CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> Service
            else -> Unknown
        }
    }
}
