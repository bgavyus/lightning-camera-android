package io.github.bgavyus.lightningcamera.extensions.android.database

import android.database.Cursor

fun Cursor.requireMoveToPosition(position: Int) = check(moveToPosition(position))

inline operator fun <reified T> Cursor.get(index: Int): T {
    val value = when (T::class) {
        ByteArray::class -> getBlob(index)
        String::class -> getString(index)
        Short::class -> getShort(index)
        Int::class -> getInt(index)
        Long::class -> getLong(index)
        Float::class -> getFloat(index)
        Double::class -> getDouble(index)
        else -> throw IllegalArgumentException()
    }

    return value as T
}
