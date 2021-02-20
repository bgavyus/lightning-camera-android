package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.os.Environment
import android.provider.MediaStore
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.content.requireInsert
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
@AutoFactory(implementing = [StorageFileFactory::class])
class LegacyStorageFile(
    @Provided private val contentResolver: ContentResolver,
    private val mimeType: String,
    private val standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String,
) : StorageFile {
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
        .apply(File::delete)

    private val outputStream = file.outputStream()

    override val descriptor: FileDescriptor get() = outputStream.fd
    override val path: String get() = file.path
    private val pending = AtomicBoolean(true)

    override fun keep() {
        if (pending.compareAndSet(true, false)) {
            save()
        }
    }

    override fun close() = outputStream.close()

    private fun save() {
        if (!file.renameTo(file)) {
            throw IOException("Failed to rename ${file.absolutePath}")
        }

        contentResolver.requireInsert(standardDirectory.externalStorageContentUri) {
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
        }
    }
}
