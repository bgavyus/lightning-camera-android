package io.github.bgavyus.splash.fs

import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

enum class StandardDirectory(val value: String, val externalStorage: Uri) {
    Music(Environment.DIRECTORY_MUSIC, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
    Podcasts(Environment.DIRECTORY_PODCASTS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
    Ringtones(Environment.DIRECTORY_RINGTONES, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
    Alarms(Environment.DIRECTORY_ALARMS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
    Notifications(Environment.DIRECTORY_NOTIFICATIONS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
    Pictures(Environment.DIRECTORY_PICTURES, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
    Movies(Environment.DIRECTORY_MOVIES, MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
    Dcim(Environment.DIRECTORY_DCIM, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
}
