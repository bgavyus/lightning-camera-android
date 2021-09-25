package io.github.bgavyus.lightningcamera.storage

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState

fun interface ThumbnailsProvider {
    suspend fun thumbnail(uri: Uri, size: Size): Bitmap
}

@Composable
fun ThumbnailsProvider.thumbnailState(
    uri: Uri,
    size: Size?,
) = produceState(null as Bitmap?, this, uri, size) {
    value = size?.let { thumbnail(uri, it) }
}
