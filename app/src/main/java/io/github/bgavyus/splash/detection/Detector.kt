package io.github.bgavyus.splash.detection

interface Detector {
    fun detected(): Boolean
    fun release()
}
