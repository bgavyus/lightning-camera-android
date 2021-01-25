package io.github.bgavyus.lightningcamera.storage

interface StorageFileFactory {
    fun create(
        mimeType: String,
        standardDirectory: StandardDirectory,
        appDirectoryName: String,
        name: String,
    ): StorageFile
}
