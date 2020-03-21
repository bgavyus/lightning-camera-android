package io.github.bgavyus.splash

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

@Suppress("DEPRECATION")
class PendingLegacyStorageFile(
    private val context: Context,
    private val mimeType: String,
    private val standardDirectory: StandardDirectory,
    appDirectoryName: String,
    private val name: String
) : PendingFile {

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

    private val tempFile = File(parentDirectory, "$name.tmp").apply {
        delete()
    }

    private val tempFileOutputStream = tempFile.outputStream()

    override val descriptor: FileDescriptor
        get() = tempFileOutputStream.fd

    override fun save() {
        close()

        val file = File(parentDirectory, name)

        if (!tempFile.renameTo(file)) {
            throw IOException("Failed to rename ${tempFile.absolutePath} to ${file.absolutePath}")
        }

        addToMediaStore(file)
    }

    private fun addToMediaStore(file: File): Uri {
        return context.contentResolver.insert(
            standardDirectory.externalStorage,
            ContentValues().apply {
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }) ?: throw IOException(
            "Failed to add file to media store: $file"
        )
    }

    override fun discard() {
        close()
    }

    private fun close() {
        tempFileOutputStream.close()
    }
}
