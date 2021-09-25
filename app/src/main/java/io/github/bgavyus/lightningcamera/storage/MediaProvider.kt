package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.android.content.*
import io.github.bgavyus.lightningcamera.extensions.android.database.CursorList
import io.github.bgavyus.lightningcamera.extensions.android.database.asList
import io.github.bgavyus.lightningcamera.extensions.android.media.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class MediaProvider @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaDirectory: MediaDirectory,
) : ThumbnailsProvider {
    // TODO: convert to non-blocking
    fun list(): CursorList<MediaMetadata> {
        val baseUri = mediaDirectory.externalStorageContentUri
        val contentCursor = contentResolver.requireQuery(baseUri, columnNames, sortOrder)
        val metadataRetriever = MediaMetadataRetriever()
        val mediaCursor = MediaCursor(contentCursor, metadataRetriever)

        return mediaCursor.asList { cursor ->
            val mediaUri = ContentUris.withAppendedId(baseUri, cursor[MediaStore.MediaColumns._ID])

            contentResolver.requireOpenFileDescriptor(mediaUri, FileMode.Read)
                .use { metadataRetriever.setDataSource(it.fileDescriptor) }

            MediaMetadata(
                uri = mediaUri,
                title = cursor[MediaStore.MediaColumns.TITLE],
                dateAdded = Instant.ofEpochSecond(cursor[MediaStore.MediaColumns.DATE_ADDED]),
                duration = Duration.ofMillis(metadataRetriever[MediaMetadataRetriever.METADATA_KEY_DURATION])
            )
        }
    }

    override suspend fun thumbnail(uri: Uri, size: Size) = withContext(Dispatchers.IO) {
        contentResolver.loadThumbnailCompat(uri, size)
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
