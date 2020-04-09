package io.github.bgavyus.splash.storage

import java.io.FileDescriptor

interface PendingFile : PendingOperation {
    val descriptor: FileDescriptor
}
