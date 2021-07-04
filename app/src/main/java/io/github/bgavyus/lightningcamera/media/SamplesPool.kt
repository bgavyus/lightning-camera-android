package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

class SamplesPool(samplesCount: Int, sampleSize: Int) : Iterator<Sample> {
    private val index = AtomicInteger()

    private val array = Array(samplesCount) {
        Sample(ByteBuffer.allocateDirect(sampleSize), MediaCodec.BufferInfo())
    }

    override fun next() = array[index.getAndUpdate { (it + 1) % array.size }]
    override fun hasNext() = true
}
