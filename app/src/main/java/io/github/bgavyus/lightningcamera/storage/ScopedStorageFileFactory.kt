package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import java.time.Clock
import javax.inject.Inject

class ScopedStorageFileFactory @Inject constructor(
    private val contentResolver: ContentResolver,
    private val clock: Clock,
) : StorageFileFactory {
    override fun create(
        mimeType: String,
        standardDirectory: StandardDirectory,
        appDirectoryName: String,
        name: String,
    ) = ScopedStorageFile(
        contentResolver,
        clock,
        mimeType,
        standardDirectory,
        appDirectoryName,
        name
    )
}
