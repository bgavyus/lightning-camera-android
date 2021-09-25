package io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.layout

import android.util.Size
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.bgavyus.lightningcamera.extensions.androidx.compose.ui.unit.toAndroidSize

val LayoutCoordinates.androidSize: Size
    get() = size.toAndroidSize()
