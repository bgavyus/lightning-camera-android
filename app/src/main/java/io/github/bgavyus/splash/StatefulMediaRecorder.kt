package io.github.bgavyus.splash

import android.media.MediaRecorder

enum class RecorderState {
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
	private var mState = RecorderState.Initial
	private var mOnErrorListener: OnErrorListener? = null

	init {
		MediaRecorder()
		setOnErrorListener { mr, what, extra ->
			mState = RecorderState.Error
			mOnErrorListener?.onError(mr, what, extra)
		}
	}

	val state
		get() = mState

	override fun setOnErrorListener(l: OnErrorListener?) {
		mOnErrorListener = l
	}

	override fun setAudioSource(audioSource: Int) {
		super.setAudioSource(audioSource)
		mState = RecorderState.Initialized
	}

	override fun setVideoSource(video_source: Int) {
		super.setVideoSource(video_source)
		mState = RecorderState.Initialized
	}

	override fun setOutputFormat(output_format: Int) {
		super.setOutputFormat(output_format)
		mState = RecorderState.DataSourceConfigured
	}

	override fun prepare() {
		super.prepare()
		mState = RecorderState.Prepared
	}

	override fun start() {
		super.start()
		mState = RecorderState.Recording
	}

	override fun stop() {
		super.stop()
		mState = RecorderState.Initial
	}

	override fun reset() {
		super.reset()
		mState = RecorderState.Initial
	}

	override fun release() {
		super.release()
		mState = RecorderState.Released
	}

	override fun pause() {
		super.pause()
		mState = RecorderState.Paused
	}

	override fun resume() {
		super.resume()
		mState = RecorderState.Recording
	}
}
