package io.github.bgavyus.lightningcamera.ui.components

import android.graphics.Bitmap
import android.net.Uri
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
import io.github.bgavyus.lightningcamera.extensions.java.time.format
import io.github.bgavyus.lightningcamera.storage.MediaMetadata
import io.github.bgavyus.lightningcamera.storage.MediaMetadataProvider
import java.time.Duration

@Composable
fun MediaCell(
    metadata: MediaMetadata,
    metadataProvider: MediaMetadataProvider,
    uriHandler: UriHandler,
) {
    var size by remember { mutableStateOf(null as Size?) }
    val thumbnail by metadataProvider.thumbnailState(metadata.uri, size)
    val duration by metadataProvider.durationState(metadata.uri)
    MediaCell(metadata, thumbnail, duration, uriHandler) { size = it }
}

@Composable
private fun MediaCell(
    metadata: MediaMetadata,
    thumbnail: Bitmap?,
    duration: Duration?,
    uriHandler: UriHandler,
    onSizeChange: (Size) -> Unit,
) {
    Column(
        Modifier
            .padding(2.dp)
            .onGloballyPositioned { onSizeChange(it.androidSize) }
            .clickable { uriHandler.openUri(metadata.uri) }
    ) {
        SquareImage(thumbnail)
        Text(metadata.dateAdded.toString())
        Text(duration?.format() ?: "n/a")
    }
}

@Composable
private fun MediaMetadataProvider.thumbnailState(
    uri: Uri,
    size: Size?,
) = produceState(null as Bitmap?, this, uri, size) {
    value = size?.let { thumbnail(uri, it) }
}

@Composable
private fun MediaMetadataProvider.durationState(
    uri: Uri,
) = produceState(null as Duration?, this, uri) {
    value = duration(uri)
}
