package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.FrameRate

object FormatFactory {
    fun create(
        size: Size,
        frameRate: FrameRate,
        mimeType: String,
    ) = MediaFormat.createVideoFormat(mimeType, size.width, size.height).apply {
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .first { it.supportedTypes.contains(mimeType) }

        setInteger(
            MediaFormat.KEY_BIT_RATE,
            codecInfo.getCapabilitiesForType(mimeType).videoCapabilities.bitrateRange.upper
                .also { Logger.log("Bit rate: $it") }
        )

        setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
    }
}
