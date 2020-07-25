package io.github.bgavyus.splash.storage

import android.annotation.TargetApi
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import io.github.bgavyus.splash.common.Application
import java.io.File
import java.io.IOException

@TargetApi(Build.VERSION_CODES.Q)
class TentativeScopedStorageFile(
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String
) : TentativeFile {
    private val contentResolver = Application.context.contentResolver

    private val uri =
        contentResolver.insert(standardDirectory.externalStorage, ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                listOf(standardDirectory.value, appDirectoryName).joinToString(File.separator)
            )
            put(MediaStore.MediaColumns.IS_PENDING, true.toInt())
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

    override val path: String get() = throw NotImplementedError()

    override fun close() = file.close()

    override fun save() {
        val values = ContentValues().apply {
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            put(MediaStore.MediaColumns.IS_PENDING, false.toInt())
        }

        contentResolver.update(uri, values, /* where = */ null, /* selectionArgs = */ null)
    }

    override fun discard() {
        val rowsDeleted =
            contentResolver.delete(uri, /* where = */ null, /* selectionArgs = */ null)

        if (rowsDeleted != 1) {
            throw IOException("Failed to delete $uri")
        }
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
