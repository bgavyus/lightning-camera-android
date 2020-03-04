package io.github.bgavyus.splash

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

class PendingLegacyStorageFile(
    context: Context,
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirName: String,
    private val name: String
) : PendingFile {

    private val parentDirectory = File(
        Environment.getExternalStoragePublicDirectory(standardDirectory.value),
        appDirName
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
        rename()
    }

    override fun discard() {
        close()
    }

    private fun close() {
        tempFileOutputStream.close()
    }

    private fun rename() {
        val file = File(parentDirectory, name)

        if (!tempFile.renameTo(file)) {
            throw IOException("Failed to rename ${tempFile.absolutePath} to ${file.absolutePath}")
        }
    }
}
