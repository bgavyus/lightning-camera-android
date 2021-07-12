package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.os.bundleOf
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
    fun toQueryArguments() = bundleOf(
        ContentResolver.QUERY_ARG_SORT_COLUMNS to columns.toTypedArray(),
        ContentResolver.QUERY_ARG_SORT_DIRECTION to direction.queryArgument,
    )

    fun toSql() = "${columns.joinToString()} ${direction.sql}"
}

@RequiresApi(Build.VERSION_CODES.Q)
fun ContentResolver.requireOpenFile(
    uri: Uri,
    mode: FileMode,
    cancellationSignal: CancellationSignal? = null,
) = openFile(uri, mode.acronym, cancellationSignal) ?: throw IllegalStateException()

fun ContentResolver.requireInsert(
    @RequiresPermission.Write url: Uri,
    values: ContentValues? = null,
) = insert(url, values) ?: throw IllegalStateException()

fun ContentResolver.requireUpdate(
    @RequiresPermission.Write uri: Uri,
    values: ContentValues? = null,
    where: String? = null,
    selectionArgs: Array<String>? = null,
) {
    val updatedRowsCount = update(uri, values, where, selectionArgs)
    check(updatedRowsCount.isPositive)
}

fun ContentResolver.requireDelete(
    @RequiresPermission.Write uri: Uri,
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
