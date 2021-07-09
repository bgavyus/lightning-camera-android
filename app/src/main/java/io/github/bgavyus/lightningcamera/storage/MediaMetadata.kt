package io.github.bgavyus.lightningcamera.storage

import java.time.Instant

data class MediaMetadata(
    val id: Int,
    val title: String,
    val dateAdded: Instant,
)
