package io.github.bgavyus.splash.flow

interface FrameDuplicatorListener {
    fun onFrameDuplicatorAvailable(frameDuplicator: FrameDuplicator)
    fun onFrameAvailable()
}
