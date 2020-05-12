package io.github.bgavyus.splash.media

import android.view.Surface

interface Recorder : AutoCloseable {
    val inputSurface: Surface
    fun record()
    fun loss()
}
