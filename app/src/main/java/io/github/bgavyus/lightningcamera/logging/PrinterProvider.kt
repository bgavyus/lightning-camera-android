package io.github.bgavyus.lightningcamera.logging

import android.util.Printer
import io.github.bgavyus.lightningcamera.BuildConfig
import javax.inject.Provider

object PrinterProvider : Provider<Printer> {
    override fun get() = if (BuildConfig.DEBUG) LocalPrinter() else RemotePrinter()
}
