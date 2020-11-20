package io.github.bgavyus.splash.capture

import io.github.bgavyus.splash.common.Rotation

data class CameraMetadata(
    val id: String,
    val orientation: Rotation,
    val streamConfigurations: List<StreamConfiguration>
) {
    private val activeStreamConfiguration = streamConfigurations.middle()
    val framesPerSecond get() = activeStreamConfiguration.framesPerSecond
    val frameSize get() = activeStreamConfiguration.frameSize
}

private fun <T> List<T>.middle(): T = get(size / 2)
