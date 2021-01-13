package io.github.bgavyus.lightningcamera.common.extensions

import android.graphics.SurfaceTexture
import android.util.Size

fun SurfaceTexture.setDefaultBufferSize(size: Size) = setDefaultBufferSize(size.width, size.height)
