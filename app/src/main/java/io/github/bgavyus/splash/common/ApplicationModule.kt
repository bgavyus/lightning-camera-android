package io.github.bgavyus.splash.common

import android.content.ContentResolver
import android.content.Context
import android.renderscript.RenderScript
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock

@InstallIn(ApplicationComponent::class)
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
}
