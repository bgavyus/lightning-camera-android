package io.github.bgavyus.lightningcamera.extensions.android.graphics

import android.graphics.PixelFormat

fun PixelFormat.load(format: Int) = PixelFormat.getPixelFormatInfo(format, this)
