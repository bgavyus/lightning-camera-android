package io.github.bgavyus.lightningcamera.storage

import io.github.bgavyus.lightningcamera.extensions.java.io.requireRenameTo
import java.io.File

class PendingFile(parent: File, child: String) : File(parent, child) {
    init {
        delete()
    }

    fun save() {
        requireRenameTo(this)
    }
}
