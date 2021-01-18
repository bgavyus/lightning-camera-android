package io.github.bgavyus.lightningcamera.extensions.java.nio

import java.nio.ByteBuffer

fun ByteBuffer.copyFrom(other: ByteBuffer) {
    position(other.position())
    limit(other.limit())
    put(other)
}
