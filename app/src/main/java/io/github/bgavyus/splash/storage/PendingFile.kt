package io.github.bgavyus.splash.storage

import io.github.bgavyus.splash.common.PendingOperation
import java.io.FileDescriptor

interface PendingFile :
    PendingOperation {
    val descriptor: FileDescriptor
}
