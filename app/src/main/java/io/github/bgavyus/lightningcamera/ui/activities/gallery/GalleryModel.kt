package io.github.bgavyus.lightningcamera.ui.activities.gallery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.bgavyus.lightningcamera.storage.MediaMetadataProvider
import javax.inject.Inject

@HiltViewModel
class GalleryModel @Inject constructor(
    val mediaProvider: MediaMetadataProvider,
) : ViewModel()
