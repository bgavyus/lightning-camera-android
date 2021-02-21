package io.github.bgavyus.lightningcamera.extensions.java.io

import io.github.bgavyus.lightningcamera.utilities.validate
import java.io.File

fun File.mkdirsIfNotExists() {
    if (!exists()) {
        requireMkdirs()
    }
}

fun File.requireMkdirs() = validate(mkdirs())

fun File.requireRenameTo(dest: File) = validate(renameTo(dest))
