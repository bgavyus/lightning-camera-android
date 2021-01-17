package io.github.bgavyus.lightningcamera.extensions

import android.media.MediaMuxer
import io.github.bgavyus.lightningcamera.graphics.media.Sample

fun MediaMuxer.writeSampleData(track: Int, sample: Sample) =
    writeSampleData(track, sample.buffer, sample.info)
