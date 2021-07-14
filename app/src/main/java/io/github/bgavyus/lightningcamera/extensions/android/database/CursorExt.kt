package io.github.bgavyus.lightningcamera.extensions.android.database

import android.database.Cursor

fun Cursor.requireMoveToPosition(position: Int) {
    check(moveToPosition(position))
}

inline operator fun <reified T> Cursor.get(index: Int) = when (T::class) {
    ByteArray::class -> getBlob(index)
    String::class -> getString(index)
    Short::class -> getShort(index)
    Int::class -> getInt(index)
    Long::class -> getLong(index)
    Float::class -> getFloat(index)
    Double::class -> getDouble(index)
    else -> throw IllegalArgumentException()
} as T

fun <T> Cursor.asList(factory: (CachedColumnCursor) -> T) =
    CursorList(CachedColumnCursor(this), factory)

class CachedColumnCursor(cursor: Cursor) : Cursor by cursor {
    val columnIndexes = columnNames.mapIndexed { index, name -> name to index }.toMap()
    inline operator fun <reified T> get(name: String): T = this[columnIndexes.getValue(name)]
}

class CursorList<Element>(
    private val cursor: CachedColumnCursor,
    private val factory: (CachedColumnCursor) -> Element,
) : AbstractList<Element>(), AutoCloseable by cursor {
    override val size get() = cursor.count

    override fun get(index: Int): Element {
        cursor.requireMoveToPosition(index)
        return factory(cursor)
    }
}
