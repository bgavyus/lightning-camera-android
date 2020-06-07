package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile
import java.nio.ByteBuffer

class Writer(private val file: StorageFile, format: MediaFormat, rotation: Rotation) :
    DeferScope() {
    companion object {
        private val TAG = Writer::class.simpleName

        private const val OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    }

    private val track: Int

    private val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MediaMuxer(file.descriptor, OUTPUT_FORMAT)
    } else {
        MediaMuxer(file.path, OUTPUT_FORMAT)
    }.apply {
        defer {
            Log.d(TAG, "Attempting to release muxer")

            if (file.valid) {
                Log.d(TAG, "Releasing muxer")
                release()
            }
        }

        setOrientationHint(rotation.degrees)
        track = addTrack(format)
        start()

        defer {
            Log.d(TAG, "Attempting to stop muxer")

            if (file.valid) {
                Log.d(TAG, "Stopping muxer")
                stop()
            }
        }
    }

    fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        muxer.writeSampleData(track, buffer, info)
        file.valid = true
    }
}
