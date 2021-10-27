package io.github.bgavyus.lightningcamera.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.github.bgavyus.lightningcamera.extensions.java.time.format
import java.time.Duration

@Composable
fun DurationText(duration: Duration?) {
    Text(duration?.format() ?: "n/a")
}
