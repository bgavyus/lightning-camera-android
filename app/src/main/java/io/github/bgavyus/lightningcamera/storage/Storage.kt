package io.github.bgavyus.lightningcamera.storage

import android.content.Context
import android.media.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.R
import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class Storage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val fileFactory: StorageFileFactory,
) {
    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val fileExtension = "mp4"
    }

    fun generateFile(): StorageFile {
        val timeString = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(clock.zone)
            .format(clock.instant())

        return fileFactory.create(
            mimeType = mimeType,
            standardDirectory = StandardDirectory.Movies,
            appDirectoryName = context.getString(R.string.video_folder_name),
            name = "VID_$timeString.$fileExtension",
        )
    }
}
