package io.github.bgavyus.lightningcamera.ui.activities.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.Surface
import androidx.compose.ui.platform.AndroidUriHandler
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.ui.components.MediaGrid
import io.github.bgavyus.lightningcamera.ui.theme.ApplicationTheme

@AndroidEntryPoint
class GalleryActivity : ComponentActivity() {
    private val model: GalleryModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriHandler = AndroidUriHandler(this)

        setContent {
            ApplicationTheme {
                Surface {
                    MediaGrid(model.mediaList, model.thumbnailsProvider, uriHandler)
                }
            }
        }
    }
}
