package io.github.bgavyus.splash.common

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.splash.storage.Storage
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    @Inject
    lateinit var storage: Storage

    override fun onCreate() {
        super.onCreate()
        logState()
    }

    private fun logState() {
        Logger.info("Storage type: ${if (storage.isLegacy) "legacy" else "scoped"}")
    }
}
