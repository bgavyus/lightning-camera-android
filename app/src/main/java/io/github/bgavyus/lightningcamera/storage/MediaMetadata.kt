package io.github.bgavyus.lightningcamera.storage

import android.provider.MediaStore
import java.time.Instant

data class MediaMetadata(
    val id: Long,
    val title: String,
    val dateAdded: Instant,
) {
    constructor(cursor: CachedColumnCursor) : this(
        id = cursor[MediaStore.MediaColumns._ID],
        title = cursor[MediaStore.MediaColumns.TITLE],
        dateAdded = Instant.ofEpochSecond(cursor[MediaStore.MediaColumns.DATE_ADDED])
    )
}
