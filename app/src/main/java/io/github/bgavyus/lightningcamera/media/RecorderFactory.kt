package io.github.bgavyus.lightningcamera.media

import android.util.Size
import io.github.bgavyus.lightningcamera.storage.Storage
import io.github.bgavyus.lightningcamera.utilities.Degrees
import io.github.bgavyus.lightningcamera.utilities.Hertz
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecorderFactory @Inject constructor(
    private val storage: Storage,
) {
    fun create(
        encoder: Encoder,
        videoSize: Size,
        frameRate: Hertz,
        orientation: Flow<Degrees>,
        recording: Flow<Boolean>,
    ) = Recorder(storage, encoder, videoSize, frameRate, orientation, recording)
}
