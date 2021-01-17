package io.github.bgavyus.lightningcamera.extensions

import java.nio.ByteBuffer

fun ByteBuffer.copyFrom(other: ByteBuffer) {
    position(other.position())
    limit(other.limit())
    put(other)
}
