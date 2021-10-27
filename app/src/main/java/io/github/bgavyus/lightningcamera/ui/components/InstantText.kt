package io.github.bgavyus.lightningcamera.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import java.time.Instant

@Composable
fun InstantText(instant: Instant) {
    Text(instant.toString())
}
