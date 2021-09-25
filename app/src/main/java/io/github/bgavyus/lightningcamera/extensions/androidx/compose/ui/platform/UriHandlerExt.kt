package io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.platform

import android.net.Uri
import androidx.compose.ui.platform.UriHandler

fun UriHandler.openUri(uri: Uri) {
    openUri(uri.toString())
}
