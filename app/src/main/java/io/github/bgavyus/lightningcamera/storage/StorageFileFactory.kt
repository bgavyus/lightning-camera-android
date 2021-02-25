package io.github.bgavyus.lightningcamera.storage

interface StorageFileFactory {
    fun create(
        mimeType: String,
        mediaDirectory: MediaDirectory,
        appDirectoryName: String,
        name: String,
    ): StorageFile
}
