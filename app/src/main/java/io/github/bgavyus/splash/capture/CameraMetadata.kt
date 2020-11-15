package io.github.bgavyus.splash.capture

import io.github.bgavyus.splash.common.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val streamConfigurations: Iterable<StreamConfiguration>
) {
    // TODO: Convert to flows
    private val activeConfig get() = streamConfigurations.elementAt(0)
    val framesPerSecond get() = activeConfig.framesPerSecond
    val frameSize get() = activeConfig.frameSize
}
