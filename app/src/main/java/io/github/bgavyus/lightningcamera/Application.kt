package io.github.bgavyus.lightningcamera

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.lightningcamera.logging.Logger

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.log("Created")

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    private fun enableStrictMode() {
        val policy = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()

        StrictMode.setThreadPolicy(policy)
    }
}
