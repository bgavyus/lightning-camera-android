package io.github.bgavyus.splash.storage

open class StorageFile(
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String
) : File {
    private val file = if (Storage.legacy) {
        TentativeLegacyStorageFile(mimeType, standardDirectory, appDirectoryName, name)
    } else {
        TentativeScopedStorageFile(mimeType, standardDirectory, appDirectoryName, name)
    }

    override val descriptor get() = file.descriptor
    override val path get() = file.path

    protected open fun save() = file.save()
    protected open fun discard() = file.discard()

    var valid = false

    override fun close() {
        file.close()

        if (valid) {
            save()
        } else {
            discard()
        }
    }
}
