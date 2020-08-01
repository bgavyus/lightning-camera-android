package io.github.bgavyus.splash.graphics.detection

import android.content.Context
import android.renderscript.RenderScript
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@InstallIn(ApplicationComponent::class)
@Module
object DetectionModule {
    @Provides
    fun provideRenderScript(@ApplicationContext context: Context) = RenderScript.create(context)
}
