package io.github.bgavyus.lightningcamera.storage

import java.time.Instant

data class MediaItem(
    val id: Int,
    val title: String,
    val dateAdded: Instant,
)
