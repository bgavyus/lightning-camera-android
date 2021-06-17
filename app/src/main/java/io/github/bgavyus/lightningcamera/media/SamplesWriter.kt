package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.media.tryRelease
import io.github.bgavyus.lightningcamera.storage.Storage
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Rotation
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

@AutoFactory
class SamplesWriter(
    @Provided storage: Storage,
    format: MediaFormat,
    orientation: Rotation,
) : DeferScope(), SamplesProcessor {
    companion object {
        private const val outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private val file = storage.generateFile()
        .also { defer(it::close) }

    private val track: Int

    private val muxer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        MediaMuxer(file.path, outputFormat)
    } else {
        MediaMuxer(file.descriptor, outputFormat)
    }.apply {
        defer(::tryRelease)
        setOrientationHint(orientation.degrees)
        track = addTrack(format)
        start()
    }

    private val active = AtomicBoolean()

    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (active.compareAndSet(false, true)) {
            file.keep()
        }

        muxer.writeSampleData(track, buffer, info)
    }
}
