package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class PresentationTimeNormalizer : SamplesProcessor {
    companion object {
        private const val microsInUnit = 1_000_000
        private const val playbackFps = 5
    }

    private val generator = generateSequence(0L) { it + microsInUnit / playbackFps }.iterator()

    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        info.presentationTimeUs = generator.next()
    }
}
