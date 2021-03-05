package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer

data class Sample(val buffer: ByteBuffer, val info: MediaCodec.BufferInfo)
