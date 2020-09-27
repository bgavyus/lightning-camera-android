package io.github.bgavyus.splash.common

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.splash.storage.Storage

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        logState()
    }

    private fun logState() {
        Logger.info("Storage type: ${if (Storage.isLegacy) "legacy" else "scoped"}")
    }
}
