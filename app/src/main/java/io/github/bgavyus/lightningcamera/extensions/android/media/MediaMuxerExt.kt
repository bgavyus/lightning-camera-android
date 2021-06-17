package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.MediaMuxer

fun MediaMuxer.tryRelease() = runCatching { release() }
