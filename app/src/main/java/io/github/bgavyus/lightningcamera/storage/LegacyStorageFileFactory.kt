package io.github.bgavyus.lightningcamera.storage

import android.content.ContentResolver
import javax.inject.Inject

class LegacyStorageFileFactory @Inject constructor(
    private val contentResolver: ContentResolver,
) : StorageFileFactory {
    override fun create(
        mimeType: String,
        standardDirectory: StandardDirectory,
        appDirectoryName: String,
        name: String,
    ) = LegacyStorageFile(contentResolver, mimeType, standardDirectory, appDirectoryName, name)
}
