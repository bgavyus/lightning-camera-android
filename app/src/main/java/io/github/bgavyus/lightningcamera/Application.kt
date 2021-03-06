package io.github.bgavyus.lightningcamera

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.lightningcamera.utilities.validate
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        setupOpenCv()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    private fun setupOpenCv() = validate(OpenCVLoader.initDebug())

    private fun enableStrictMode() =
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
}
