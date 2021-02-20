package io.github.bgavyus.lightningcamera

import android.content.ContentResolver
import android.content.Context
import android.os.Handler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.bgavyus.lightningcamera.utilities.SingleThreadHandler
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Singleton
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Singleton
    @Provides
    fun provideContext(@ApplicationContext context: Context) = context

    @Singleton
    @Provides
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Singleton
    @Provides
    fun provideHandler(): Handler = SingleThreadHandler("Worker")
}
