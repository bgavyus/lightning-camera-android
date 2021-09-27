package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.android.content.*
import io.github.bgavyus.lightningcamera.extensions.android.database.toList
import io.github.bgavyus.lightningcamera.extensions.android.media.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class MediaMetadataProvider @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaDirectory: MediaDirectory,
) {
    suspend fun list() = withContext(Dispatchers.IO) {
        val baseUri = mediaDirectory.externalStorageContentUri

        contentResolver.requireQuery(baseUri, columnNames, sortOrder).toList { cursor ->
            val mediaUri = ContentUris.withAppendedId(baseUri, cursor[MediaStore.MediaColumns._ID])

            MediaMetadata(
                uri = mediaUri,
                title = cursor[MediaStore.MediaColumns.TITLE],
                dateAdded = Instant.ofEpochSecond(cursor[MediaStore.MediaColumns.DATE_ADDED]),
            )
        }
    }

    suspend fun thumbnail(uri: Uri, size: Size) = withContext(Dispatchers.IO) {
        contentResolver.loadThumbnailCompat(uri, size)
    }

    suspend fun duration(uri: Uri): Duration = withContext(Dispatchers.IO) {
        MediaMetadataRetriever().use { metadataRetriever ->
            contentResolver.requireOpenFileDescriptor(uri, FileMode.Read)
                .use { metadataRetriever.setDataSource(it.fileDescriptor) }

            Duration.ofMillis(metadataRetriever[MediaMetadataRetriever.METADATA_KEY_DURATION])
        }
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
