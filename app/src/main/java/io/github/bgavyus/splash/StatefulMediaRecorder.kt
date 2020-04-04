package io.github.bgavyus.splash

import android.media.MediaRecorder

open class StatefulMediaRecorder : MediaRecorder() {
    private var _state = RecorderState.Initial
    private var onErrorListener: OnErrorListener? = null

    init {
        MediaRecorder()
        setOnErrorListener { mr, what, extra ->
            _state = RecorderState.Error
            onErrorListener?.onError(mr, what, extra)
        }
    }

    val state get() = _state

    override fun setOnErrorListener(l: OnErrorListener?) {
        onErrorListener = l
    }

    override fun setAudioSource(audioSource: Int) {
        super.setAudioSource(audioSource)
        _state = RecorderState.Initialized
    }

    override fun setVideoSource(video_source: Int) {
        super.setVideoSource(video_source)
        _state = RecorderState.Initialized
    }

    override fun setOutputFormat(output_format: Int) {
        super.setOutputFormat(output_format)
        _state = RecorderState.DataSourceConfigured
    }

    override fun prepare() {
        super.prepare()
        _state = RecorderState.Prepared
    }

    override fun start() {
        super.start()
        _state = RecorderState.Recording
    }

    override fun stop() {
        super.stop()
        _state = RecorderState.Initial
    }

    override fun reset() {
        super.reset()
        _state = RecorderState.Initial
    }

    override fun release() {
        super.release()
        _state = RecorderState.Released
    }

    override fun pause() {
        super.pause()
        _state = RecorderState.Paused
    }

    override fun resume() {
        super.resume()
        _state = RecorderState.Recording
    }
}
