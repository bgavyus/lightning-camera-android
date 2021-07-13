package io.github.bgavyus.lightningcamera.storage

import android.database.Cursor
import io.github.bgavyus.lightningcamera.extensions.android.database.get

class CachedColumnCursor(cursor: Cursor) : Cursor by cursor {
    val columnIndexes = columnNames
        .mapIndexed { index, name -> name to index }
        .toMap()

    inline operator fun <reified T> get(name: String): T = this[columnIndexes.getValue(name)]
}
