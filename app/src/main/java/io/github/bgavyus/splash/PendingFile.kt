package io.github.bgavyus.splash

import java.io.FileDescriptor

interface PendingFile {
    val descriptor: FileDescriptor
    fun save()
    fun discard()
}
