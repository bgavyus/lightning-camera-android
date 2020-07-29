package io.github.bgavyus.splash.storage

import android.content.ContentResolver
import android.os.Build
import android.os.Environment

class Storage(
    private val contentResolver: ContentResolver
) {
    fun file(
        mimeType: String,
        standardDirectory: StandardDirectory,
        appDirectoryName: String,
        name: String
    ) = if (legacy) {
        LegacyStorageFile(
            contentResolver = contentResolver,
            mimeType = mimeType,
            standardDirectory = standardDirectory,
            appDirectoryName = appDirectoryName,
            name = name
        )
    } else {
        ScopedStorageFile(
            contentResolver = contentResolver,
            mimeType = mimeType,
            standardDirectory = standardDirectory,
            appDirectoryName = appDirectoryName,
            name = name
        )
    }

    val legacy
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageLegacy()
}
