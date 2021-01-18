package io.github.bgavyus.lightningcamera.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range

val CameraCharacteristics.streamConfigurationMap: StreamConfigurationMap
    get() = requireGet(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

val CameraCharacteristics.fpsRanges: Array<Range<Int>>
    get() = requireGet(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

val CameraCharacteristics.sensorOrientation: Int
    get() = requireGet(CameraCharacteristics.SENSOR_ORIENTATION)

fun <T> CameraCharacteristics.requireGet(key: CameraCharacteristics.Key<T>) =
    get(key) ?: throw RuntimeException()
