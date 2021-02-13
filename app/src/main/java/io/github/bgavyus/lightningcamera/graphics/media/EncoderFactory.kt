package io.github.bgavyus.lightningcamera.graphics.media

import android.os.Handler
import android.util.Size
import io.github.bgavyus.lightningcamera.common.Hertz
import javax.inject.Inject

class EncoderFactory @Inject constructor(
    private val handler: Handler,
) {
    fun create(size: Size, frameRate: Hertz) = Encoder(handler, size, frameRate)
}
