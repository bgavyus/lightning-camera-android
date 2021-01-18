package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.MediaCodec

fun MediaCodec.BufferInfo.copyFrom(other: MediaCodec.BufferInfo) =
    set(other.offset, other.size, other.presentationTimeUs, other.flags)
