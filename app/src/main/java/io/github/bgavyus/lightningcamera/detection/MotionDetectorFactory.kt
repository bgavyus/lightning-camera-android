package io.github.bgavyus.lightningcamera.detection

import android.content.Context
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import javax.inject.Inject

class MotionDetectorFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeferScope() {
    fun create(bufferSize: Size) = MotionDetector(context, bufferSize)
}
