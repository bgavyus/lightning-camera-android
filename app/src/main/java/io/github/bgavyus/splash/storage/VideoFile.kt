package io.github.bgavyus.splash.storage

import android.media.MediaFormat
import android.util.Log
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.common.App
import java.text.SimpleDateFormat
import java.util.*

class VideoFile : StorageFile(
    MIME_TYPE,
    StandardDirectory.Movies,
    App.context.getString(R.string.video_folder_name),
    "VID_$currentTimeStamp.$FILE_EXTENSION"
) {
    companion object {
        private val TAG = VideoFile::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val FILE_EXTENSION = "mp4"

        private val currentTimeStamp get() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    override fun save() {
        Log.i(TAG, "Saving video")
        super.save()
    }

    override fun discard() {
        Log.i(TAG, "Discarding video")
        super.discard()
    }
}
