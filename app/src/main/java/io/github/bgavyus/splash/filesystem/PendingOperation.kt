package io.github.bgavyus.splash.filesystem

interface PendingOperation {
	fun save()
	fun discard()
}
