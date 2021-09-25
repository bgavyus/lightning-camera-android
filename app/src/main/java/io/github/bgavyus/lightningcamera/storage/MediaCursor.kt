package io.github.bgavyus.lightningcamera.storage

import android.database.Cursor
import android.media.MediaMetadataRetriever

class MediaCursor(
    private val cursor: Cursor,
    private val retriever: MediaMetadataRetriever,
) : Cursor by cursor {
    override fun close() {
        retriever.close()
        cursor.close()
    }
}
