package io.github.bgavyus.lightningcamera.ui.activities.gallery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.bgavyus.lightningcamera.storage.MediaProvider
import io.github.bgavyus.lightningcamera.storage.ThumbnailsProvider
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import javax.inject.Inject

@HiltViewModel
class GalleryModel @Inject constructor(
    private val mediaProvider: MediaProvider,
) : ViewModel() {
    private val deferScope = DeferScope()

    val thumbnailsProvider: ThumbnailsProvider
        get() = mediaProvider

    val mediaList = mediaProvider.list()
        .also { deferScope.defer(it::close) }

    override fun onCleared() {
        deferScope.close()
    }
}
