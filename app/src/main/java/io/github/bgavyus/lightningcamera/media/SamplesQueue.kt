package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.util.Size
import com.google.common.collect.EvictingQueue
import com.google.common.collect.Queues
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import java.nio.ByteBuffer
import java.util.Queue
import kotlin.math.ceil

class SamplesQueue(frameSize: Size, frameRate: FrameRate) : SamplesProcessor {
    private val pool: Iterator<Sample>
    private val queue: Queue<Sample>

    init {
        val samplesCount = ceil(frameRate.fps * minBufferSeconds).toInt()
        pool = SamplesPool(samplesCount, frameSize.area)

        @Suppress("UnstableApiUsage")
        queue = Queues.synchronizedQueue(EvictingQueue.create(samplesCount))
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

    fun clear() {
        queue.clear()
    }

    companion object {
        private const val minBufferSeconds = 0.05
    }
}
