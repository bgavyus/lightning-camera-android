package io.github.bgavyus.splash.graphics.media

import io.github.bgavyus.splash.graphics.ImageConsumer

interface Recorder : ImageConsumer {
    fun record()
    fun loss()
}
