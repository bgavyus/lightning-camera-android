package io.github.bgavyus.lightningcamera.capture

import io.github.bgavyus.lightningcamera.common.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val streamConfigurations: List<StreamConfiguration>
) {
    private val activeStreamConfiguration = streamConfigurations.last()
    val framesPerSecond get() = activeStreamConfiguration.framesPerSecond
    val frameSize get() = activeStreamConfiguration.frameSize
}
