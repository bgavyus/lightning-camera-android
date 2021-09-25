package io.github.bgavyus.lightningcamera.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = darkColors(
            surface = Color.Black,
        ),
        content = content
    )
}
