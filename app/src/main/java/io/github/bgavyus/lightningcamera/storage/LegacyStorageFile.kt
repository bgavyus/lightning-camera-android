package io.github.bgavyus.lightningcamera.storage

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

@Suppress("DEPRECATION")
class LegacyStorageFile(
    private val contentResolver: ContentResolver,
    private val mimeType: String,
    private val standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String,
) : StorageFile {
    companion object {
        val permissions = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val parentDirectory =
        File(
            Environment.getExternalStoragePublicDirectory(standardDirectory.value),
            appDirectoryName,
        ).apply {
            if (!exists()) {
                if (!mkdirs()) {
                    throw IOException("Failed to create directory $path")
                }
            }
        }

    private val file = File(parentDirectory, name)
        .apply { delete() }

    private val outputStream = file.outputStream()

    override val descriptor: FileDescriptor get() = outputStream.fd
    override val path: String get() = file.path
    private var pending = true

    override fun keep() {
        if (pending) {
            save()
            pending = false
        }
    }

    override fun close() = outputStream.close()

    private fun save() {
        if (!file.renameTo(file)) {
            throw IOException("Failed to rename ${file.absolutePath}")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
        }

        contentResolver.insert(standardDirectory.externalStorageContentUri, contentValues)
            ?: throw IOException("Failed to add file to media store: $file")
    }
}
