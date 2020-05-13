package io.github.bgavyus.splash.flow

import android.graphics.Bitmap
import android.view.Surface
import io.github.bgavyus.splash.common.Streamer

interface FrameDuplicator : Streamer {
    val inputSurface: Surface
    val outputBitmap: Bitmap
}
