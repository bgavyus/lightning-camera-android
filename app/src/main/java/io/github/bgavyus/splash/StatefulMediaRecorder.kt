package io.github.bgavyus.splash

import android.media.MediaRecorder

enum class State {
	Initial,
	Initialized,
	DataSourceConfigured,
	Prepared,
	Recording,
	Released,
	Error,
	Paused
}

class StatefulMediaRecorder : MediaRecorder() {
	private var mState = State.Initial
	private var mErrorListener: OnErrorListener? = null

	init {
		MediaRecorder()
		setOnErrorListener { mr, what, extra ->
			mState = State.Error
			mErrorListener?.onError(mr, what, extra)
		}
	}

	val state
		get() = mState

	override fun setOnErrorListener(l: OnErrorListener?) {
		mErrorListener = l
	}

	override fun setAudioSource(audioSource: Int) {
		super.setAudioSource(audioSource)
		mState = State.Initialized
	}

	override fun setVideoSource(video_source: Int) {
		super.setVideoSource(video_source)
		mState = State.Initialized
	}

	override fun setOutputFormat(output_format: Int) {
		super.setOutputFormat(output_format)
		mState = State.DataSourceConfigured
	}

	override fun prepare() {
		super.prepare()
		mState = State.Prepared
	}

	override fun start() {
		super.start()
		mState = State.Recording
	}

	override fun stop() {
		super.stop()
		mState = State.Initial
	}

	override fun reset() {
		super.reset()
		mState = State.Initial
	}

	override fun release() {
		super.release()
		mState = State.Released
	}

	override fun pause() {
		super.pause()
		mState = State.Paused
	}

	override fun resume() {
		super.resume()
		mState = State.Recording
	}
}
