package io.github.bgavyus.lightningcamera.storage

interface StorageFileFactory {
    fun create(
        mimeType: String,
        appDirectoryName: String,
        name: String,
    ): StorageFile
}
