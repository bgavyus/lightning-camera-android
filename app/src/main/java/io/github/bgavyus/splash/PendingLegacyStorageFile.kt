package io.github.bgavyus.splash

import android.content.Context
import java.io.FileDescriptor

class PendingLegacyStorageFile(
	context: Context,
	mimeType: String,
	path: Iterable<String>,
	name: String
) : PendingFile {

	override val descriptor: FileDescriptor
		get() = TODO("not implemented")

	override fun save() {
		TODO("not implemented")
	}

	override fun discard() {
		TODO("not implemented")
	}
}
