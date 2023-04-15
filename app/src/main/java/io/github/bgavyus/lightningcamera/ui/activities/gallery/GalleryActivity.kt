package io.github.bgavyus.lightningcamera.ui.activities.gallery

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.Surface
import androidx.compose.ui.platform.AndroidUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.ui.components.MediaGrid
import io.github.bgavyus.lightningcamera.ui.theme.ApplicationTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryActivity : ComponentActivity() {
    private val model: GalleryModel by viewModels()

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) { onCreated() }
        }
    }

    private suspend fun onCreated() {
        val metadataList = model.mediaProvider.list()
        val uriHandler = AndroidUriHandler(this)

        setContent {
            ApplicationTheme {
                Surface {
                    MediaGrid(metadataList, model.mediaProvider, uriHandler)
                }
            }
        }
    }
}
