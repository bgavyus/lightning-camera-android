package io.github.bgavyus.splash

interface Detector {
	fun detected(): Boolean
	fun release()
}
