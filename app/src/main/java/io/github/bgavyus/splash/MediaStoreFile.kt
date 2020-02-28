package io.github.bgavyus.splash

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import java.io.IOException
import java.nio.file.Paths

class MediaStoreFile(
    contentResolver: ContentResolver, mode: String, mimeType: String, collection: Uri,
    relativePath: String, name: String
) : PendingFile {
    companion object {
        const val IS_PENDING_TRUE = 1
        const val IS_PENDING_FALSE = 0
    }

    private val mContentResolver = contentResolver
    private val mUri = contentResolver.insert(collection, ContentValues().apply {
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.IS_PENDING, IS_PENDING_TRUE)
        put(
            MediaStore.MediaColumns.DATE_EXPIRES,
            (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000
        )
    }) ?: throw IOException(
        "Failed to create ${Paths.get(
            collection.toString(),
            relativePath,
            name
        )}"
    )

    private val mFile = contentResolver.openFileDescriptor(mUri, mode)
        ?: throw IOException("Failed to open $mUri")

    override val descriptor
        get() = mFile.fileDescriptor
            ?: throw IOException("Failed to get file descriptor for $mUri")

    override fun save() {
        close()
        markAsDone()
    }

    override fun discard() {
        close()
        delete()
    }

    private fun close() {
        mFile.close()
    }

    private fun markAsDone() {
        mContentResolver.update(mUri, ContentValues().apply {
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            put(MediaStore.MediaColumns.IS_PENDING, IS_PENDING_FALSE)
        }, null, null)
    }

    private fun delete() {
        val rowsDeleted = mContentResolver.delete(mUri, null, null)

        if (rowsDeleted != 1) {
            throw IOException("Failed to delete $mUri")
        }
    }
}
