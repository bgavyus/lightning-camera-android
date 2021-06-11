package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.MediaMuxer

fun MediaMuxer.safeRelease() = runCatching { release() }
