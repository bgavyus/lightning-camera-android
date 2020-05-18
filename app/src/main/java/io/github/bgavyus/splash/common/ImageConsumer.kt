package io.github.bgavyus.splash.common

import android.view.Surface

interface ImageConsumer : AutoCloseable {
    val surface: Surface
}
