package io.github.bgavyus.splash.capture

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice

class CameraError(val type: CameraErrorType) : Exception() {
    companion object {
        fun fromAccessException(exception: CameraAccessException) = CameraError(
            when (exception.reason) {
                CameraAccessException.CAMERA_IN_USE -> CameraErrorType.InUse
                CameraAccessException.MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
                CameraAccessException.CAMERA_DISABLED -> CameraErrorType.Disabled
                CameraAccessException.CAMERA_ERROR -> CameraErrorType.Device
                else -> CameraErrorType.Unknown
            }
        )

        fun fromStateError(error: Int) = CameraError(
            when (error) {
                CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> CameraErrorType.InUse
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> CameraErrorType.Disabled
                CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> CameraErrorType.Device
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> CameraErrorType.Service
                else -> CameraErrorType.Unknown
            }
        )
    }
}
