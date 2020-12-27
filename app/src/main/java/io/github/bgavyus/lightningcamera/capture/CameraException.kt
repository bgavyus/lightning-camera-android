package io.github.bgavyus.lightningcamera.capture

import android.hardware.camera2.CameraAccessException

class CameraException(val type: CameraExceptionType) : Exception() {
    companion object {
        fun fromAccessException(exception: CameraAccessException) =
            CameraException(CameraExceptionType.fromAccessException(exception))

        fun fromStateError(error: Int) =
            CameraException(CameraExceptionType.fromStateError(error))
    }
}
