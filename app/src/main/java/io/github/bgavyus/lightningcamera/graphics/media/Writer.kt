package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.storage.Storage
import java.util.concurrent.atomic.AtomicBoolean

open class Writer(storage: Storage, format: MediaFormat, orientation: Degrees) : DeferScope() {
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
        defer(::release)
        setOrientationHint(orientation.normalized.value)
        track = addTrack(format)
        start()
    }

    private val active = AtomicBoolean()

    open fun write(sample: Sample) {
        if (active.compareAndSet(false, true)) {
            file.keep()
        }

        muxer.writeSampleData(track, sample.buffer, sample.info)
    }
}
