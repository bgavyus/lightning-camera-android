package io.github.bgavyus.splash.capture

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import androidx.annotation.StringRes
import io.github.bgavyus.splash.common.ResourceCapable

class CameraException(type: CameraExceptionType) : Exception(), ResourceCapable {
    companion object {
        fun fromAccessException(exception: CameraAccessException) = CameraException(
            when (exception.reason) {
                CameraAccessException.CAMERA_IN_USE -> CameraExceptionType.InUse
                CameraAccessException.MAX_CAMERAS_IN_USE -> CameraExceptionType.MaxInUse
                CameraAccessException.CAMERA_DISABLED -> CameraExceptionType.Disabled
                CameraAccessException.CAMERA_ERROR -> CameraExceptionType.Device
                else -> CameraExceptionType.Unknown
            }
        )

        fun fromStateError(error: Int) = CameraException(
            when (error) {
                CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> CameraExceptionType.InUse
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> CameraExceptionType.MaxInUse
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> CameraExceptionType.Disabled
                CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> CameraExceptionType.Device
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> CameraExceptionType.Service
                else -> CameraExceptionType.Unknown
            }
        )
    }

    @StringRes override val resourceId = type.resourceId
}
