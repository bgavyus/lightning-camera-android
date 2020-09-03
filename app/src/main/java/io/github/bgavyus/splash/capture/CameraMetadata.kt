package io.github.bgavyus.splash.capture

import io.github.bgavyus.splash.common.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val streamConfigurations: Iterable<StreamConfiguration>
) {
    // TODO: Convert to flows
    val framesPerSecond get() = streamConfigurations.first().framesPerSecond
    val frameSize get() = streamConfigurations.first().frameSize
}
