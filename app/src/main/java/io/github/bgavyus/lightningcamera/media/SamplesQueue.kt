package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.util.Size
import com.google.common.collect.EvictingQueue
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import java.nio.ByteBuffer
import kotlin.math.ceil

@Suppress("UnstableApiUsage")
class SamplesQueue(frameSize: Size, frameRate: FrameRate) : SamplesProcessor {
    companion object {
        private const val minBufferSeconds = 0.05
    }

    private val pool: SamplesPool
    private val queue: EvictingQueue<Sample>

    init {
        val samplesCount = ceil(frameRate.hertz * minBufferSeconds).toInt()
        pool = SamplesPool(samplesCount, frameSize.area)
        queue = EvictingQueue.create(samplesCount)
    }

    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val sample = pool.next().also {
            it.buffer.copyFrom(buffer)
            it.info.copyFrom(info)
        }

        queue.add(sample)
    }

    fun drain(processor: SamplesProcessor) {
        while (true) {
            val sample = queue.poll() ?: break
            processor.process(sample.buffer, sample.info)
        }
    }

    fun clear() = queue.clear()
}
