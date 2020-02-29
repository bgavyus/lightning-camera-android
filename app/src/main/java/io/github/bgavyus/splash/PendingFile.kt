package io.github.bgavyus.splash

import java.io.FileDescriptor

interface PendingFile : PendingOperation {
    val descriptor: FileDescriptor
}
