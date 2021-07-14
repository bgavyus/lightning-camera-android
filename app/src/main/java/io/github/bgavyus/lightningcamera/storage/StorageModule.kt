package io.github.bgavyus.lightningcamera.storage

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    fun provideStorageFileFactory(
        scopedStorageFileFactoryProvider: Provider<ScopedStorageFileFactory>,
        legacyStorageFileFactoryProvider: Provider<LegacyStorageFileFactory>,
    ): StorageFileFactory = if (StorageCharacteristics.isScoped) {
        scopedStorageFileFactoryProvider.get()
    } else {
        legacyStorageFileFactoryProvider.get()
    }

    @Provides
    fun provideMediaDirectory() = MediaDirectory.Movies

    @Provides
    fun provideUri(mediaDirectory: MediaDirectory) = mediaDirectory.externalStorageContentUri
}
