package io.github.bgavyus.lightningcamera.storage

import android.net.Uri
import java.time.Duration
import java.time.Instant

data class MediaMetadata(
    val uri: Uri,
    val title: String,
    val dateAdded: Instant,
    val duration: Duration,
)
