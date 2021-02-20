package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.Hertz

object FormatFactory {
    fun create(
        size: Size,
        frameRate: Hertz,
        mimeType: String,
    ) = MediaFormat.createVideoFormat(mimeType, size.width, size.height).apply {
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .find { it.supportedTypes.contains(mimeType) }
            ?: throw RuntimeException()

        setInteger(
            MediaFormat.KEY_BIT_RATE,
            codecInfo.getCapabilitiesForType(mimeType).videoCapabilities.bitrateRange.upper
                .also { Logger.log("Bit rate: $it") }
        )

        setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.value)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
    }
}
