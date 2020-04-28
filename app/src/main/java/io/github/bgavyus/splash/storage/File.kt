package io.github.bgavyus.splash.storage

import java.io.FileDescriptor

interface File : AutoCloseable {
    val descriptor: FileDescriptor
    val path: String
}
