package io.github.bgavyus.splash

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

class MediaStoreFile(contentResolver: ContentResolver, mode: String, mimeType: String,
					 baseUri: Uri, relativePath: String, name: String) {
	private val mContentResolver = contentResolver
	private val mUri = contentResolver.insert(baseUri, ContentValues().apply {
		put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
		put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
		put(MediaStore.MediaColumns.DISPLAY_NAME, name)
	})!!
	private val mFile = contentResolver.openFileDescriptor(mUri, mode)!!

	val fileDescriptor
		get() = mFile.fileDescriptor!!

	fun close() {
		mFile.checkError()
		mFile.close()
	}

	fun delete() {
		val rowsDeleted = mContentResolver.delete(mUri, null, null)

		if (rowsDeleted != 1) {
			throw IOException("Failed to delete $mUri")
		}
	}
}
