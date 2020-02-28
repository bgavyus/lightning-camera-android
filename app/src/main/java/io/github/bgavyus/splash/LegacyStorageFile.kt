package io.github.bgavyus.splash

import android.content.Context
import java.io.FileDescriptor

class LegacyStorageFile(context: Context) : PendingFile {
	init {

	}
	override val descriptor: FileDescriptor
		get() = TODO("not implemented")

	override fun save() {
		TODO("not implemented")
	}

	override fun discard() {
		TODO("not implemented")
	}
}
