package io.github.bgavyus.lightningcamera.storage

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.extensions.toInt
import java.io.File.separator
import java.io.IOException
import java.time.Clock
import java.time.Period

@TargetApi(Build.VERSION_CODES.Q)
class ScopedStorageFile(
    private val contentResolver: ContentResolver,
    clock: Clock,
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

            val expirationTime = clock.instant().plus(Period.ofDays(1))
            put(MediaStore.MediaColumns.DATE_EXPIRES, expirationTime.epochSecond)
        }

        uri = contentResolver.insert(standardDirectory.externalStorageContentUri, contentValues)
            ?: throw IOException("Failed to create")

        Logger.debug("Inserted URI: $uri")

        file = contentResolver.openFileDescriptor(uri, "w")
            ?: throw IOException("Failed to open")
    }

    override val descriptor
        get() = file.fileDescriptor
            ?: throw IOException("Failed to get file descriptor")

    override val path get() = throw NotImplementedError()
    private var pending = true

    override fun keep() {
        if (pending) {
            save()
            pending = false
        }
    }

    override fun close() {
        file.close()

        if (pending) {
            discard()
        }
    }

    private fun save() {
        val values = ContentValues().apply {
            putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            put(MediaStore.MediaColumns.IS_PENDING, false.toInt())
        }

        contentResolver.update(uri, values, null, null)
    }

    private fun discard() {
        val rowsDeletedCount = contentResolver.delete(uri, null, null)

        if (rowsDeletedCount == 0) {
            throw IOException("Failed to delete")
        }
    }
}
