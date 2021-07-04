package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.CancellationSignal
import io.github.bgavyus.lightningcamera.extensions.isPositive

enum class FileMode {
    Read,
    Write,
    WriteAppend,
    ReadWrite,
    ReadWriteTruncate,
    ;

    val acronym: String
        get() = when (this) {
            Read -> "r"
            Write -> "w"
            WriteAppend -> "wa"
            ReadWrite -> "rw"
            ReadWriteTruncate -> "rwt"
        }
}

fun ContentResolver.requireOpenFileDescriptor(
    url: Uri,
    mode: FileMode,
    cancellationSignal: CancellationSignal? = null,
) = openFileDescriptor(url, mode, cancellationSignal) ?: throw RuntimeException()

fun ContentResolver.openFileDescriptor(
    url: Uri,
    mode: FileMode,
    cancellationSignal: CancellationSignal? = null,
) = openFileDescriptor(url, mode.acronym, cancellationSignal)

fun ContentResolver.requireInsert(url: Uri, valuesBlock: ContentValues.() -> Unit) =
    insert(url, valuesBlock) ?: throw RuntimeException()

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
