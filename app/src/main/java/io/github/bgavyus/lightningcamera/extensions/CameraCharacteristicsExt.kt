package io.github.bgavyus.lightningcamera.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import io.github.bgavyus.lightningcamera.common.Rotation

val CameraCharacteristics.supportsHighSpeed
    get() = CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in requestAvailableCapabilities

val CameraCharacteristics.requestAvailableCapabilities: IntArray
    get() = requireGet(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val CameraCharacteristics.streamConfigurationMap: StreamConfigurationMap
    get() = requireGet(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

val CameraCharacteristics.sensorOrientation
    get() = Rotation.fromDegrees(requireGet(CameraCharacteristics.SENSOR_ORIENTATION))

val CameraCharacteristics.fpsRanges: Array<Range<Int>>
    get() = requireGet(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

fun <T> CameraCharacteristics.requireGet(key: CameraCharacteristics.Key<T>) =
    get(key) ?: throw RuntimeException()
