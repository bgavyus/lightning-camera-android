package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.util.Size
import com.google.common.collect.EvictingQueue
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import io.github.bgavyus.lightningcamera.utilities.Hertz
import java.nio.ByteBuffer
import kotlin.math.ceil

@Suppress("UnstableApiUsage")
class SamplesQueue(frameSize: Size, frameRate: Hertz) : SamplesProcessor {
    companion object {
        private const val minBufferSeconds = 0.05
    }

    private var index = 0
    private val samplesPool: Array<Sample>
    private val queue: EvictingQueue<Sample>

    init {
        val samplesCount = ceil(frameRate.value * minBufferSeconds).toInt()

        samplesPool = Array(samplesCount) {
            Sample(ByteBuffer.allocateDirect(frameSize.area), MediaCodec.BufferInfo())
        }

        queue = EvictingQueue.create(samplesCount)
    }

    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val sample = samplesPool[index].also {
            it.buffer.copyFrom(buffer)
            it.info.copyFrom(info)
        }

        queue.offer(sample)
        index = (index + 1) % samplesPool.size
    }

    fun drain(processor: SamplesProcessor) {
        while (true) {
            val sample = queue.poll() ?: break
            processor.process(sample.buffer, sample.info)
        }

        index = 0
    }

    fun clear() {
        queue.clear()
        index = 0
    }
}
