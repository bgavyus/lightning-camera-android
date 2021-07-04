package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.content.*
import io.github.bgavyus.lightningcamera.extensions.toInt
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import java.io.File.separator
import java.io.FileDescriptor
import java.time.Clock
import java.time.Period
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.Q)
@AutoFactory(implementing = [StorageFileFactory::class])
class ScopedStorageFile(
    @Provided private val contentResolver: ContentResolver,
    @Provided clock: Clock,
    mimeType: String,
    mediaDirectory: MediaDirectory,
    appDirectoryName: String,
    name: String,
) : DeferScope(), StorageFile {
    private val pending = AtomicBoolean(true)

    private val uri = contentResolver.requireInsert(mediaDirectory.externalStorageContentUri) {
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.IS_PENDING, true.toInt())

        val segments = listOf(mediaDirectory.value, appDirectoryName)
        put(MediaStore.MediaColumns.RELATIVE_PATH, segments.joinToString(separator))

        val expirationTime = clock.instant() + Period.ofDays(1)
        put(MediaStore.MediaColumns.DATE_EXPIRES, expirationTime.epochSecond)
    }
        .also { Logger.log("Inserted URI: $it") }

    init {
        defer(::discardIfPending)
    }

    private val file = contentResolver.requireOpenFileDescriptor(uri, FileMode.Write)
        .also { defer(it::close) }

    override val descriptor: FileDescriptor get() = file.fileDescriptor
    override val path get() = throw NotImplementedError()

    override fun keep() {
        if (pending.compareAndSet(true, false)) {
            save()
        }
    }

    private fun save() = contentResolver.requireUpdate(uri) {
        putNull(MediaStore.MediaColumns.DATE_EXPIRES)
        put(MediaStore.MediaColumns.IS_PENDING, false.toInt())
    }

    private fun discardIfPending() {
        if (pending.get()) {
            discard()
        }
    }

    private fun discard() {
        contentResolver.requireDelete(uri)
    }
}
