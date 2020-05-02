package io.github.bgavyus.splash.common

import android.content.res.Configuration
import android.os.Build
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import java.util.*

fun getDefaultString(resourceId: Int): String {
    val config = Configuration().apply { setLocale(Locale.ROOT) }
    return App.context.createConfigurationContext(config).getString(resourceId)
}

fun showMessage(resourceId: Int) {
    Thread {
        Looper.prepare()
        Toast.makeText(App.context, resourceId, Toast.LENGTH_LONG).run {
            setGravity(Gravity.CENTER, 0, 0)
            show()
        }
        Looper.loop()
    }.start()
}

fun floorMod(x: Int, y: Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Math.floorMod(x, y)
} else {
    floorModImpl(x, y)
}

private fun floorModImpl(x: Int, y: Int) = x - floorDiv(x, y) * y

fun floorDiv(x: Int, y: Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Math.floorDiv(x, y)
} else {
    floorDivImpl(x, y)
}

private fun floorDivImpl(x: Int, y: Int): Int {
    var r = x / y

    if (x xor y < 0 && r * y != x) {
        r--
    }

    return r
}
