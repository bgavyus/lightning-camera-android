package io.github.bgavyus.lightningcamera.extensions.android.util

import android.graphics.Point
import android.provider.MediaStore
import android.util.Rational
import android.util.Size

val Size.area get() = width * height
val Size.aspectRatio get() = Rational(width, height)
val Size.center get() = Point(width / 2, height / 2)

@Suppress("DEPRECATION")
val Size.videoThumbnailKind
    get() = when {
        width <= 96 && height <= 96 -> MediaStore.Video.Thumbnails.MICRO_KIND
        width <= 512 && height <= 384 -> MediaStore.Video.Thumbnails.MINI_KIND
        width <= 1024 && height <= 786 -> MediaStore.Video.Thumbnails.FULL_SCREEN_KIND
        else -> MediaStore.Video.Thumbnails.FULL_SCREEN_KIND
    }
