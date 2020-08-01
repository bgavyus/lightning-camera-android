package io.github.bgavyus.splash.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Storage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun file(
        mimeType: String,
        standardDirectory: StandardDirectory,
        appDirectoryName: String,
        name: String
    ) = if (legacy) {
        LegacyStorageFile(
            contentResolver = context.contentResolver,
            mimeType = mimeType,
            standardDirectory = standardDirectory,
            appDirectoryName = appDirectoryName,
            name = name
        )
    } else {
        ScopedStorageFile(
            contentResolver = context.contentResolver,
            mimeType = mimeType,
            standardDirectory = standardDirectory,
            appDirectoryName = appDirectoryName,
            name = name
        )
    }

    val legacy
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageLegacy()
}
