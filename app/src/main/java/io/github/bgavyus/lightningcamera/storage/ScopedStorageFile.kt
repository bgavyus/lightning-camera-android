package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.contentValuesOf
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.content.*
import io.github.bgavyus.lightningcamera.extensions.toInt
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import java.io.File
import java.io.FileDescriptor
import java.time.Clock
import java.time.Period
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.Q)
@AutoFactory(implementing = [StorageFileFactory::class])
class ScopedStorageFile(
    @Provided private val contentResolver: ContentResolver,
    @Provided clock: Clock,
    @Provided mediaDirectory: MediaDirectory,
    mimeType: String,
    appDirectoryName: String,
    name: String,
) : DeferScope(), StorageFile {
    private val pending = AtomicBoolean(true)

    private val uri = run {
        val segments = listOf(mediaDirectory.value, appDirectoryName)
        val expirationTime = clock.instant() + Period.ofDays(1)

        val values = contentValuesOf(
            MediaStore.MediaColumns.MIME_TYPE to mimeType,
            MediaStore.MediaColumns.DISPLAY_NAME to name,
            MediaStore.MediaColumns.IS_PENDING to true.toInt(),
            MediaStore.MediaColumns.RELATIVE_PATH to segments.joinToString(File.separator),
            MediaStore.MediaColumns.DATE_EXPIRES to expirationTime.epochSecond,
        )

        contentResolver.requireInsert(mediaDirectory.externalStorageContentUri, values)
            .also { Logger.log("Inserted URI: $it") }
    }

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

    private fun save() {
        val values = contentValuesOf(
            MediaStore.MediaColumns.DATE_EXPIRES to null,
            MediaStore.MediaColumns.IS_PENDING to false.toInt(),
        )

        contentResolver.requireUpdate(uri, values)
    }

    private fun discardIfPending() {
        if (pending.get()) {
            Logger.log("Discarding")
            discard()
        }
    }

    private fun discard() {
        contentResolver.requireDelete(uri)
    }
}
