package io.github.bgavyus.splash.recording

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile
import java.nio.ByteBuffer

class RetroMediaWriter(file: StorageFile, format: MediaFormat, rotation: Rotation) : AutoCloseable {
    companion object {
        private const val OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private val closeStack = CloseStack()
    private val track: Int
    private val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MediaMuxer(file.descriptor, OUTPUT_FORMAT)
    } else {
        MediaMuxer(file.path, OUTPUT_FORMAT)
    }.apply {
        setOrientationHint(rotation.degrees)
        closeStack.push(::release)
        track = addTrack(format)
        start()
    }

    private var recording = false

    fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (recording) {
            muxer.writeSampleData(track, buffer, info)
        }
    }
    
    fun stream() {
        recording = true
    }
    
    fun hold() {
        recording = false
    }

    override fun close() {
        closeStack.close()
    }
}
