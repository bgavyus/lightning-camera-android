package io.github.bgavyus.lightningcamera.ui.components

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.layout.androidSize
import io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.platform.openUri
import io.github.bgavyus.lightningcamera.storage.MediaMetadata
import io.github.bgavyus.lightningcamera.storage.ThumbnailsProvider
import io.github.bgavyus.lightningcamera.storage.thumbnailState

@Composable
fun MediaCell(
    metadata: MediaMetadata,
    thumbnailsProvider: ThumbnailsProvider,
    uriHandler: UriHandler,
) {
    var size by remember { mutableStateOf(null as Size?) }
    val thumbnail by thumbnailsProvider.thumbnailState(metadata.uri, size)
    MediaCell(metadata, thumbnail, uriHandler) { size = it }
}

@Composable
fun MediaCell(
    metadata: MediaMetadata,
    thumbnail: Bitmap?,
    uriHandler: UriHandler,
    onSizeChange: (Size) -> Unit,
) {
    Column(
        Modifier
            .padding(2.dp)
            .onGloballyPositioned { onSizeChange(it.androidSize) }
            .clickable { uriHandler.openUri(metadata.uri) }
    ) {
        MediaThumbnail(thumbnail)
        Text(metadata.dateAdded.toString())
        Text(metadata.duration.toString())
    }
}
