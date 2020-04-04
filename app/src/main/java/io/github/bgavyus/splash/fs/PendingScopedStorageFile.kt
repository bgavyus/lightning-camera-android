package io.github.bgavyus.splash.fs

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.text.format.DateUtils
import java.io.File
import java.io.IOException

class PendingScopedStorageFile(
    context: Context,
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String
) : PendingFile {
    companion object {
        const val IS_PENDING_TRUE = 1
        const val IS_PENDING_FALSE = 0
    }

    private val contentResolver = context.contentResolver

    private val uri = contentResolver.insert(standardDirectory.externalStorage, ContentValues().apply {
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            listOf(standardDirectory.value, appDirectoryName).joinToString(File.separator)
        )
        put(MediaStore.MediaColumns.IS_PENDING,
            IS_PENDING_TRUE
        )
        put(
            MediaStore.MediaColumns.DATE_EXPIRES,
            (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000
        )
    }) ?: throw IOException(
        "Failed to create ${standardDirectory.externalStorage.buildUpon()
            .appendPath(standardDirectory.value)
            .appendPath(appDirectoryName)
            .appendPath(name)}"
    )

    private val file = contentResolver.openFileDescriptor(uri, "w")
        ?: throw IOException("Failed to open $uri")

    override val descriptor
        get() = file.fileDescriptor
            ?: throw IOException("Failed to get file descriptor for $uri")

    override fun save() {
        close()
        markAsDone()
    }

    override fun discard() {
        close()
        delete()
    }

    private fun close() {
        file.close()
    }

    private fun markAsDone() {
        contentResolver.update(uri, ContentValues().apply {
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            put(MediaStore.MediaColumns.IS_PENDING,
                IS_PENDING_FALSE
            )
        }, null, null)
    }

    private fun delete() {
        val rowsDeleted = contentResolver.delete(uri, null, null)

        if (rowsDeleted != 1) {
            throw IOException("Failed to delete $uri")
        }
    }
}
