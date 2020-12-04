package io.github.bgavyus.lightningcamera.storage

import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

enum class StandardDirectory(val value: String, val externalStorageContentUri: Uri) {
    Movies(Environment.DIRECTORY_MOVIES, MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
}
