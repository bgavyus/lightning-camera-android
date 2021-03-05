package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class SamplesPipeline(private val stages: Iterable<SamplesProcessor>) : SamplesProcessor {
    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        stages.forEach { it.process(buffer, info) }
    }
}
