package io.github.bgavyus.splash.recording

import android.media.MediaCodec
import java.nio.ByteBuffer

data class MediaBuffer(var buffer: ByteBuffer, var info: MediaCodec.BufferInfo)
