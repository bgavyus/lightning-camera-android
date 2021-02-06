package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.MediaCodec
import android.media.MediaCrypto
import android.media.MediaFormat
import android.os.Handler
import io.github.bgavyus.lightningcamera.common.OptionSet
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun MediaCodec.encoderEvents(handler: Handler? = null) = callbackFlow<EncoderEvent> {
    val callback = object : MediaCodec.Callback() {
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) =
            sendBlocking(EncoderEvent.FormatChanged(format))

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
        ) = sendBlocking(EncoderEvent.BufferAvailable(index, info))

        override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) = cancel(error)
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
    }

    setCallback(callback, handler)
    awaitClose { setCallback(null) }
}

sealed class EncoderEvent {
    data class FormatChanged(val format: MediaFormat) : EncoderEvent()
    data class BufferAvailable(val index: Int, val info: MediaCodec.BufferInfo) : EncoderEvent()
}

fun MediaCodec.configureEncoder(format: MediaFormat? = null, crypto: MediaCrypto? = null) =
    configure(format, null, crypto, MediaCodec.CONFIGURE_FLAG_ENCODE)

val MediaCodec.BufferInfo.flagSet get() = OptionSet(flags)

fun MediaCodec.BufferInfo.copyFrom(other: MediaCodec.BufferInfo) =
    set(other.offset, other.size, other.presentationTimeUs, other.flags)
