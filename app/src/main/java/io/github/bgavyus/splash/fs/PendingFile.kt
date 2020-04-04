package io.github.bgavyus.splash.fs

import java.io.FileDescriptor

interface PendingFile : PendingOperation {
    val descriptor: FileDescriptor
}
