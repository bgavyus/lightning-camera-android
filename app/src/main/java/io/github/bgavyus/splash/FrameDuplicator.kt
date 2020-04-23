package io.github.bgavyus.splash

import android.graphics.Bitmap
import android.view.Surface

interface FrameDuplicator {
    val inputSurface: Surface
    val outputBitmap: Bitmap

    fun propagate()
    fun startStreaming()
    fun stopStreaming()
}
