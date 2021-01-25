package io.github.bgavyus.lightningcamera.storage

import android.annotation.TargetApi
import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.extensions.android.content.*
import io.github.bgavyus.lightningcamera.extensions.toInt
import java.io.File.separator
import java.time.Clock
import java.time.Period
import java.util.concurrent.atomic.AtomicBoolean

@TargetApi(Build.VERSION_CODES.Q)
class ScopedStorageFile(
    private val contentResolver: ContentResolver,
    clock: Clock,
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String,
) : StorageFile {
    private val uri = contentResolver.requireInsert(standardDirectory.externalStorageContentUri) {
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)

        val segments = listOf(standardDirectory.value, appDirectoryName)
        put(MediaStore.MediaColumns.RELATIVE_PATH, segments.joinToString(separator))

        put(MediaStore.MediaColumns.IS_PENDING, true.toInt())

        val expirationTime = clock.instant() + Period.ofDays(1)
        put(MediaStore.MediaColumns.DATE_EXPIRES, expirationTime.epochSecond)
    }
        .also { Logger.log("Inserted URI: $it") }

    private val file = contentResolver.requireOpenFileDescriptor(uri, FileMode.Write)

    override val descriptor
        get() = file.fileDescriptor
            ?: throw RuntimeException()

    override val path get() = throw NotImplementedError()
    private val pending = AtomicBoolean(true)

    override fun keep() {
        if (pending.compareAndSet(true, false)) {
            save()
        }
    }

    override fun close() {
        file.close()

        if (pending.get()) {
            discard()
        }
    }

    private fun save() = contentResolver.requireUpdate(uri) {
        putNull(MediaStore.MediaColumns.DATE_EXPIRES)
        put(MediaStore.MediaColumns.IS_PENDING, false.toInt())
    }

    private fun discard() = contentResolver.requireDelete(uri)
}
