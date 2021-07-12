package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.provider.MediaStore
import io.github.bgavyus.lightningcamera.extensions.android.content.SortDirection
import io.github.bgavyus.lightningcamera.extensions.android.content.SortOrder
import io.github.bgavyus.lightningcamera.extensions.android.content.requireQuery
import io.github.bgavyus.lightningcamera.extensions.android.database.requireMoveToPosition
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import java.time.Instant
import javax.inject.Inject

class MediaMetadataList @Inject constructor(
    contentResolver: ContentResolver,
    mediaDirectory: MediaDirectory,
) : AbstractList<MediaMetadata>(), AutoCloseable {
    private val deferScope = DeferScope()

    private val cursor = contentResolver.requireQuery(
        mediaDirectory.externalStorageContentUri,
        columnNames,
        sortOrder
    )
        .apply { deferScope.defer(::close) }

    override val size get() = cursor.count

    override fun get(index: Int): MediaMetadata {
        cursor.requireMoveToPosition(index)
        return element()
    }

    private fun element() = MediaMetadata(
        id = cursor.getLong(columnIndexes.getValue(MediaStore.MediaColumns._ID)),
        title = cursor.getString(columnIndexes.getValue(MediaStore.MediaColumns.TITLE)),
        dateAdded = Instant.ofEpochSecond(
            cursor.getLong(columnIndexes.getValue(MediaStore.MediaColumns.DATE_ADDED))
        )
    )

    override fun close() {
        deferScope.close()
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

        private val columnIndexes = columnNames
            .mapIndexed { index, name -> name to index }
            .toMap()
    }
}