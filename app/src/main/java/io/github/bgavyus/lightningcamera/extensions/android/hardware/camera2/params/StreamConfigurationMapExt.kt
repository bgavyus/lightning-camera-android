package io.github.bgavyus.lightningcamera.extensions.android.hardware.camera2.params

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import kotlin.math.roundToInt

fun StreamConfigurationMap.getOutputMaxFps(format: Int, size: Size) =
    (1e9 / getOutputMinFrameDuration(format, size)).roundToInt()
