package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.storage.Storage

class NormalizedWriter(
    storage: Storage,
    format: MediaFormat,
    orientation: Degrees,
) : Writer(storage, format, orientation) {
    companion object {
        private const val microsInUnit = 1_000_000
        private const val playbackFps = 5
    }

    private val presentationTimeGenerator =
        generateSequence(0L) { it + microsInUnit / playbackFps }.iterator()

    override fun write(sample: Sample) {
        normalize(sample.info)
        super.write(sample)
    }

    private fun normalize(info: MediaCodec.BufferInfo) {
        info.presentationTimeUs = presentationTimeGenerator.next()
    }
}
