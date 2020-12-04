package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec

val MediaCodec.BufferInfo.empty get() = size == 0
val MediaCodec.BufferInfo.keyFrame get() = flags.isSet(MediaCodec.BUFFER_FLAG_KEY_FRAME)
val MediaCodec.BufferInfo.codecConfig get() = flags.isSet(MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
val MediaCodec.BufferInfo.endOfStream get() = flags.isSet(MediaCodec.BUFFER_FLAG_END_OF_STREAM)

private fun Int.isSet(flag: Int) = and(flag) != 0
