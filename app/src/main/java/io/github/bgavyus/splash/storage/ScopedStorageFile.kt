package io.github.bgavyus.splash.storage

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.text.format.DateUtils
import java.io.File.separator
import java.io.IOException

@TargetApi(Build.VERSION_CODES.Q)
class ScopedStorageFile(
    private val contentResolver: ContentResolver,
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String
) : StorageFile {
    private val uri: Uri
    private val file: ParcelFileDescriptor

    init {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)

            val segments = listOf(standardDirectory.value, appDirectoryName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, segments.joinToString(separator))

            put(MediaStore.MediaColumns.IS_PENDING, true.toInt())

            // TODO: Refactor to use DI
            val expirationTime = (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000
            put(MediaStore.MediaColumns.DATE_EXPIRES, expirationTime)
        }

        uri = contentResolver.insert(standardDirectory.externalStorageContentUri, contentValues)
            ?: throw IOException("Failed to create")

        file = contentResolver.openFileDescriptor(uri, "w")
            ?: throw IOException("Failed to open")
    }

    override val descriptor
        get() = file.fileDescriptor
            ?: throw IOException("Failed to get file descriptor")

    override val path: String get() = throw NotImplementedError()
    override var keep = false

    override fun close() {
        file.close()

        if (keep) {
            save()
        } else {
            discard()
        }
    }

    private fun save() {
        val values = ContentValues().apply {
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            put(MediaStore.MediaColumns.IS_PENDING, false.toInt())
        }

        contentResolver.update(uri, values, /* where = */ null, /* selectionArgs = */ null)
    }

    private fun discard() {
        val rowsDeletedCount =
            contentResolver.delete(uri, /* where = */ null, /* selectionArgs = */ null)

        if (rowsDeletedCount == 0) {
            throw IOException("Failed to delete")
        }
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
