package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.bgavyus.lightningcamera.extensions.isPositive

enum class SortDirection {
    Ascending,
    Descending,
    ;

    val queryArgument
        @RequiresApi(Build.VERSION_CODES.O)
        get() = when (this) {
            Ascending -> ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
            Descending -> ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        }

    val sql
        get() = when (this) {
            Ascending -> "ASC"
            Descending -> "DESC"
        }
}

enum class FileMode {
    Read,
    Write,
    WriteAppend,
    ReadWrite,
    ReadWriteTruncate,
    ;

    val acronym
        get() = when (this) {
            Read -> "r"
            Write -> "w"
            WriteAppend -> "wa"
            ReadWrite -> "rw"
            ReadWriteTruncate -> "rwt"
        }
}

data class SortOrder(val columns: Collection<String>, val direction: SortDirection) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toQueryArguments() = Bundle().apply {
        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, columns.toTypedArray())
        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, direction.queryArgument)
    }

    fun toSql() = "${columns.joinToString()} ${direction.sql}"
}

fun ContentResolver.requireOpenFileDescriptor(
    url: Uri,
    mode: FileMode,
    cancellationSignal: CancellationSignal? = null,
) = openFileDescriptor(url, mode, cancellationSignal) ?: throw IllegalStateException()

fun ContentResolver.openFileDescriptor(
    url: Uri,
    mode: FileMode,
    cancellationSignal: CancellationSignal? = null,
) = openFileDescriptor(url, mode.acronym, cancellationSignal)

fun ContentResolver.requireInsert(url: Uri, valuesBlock: ContentValues.() -> Unit) =
    insert(url, valuesBlock) ?: throw IllegalStateException()

fun ContentResolver.insert(url: Uri, valuesBlock: ContentValues.() -> Unit) =
    insert(url, ContentValues().apply(valuesBlock))

fun ContentResolver.requireUpdate(
    uri: Uri,
    where: String? = null,
    selectionArgs: Array<String>? = null,
    valuesBlock: ContentValues.() -> Unit,
) {
    val updatedRowsCount = update(uri, where, selectionArgs, valuesBlock)
    check(updatedRowsCount.isPositive)
}

fun ContentResolver.update(
    uri: Uri,
    where: String? = null,
    selectionArgs: Array<String>? = null,
    valuesBlock: ContentValues.() -> Unit,
) = update(uri, ContentValues().apply(valuesBlock), where, selectionArgs)

fun ContentResolver.requireDelete(
    uri: Uri,
    where: String? = null,
    selectionArgs: Array<String>? = null,
) {
    val deletedRowsCount = delete(uri, where, selectionArgs)
    check(deletedRowsCount.isPositive)
}

fun ContentResolver.requireQuery(
    @RequiresPermission.Read uri: Uri,
    projection: Array<String>? = null,
    sortOrder: SortOrder? = null,
    cancellationSignal: CancellationSignal? = null,
) = query(uri, projection, sortOrder, cancellationSignal) ?: throw IllegalStateException()

fun ContentResolver.query(
    @RequiresPermission.Read uri: Uri,
    projection: Array<String>? = null,
    sortOrder: SortOrder? = null,
    cancellationSignal: CancellationSignal? = null,
) = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
    query(uri, projection, null, null, sortOrder?.toSql(), cancellationSignal)
} else {
    query(uri, projection, sortOrder?.toQueryArguments(), cancellationSignal)
}
