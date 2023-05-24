package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.Image
import java.nio.ByteBuffer

val Image.firstPlainBuffer: ByteBuffer get() = planes[0].buffer
