package io.github.bgavyus.splash.storage

interface PendingOperation {
	fun save()
	fun discard()
}
