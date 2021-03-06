package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class SamplesPool(samplesCount: Int, sampleSize: Int): Iterator<Sample> {
    private var index = 0

    private val array = Array(samplesCount) {
        Sample(ByteBuffer.allocateDirect(sampleSize), MediaCodec.BufferInfo())
    }

    override fun next(): Sample {
        val sample = array[index]
        index = (index + 1) % array.size
        return sample
    }

    override fun hasNext() = true
}
