package io.github.bgavyus.splash.recording

import android.view.Surface

interface Recorder : AutoCloseable {
    val inputSurface: Surface
    fun record()
    fun loss()
}
