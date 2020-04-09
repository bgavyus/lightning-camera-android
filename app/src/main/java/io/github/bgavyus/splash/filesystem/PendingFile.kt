package io.github.bgavyus.splash.filesystem

import java.io.FileDescriptor

interface PendingFile : PendingOperation {
    val descriptor: FileDescriptor
}
