package io.github.bgavyus.lightningcamera.ui.activities

import androidx.activity.result.ActivityResultCaller
import androidx.fragment.app.FragmentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides
    fun provideActivityResultCaller(activity: FragmentActivity): ActivityResultCaller = activity
}
