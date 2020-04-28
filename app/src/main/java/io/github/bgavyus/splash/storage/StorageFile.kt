package io.github.bgavyus.splash.storage

open class StorageFile(
    mimeType: String,
    standardDirectory: StandardDirectory,
    appDirectoryName: String,
    name: String
) : File {
    private val file = if (Storage.scoped) {
        TentativeScopedStorageFile(mimeType, standardDirectory, appDirectoryName, name)
    } else {
        TentativeLegacyStorageFile(mimeType, standardDirectory, appDirectoryName, name)
    }

    override val descriptor get() = file.descriptor
    override val path get() = file.path

    internal open fun save() {
        file.save()
    }

    internal open fun discard() {
        file.discard()
    }

    var valid: Boolean = false

    override fun close() {
        file.close()

        if (valid) {
            save()
        } else {
            discard()
        }
    }
}
