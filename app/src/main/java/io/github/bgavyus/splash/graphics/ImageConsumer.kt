package io.github.bgavyus.splash.graphics

import android.view.Surface

interface ImageConsumer : AutoCloseable {
    val surface: Surface
}
