package io.github.bgavyus.splash.fs

interface PendingOperation {
	fun save()
	fun discard()
}
