package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.android.content.SortDirection
import io.github.bgavyus.lightningcamera.extensions.android.content.SortOrder
import io.github.bgavyus.lightningcamera.extensions.android.content.loadThumbnailCompat
import io.github.bgavyus.lightningcamera.extensions.android.content.requireQuery
import io.github.bgavyus.lightningcamera.extensions.android.database.asList
import java.time.Instant
import javax.inject.Inject

class MediaProvider @Inject constructor(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) {
    fun list() = contentResolver.requireQuery(uri, columnNames, sortOrder).asList { cursor ->
        MediaMetadata(
            uri = ContentUris.withAppendedId(uri, cursor[MediaStore.MediaColumns._ID]),
            title = cursor[MediaStore.MediaColumns.TITLE],
            dateAdded = Instant.ofEpochSecond(cursor[MediaStore.MediaColumns.DATE_ADDED])
        )
    }

    fun thumbnail(uri: Uri, size: Size) = contentResolver.loadThumbnailCompat(uri, size)

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
