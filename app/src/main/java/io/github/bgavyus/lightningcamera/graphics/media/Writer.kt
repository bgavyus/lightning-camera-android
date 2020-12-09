package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.storage.StorageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class Writer(
    private val file: StorageFile,
    format: MediaFormat,
    rotation: Rotation
) : DeferScope() {
    companion object {
        private const val OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val track: Int
    private var active = false

    private val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MediaMuxer(file.descriptor, OUTPUT_FORMAT)
    } else {
        MediaMuxer(file.path, OUTPUT_FORMAT)
    }.apply {
        defer(::release)
        setOrientationHint(rotation.degrees)
        track = addTrack(format)
        start()
    }

    fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!active) {
            scope.launch { file.keep() }
            active = true
        }

        muxer.writeSampleData(track, buffer, info)
    }
}
