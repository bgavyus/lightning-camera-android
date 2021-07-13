package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.provider.MediaStore
import io.github.bgavyus.lightningcamera.extensions.android.content.SortDirection
import io.github.bgavyus.lightningcamera.extensions.android.content.SortOrder
import io.github.bgavyus.lightningcamera.extensions.android.content.requireQuery
import io.github.bgavyus.lightningcamera.extensions.android.database.requireMoveToPosition
import javax.inject.Inject

class MediaMetadataList @Inject constructor(
    contentResolver: ContentResolver,
    mediaDirectory: MediaDirectory,
) : AbstractList<MediaMetadata>(), AutoCloseable {
    private val cursor = contentResolver.requireQuery(
        mediaDirectory.externalStorageContentUri,
        columnNames,
        sortOrder
    )
        .let { CachedColumnCursor(it) }

    override val size get() = cursor.count

    override fun get(index: Int): MediaMetadata {
        cursor.requireMoveToPosition(index)
        return MediaMetadata(cursor)
    }

    override fun close() {
        cursor.close()
    }

    companion object {
        private val columnNames = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATE_ADDED,
        )

        private val sortOrder = SortOrder(
            listOf(MediaStore.MediaColumns.DATE_ADDED),
            SortDirection.Descending
        )
    }
}
