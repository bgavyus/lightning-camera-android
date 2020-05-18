package io.github.bgavyus.splash.storage

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import io.github.bgavyus.splash.common.App
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

@Suppress("DEPRECATION")
class TentativeLegacyStorageFile(
    private val mimeType: String,
    private val standardDirectory: StandardDirectory,
    appDirectoryName: String,
    private val name: String
) : TentativeFile {

    private val parentDirectory = File(
        Environment.getExternalStoragePublicDirectory(standardDirectory.value),
        appDirectoryName
    ).apply {
        if (!exists()) {
            if (!mkdirs()) {
                throw IOException("Failed to create directory $path")
            }
        }
    }

    private val tempFile = File(parentDirectory, "$name.tmp")
        .apply { delete() }

    private val tempFileOutputStream = tempFile.outputStream()

    override val descriptor: FileDescriptor get() = tempFileOutputStream.fd
    override val path: String get() = tempFile.path

    override fun close() = tempFileOutputStream.close()

    override fun save() {
        val file = File(parentDirectory, name)

        if (!tempFile.renameTo(file)) {
            throw IOException("Failed to rename ${tempFile.absolutePath} to ${file.absolutePath}")
        }

        addToMediaStore(file)
    }

    private fun addToMediaStore(file: File): Uri {
        return App.context.contentResolver.insert(
            standardDirectory.externalStorage,
            ContentValues().apply {
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }) ?: throw IOException(
            "Failed to add file to media store: $file"
        )
    }

    override fun discard() {}
}
