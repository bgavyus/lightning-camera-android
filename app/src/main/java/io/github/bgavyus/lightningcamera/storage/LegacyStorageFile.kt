package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.content.requireInsert
import io.github.bgavyus.lightningcamera.extensions.java.io.mkdirsIfNotExists
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
@AutoFactory(implementing = [StorageFileFactory::class])
class LegacyStorageFile(
    @Provided private val contentResolver: ContentResolver,
    @Provided private val mediaDirectory: MediaDirectory,
    private val mimeType: String,
    appDirectoryName: String,
    name: String,
) : DeferScope(), StorageFile {
    private val pending = AtomicBoolean(true)
    private val file: PendingFile

    init {
        val root = Environment.getExternalStoragePublicDirectory(mediaDirectory.value)

        val parent = File(root, appDirectoryName)
            .apply(File::mkdirsIfNotExists)

        file = PendingFile(parent, name)
    }

    private val outputStream = file.outputStream()
        .also { defer(it::close) }

    override val descriptor: FileDescriptor get() = outputStream.fd
    override val path: String get() = file.path

    override fun keep() {
        if (pending.compareAndSet(true, false)) {
            save()
        }
    }

    private fun save() {
        file.save()

        val values = contentValuesOf(
            MediaStore.MediaColumns.MIME_TYPE to mimeType,
            MediaStore.MediaColumns.DISPLAY_NAME to file.name,
            MediaStore.MediaColumns.DATA to file.absolutePath,
        )

        contentResolver.requireInsert(mediaDirectory.externalStorageContentUri, values)
    }
}
