package io.github.bgavyus.lightningcamera.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale

@Composable
fun MediaThumbnail(thumbnail: Bitmap?) {
    val painter = remember(thumbnail) {
        if (thumbnail != null) {
            BitmapPainter(thumbnail.asImageBitmap())
        } else {
            ColorPainter(Color.Black)
        }
    }

    Image(
        painter,
        null,
        Modifier.aspectRatio(1f),
        contentScale = ContentScale.Crop
    )
}
