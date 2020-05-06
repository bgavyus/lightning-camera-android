package io.github.bgavyus.splash.recording

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile
import java.nio.ByteBuffer
import java.util.*

class RetroMediaWriter(file: StorageFile, format: MediaFormat, rotation: Rotation) : AutoCloseable {
    companion object {
        private val TAG = RetroRecorder::class.simpleName

        private const val QUEUE_SIZE = 16
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
        closeStack.push(::stop)
    }

    private var recording = false
    private val buffers = ArrayDeque<MediaBuffer>(QUEUE_SIZE)

    fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!recording) {
            keep(buffer, info)
        } else {
            directWrite(buffer, info)
        }
    }

    private fun keep(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (buffers.size < QUEUE_SIZE) {
            buffers.addLast(copy(buffer, info))
        } else {
            val mediaBuffer = buffers.removeFirst()
            copy(buffer, info, mediaBuffer)
            buffers.addLast(mediaBuffer)
        }
    }

    private fun copy(buffer: ByteBuffer, info: MediaCodec.BufferInfo): MediaBuffer {
        val bufferCopy = ByteBuffer.allocateDirect(2 shl 18)
        val infoCopy = MediaCodec.BufferInfo()
        val mediaBuffer = MediaBuffer(bufferCopy, infoCopy)
        copy(buffer, info, mediaBuffer)
        return mediaBuffer
    }

    private fun copy(buffer: ByteBuffer, info: MediaCodec.BufferInfo, mediaBuffer: MediaBuffer) {
        copyBuffer(buffer, mediaBuffer.buffer)
        copyBufferInfo(info, mediaBuffer.info)
    }

    private fun copyBuffer(src: ByteBuffer, dst: ByteBuffer) {
        dst.position(src.position()).limit(src.limit())
        dst.put(src)
    }

    private fun copyBufferInfo(src: MediaCodec.BufferInfo, dst: MediaCodec.BufferInfo) {
        dst.set(src.offset, src.size, src.presentationTimeUs, src.flags)
    }

    fun stream() {
        flush()
        recording = true
    }

    private fun flush() {
        Log.d(TAG, "Flushing")

        while (true) {
            val mediaBuffer = buffers.pollFirst() ?: break
            directWrite(mediaBuffer.buffer, mediaBuffer.info)
        }

        Log.d(TAG, "Flushed")
    }

    private fun directWrite(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        muxer.writeSampleData(track, buffer, info)
    }

    fun hold() {
        recording = false
    }

    override fun close() {
        closeStack.close()
    }
}
