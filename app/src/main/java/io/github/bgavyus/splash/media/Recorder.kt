package io.github.bgavyus.splash.media

import io.github.bgavyus.splash.common.ImageConsumer

interface Recorder : ImageConsumer, AutoCloseable {
    fun record()
    fun loss()
}
