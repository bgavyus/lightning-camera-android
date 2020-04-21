package io.github.bgavyus.splash.recording

import android.media.MediaRecorder

open class StatefulMediaRecorder : MediaRecorder() {
    private var state = RecorderState.Initial
    private var onErrorListener: OnErrorListener? = null

    init {
        setOnErrorListener { mr, what, extra ->
            state = RecorderState.Error
            onErrorListener?.onError(mr, what, extra)
        }
    }

    override fun setOnErrorListener(l: OnErrorListener?) {
        onErrorListener = l
    }

    override fun setAudioSource(audioSource: Int) {
        super.setAudioSource(audioSource)
        state = RecorderState.Initialized
    }

    override fun setVideoSource(video_source: Int) {
        super.setVideoSource(video_source)
        state = RecorderState.Initialized
    }

    override fun setOutputFormat(output_format: Int) {
        super.setOutputFormat(output_format)
        state = RecorderState.DataSourceConfigured
    }

    override fun prepare() {
        super.prepare()
        state = RecorderState.Prepared
    }

    override fun start() {
        super.start()
        state = RecorderState.Recording
    }

    override fun stop() {
        super.stop()
        state = RecorderState.Initial
    }

    override fun reset() {
        super.reset()
        state = RecorderState.Initial
    }

    override fun release() {
        super.release()
        state = RecorderState.Released
    }

    override fun pause() {
        super.pause()
        state = RecorderState.Paused
    }

    override fun resume() {
        super.resume()
        state = RecorderState.Recording
    }

    val initial get() = state == RecorderState.Initial
    val initialized get() = state == RecorderState.Initialized
    val dataSourceConfigured get() = state == RecorderState.DataSourceConfigured
    val prepared get() = state == RecorderState.Prepared
    val recording get() = state == RecorderState.Recording
    val paused get() = state == RecorderState.Paused
    val released get() = state == RecorderState.Released
    val error get() = state == RecorderState.Error
}
