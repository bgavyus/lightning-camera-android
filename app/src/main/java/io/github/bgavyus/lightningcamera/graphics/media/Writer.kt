package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class Writer(storage: Storage, format: MediaFormat, rotation: Rotation) : DeferScope() {
    companion object {
        private const val outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val file = storage.generateFile()
        .also { defer(it::close) }

    private val track: Int
    private val active = AtomicBoolean()

    private val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MediaMuxer(file.descriptor, outputFormat)
    } else {
        MediaMuxer(file.path, outputFormat)
    }.apply {
        defer(::release)
        setOrientationHint(rotation.degrees)
        track = addTrack(format)
        start()
    }

    fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (active.compareAndSet(false, true)) {
            scope.launch { file.keep() }
        }

        try {
            muxer.writeSampleData(track, buffer, info)
        } catch (exception: IllegalStateException) {
            Logger.error("Write failed", exception)
        }
    }
}
