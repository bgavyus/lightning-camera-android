package io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.unit

import android.util.Size
import androidx.compose.ui.unit.IntSize

fun IntSize.toAndroidSize() = Size(width, height)
