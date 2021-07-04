package io.github.bgavyus.lightningcamera.extensions.java.io

import java.io.File

fun File.mkdirsIfNotExists() {
    if (!exists()) {
        requireMkdirs()
    }
}

fun File.requireMkdirs() {
    check(mkdirs())
}

fun File.requireRenameTo(dest: File) {
    check(renameTo(dest))
}
