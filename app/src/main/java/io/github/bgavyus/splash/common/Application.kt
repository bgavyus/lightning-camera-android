package io.github.bgavyus.splash.common

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.splash.storage.Storage
import javax.inject.Inject

@HiltAndroidApp
class Application : Application() {
    companion object {
        private val TAG = Application::class.simpleName
    }

    @Inject
    lateinit var storage: Storage

    override fun onCreate() {
        super.onCreate()
        logState()
    }

    private fun logState() {
        Log.i(TAG, "Storage: ${if (storage.legacy) "legacy" else "scoped"}")
    }
}
