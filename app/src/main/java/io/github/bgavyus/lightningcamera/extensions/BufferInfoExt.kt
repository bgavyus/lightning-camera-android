package io.github.bgavyus.lightningcamera.extensions

import android.media.MediaCodec

fun MediaCodec.BufferInfo.copyFrom(other: MediaCodec.BufferInfo) =
    set(other.offset, other.size, other.presentationTimeUs, other.flags)
