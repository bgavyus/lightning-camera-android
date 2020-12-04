package io.github.bgavyus.lightningcamera.storage

import java.io.FileDescriptor

interface StorageFile : AutoCloseable {
    val descriptor: FileDescriptor
    val path: String
    fun keep()
}
