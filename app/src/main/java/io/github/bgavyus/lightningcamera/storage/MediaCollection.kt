package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.content.SortDirection
import io.github.bgavyus.lightningcamera.extensions.android.content.SortOrder
import io.github.bgavyus.lightningcamera.extensions.android.content.requireQuery
import io.github.bgavyus.lightningcamera.extensions.android.database.requireMoveToPosition
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import java.time.Instant

@AutoFactory
class MediaCollection(
    @Provided contentResolver: ContentResolver,
    uri: Uri,
) : AbstractList<MediaItem>(), AutoCloseable {
    private val deferScope = DeferScope()

    private val cursor = contentResolver.requireQuery(uri, columnNames, sortOrder)
        .apply { deferScope.defer(::close) }

    override val size get() = cursor.count

    override fun get(index: Int): MediaItem {
        cursor.requireMoveToPosition(index)
        return item()
    }

    private fun item() = MediaItem(
        id = cursor.getInt(columnIndexes.getValue(MediaStore.MediaColumns._ID)),
        title = cursor.getString(columnIndexes.getValue(MediaStore.MediaColumns.TITLE)),
        dateAdded = Instant.ofEpochSecond(
            cursor.getInt(columnIndexes.getValue(MediaStore.MediaColumns.DATE_ADDED)).toLong()
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
