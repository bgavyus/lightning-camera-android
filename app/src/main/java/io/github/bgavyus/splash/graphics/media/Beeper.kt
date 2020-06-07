package io.github.bgavyus.splash.graphics.media

import android.media.AudioManager
import android.media.ToneGenerator
import io.github.bgavyus.splash.common.DeferScope

class Beeper : DeferScope() {
    private val generator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        .apply { defer(::release) }

    fun start() = generator.startTone(ToneGenerator.TONE_DTMF_0)
    fun stop() = generator.stopTone()
}
