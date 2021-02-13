package io.github.bgavyus.lightningcamera.graphics.media

import android.util.Size
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.common.Hertz
import io.github.bgavyus.lightningcamera.storage.Storage
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
