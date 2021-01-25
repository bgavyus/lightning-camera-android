package io.github.bgavyus.lightningcamera.common

import android.content.ContentResolver
import android.content.Context
import android.renderscript.RenderScript
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.bgavyus.lightningcamera.storage.LegacyStorageFileFactory
import io.github.bgavyus.lightningcamera.storage.ScopedStorageFileFactory
import io.github.bgavyus.lightningcamera.storage.StorageConfiguration
import io.github.bgavyus.lightningcamera.storage.StorageFileFactory
import java.time.Clock
import javax.inject.Provider

@InstallIn(SingletonComponent::class)
@Module
object ApplicationModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideRenderScript(@ApplicationContext context: Context): RenderScript =
        RenderScript.create(context)

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun provideStorageFileFactory(
        scopedStorageFileFactoryProvider: Provider<ScopedStorageFileFactory>,
        legacyStorageFileFactoryProvider: Provider<LegacyStorageFileFactory>,
    ): StorageFileFactory = if (StorageConfiguration.isScoped) {
        scopedStorageFileFactoryProvider.get()
    } else {
        legacyStorageFileFactoryProvider.get()
    }
}
