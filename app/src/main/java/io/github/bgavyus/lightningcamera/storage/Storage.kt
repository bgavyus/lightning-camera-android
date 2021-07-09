package io.github.bgavyus.lightningcamera.storage

import android.content.Context
import android.media.MediaFormat
import io.github.bgavyus.lightningcamera.R
import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class Storage @Inject constructor(
    private val context: Context,
    private val clock: Clock,
    private val fileFactory: StorageFileFactory,
) {
    fun generateFile(): StorageFile {
        val namePrefix = context.getString(R.string.file_name_prefix)

        val timeString = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")
            .withZone(clock.zone)
            .format(clock.instant())

        return fileFactory.create(
            mimeType = mimeType,
            appDirectoryName = context.getString(R.string.app_directory_name),
            name = "${namePrefix}_$timeString.$fileExtension",
        )
    }

    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val fileExtension = "mp4"
    }
}
