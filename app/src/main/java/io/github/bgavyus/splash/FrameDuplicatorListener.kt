package io.github.bgavyus.splash

interface FrameDuplicatorListener {
    fun onFrameDuplicatorAvailable(frameDuplicator: FrameDuplicator)
    fun onFrameAvailable()
}
