package io.github.bgavyus.splash.common

import android.app.Application
import android.content.res.Configuration
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import java.util.*

class App : Application() {
    companion object {
        lateinit var context: App

        private val windowManager
            get() = context.getSystemService(WindowManager::class.java)
                ?: throw RuntimeException("Failed to get window manager service")

        val deviceOrientation: Rotation
            get() = -Rotation.fromSurfaceRotation(windowManager.defaultDisplay.rotation)

        fun getDefaultString(resourceId: Int): String {
            val config = Configuration().apply { setLocale(Locale.ROOT) }
            return context.createConfigurationContext(config).getString(resourceId)
        }

        fun showMessage(resourceId: Int) {
            Thread {
                Looper.prepare()
                Toast.makeText(context, resourceId, Toast.LENGTH_LONG).run {
                    setGravity(Gravity.CENTER, 0, 0)
                    show()
                }
                Looper.loop()
            }.start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
