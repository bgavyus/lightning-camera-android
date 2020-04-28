package io.github.bgavyus.splash.detection

abstract class Detector(private val listener: DetectionListener) :
    AutoCloseable {
    private var lastDetected = false

    private fun propagate(detected: Boolean) {
        if (detected == lastDetected) {
            return
        }

        lastDetected = detected

        if (detected) {
            listener.onSubjectEntered()
        } else {
            listener.onSubjectLeft()
        }
    }

    abstract fun detected(): Boolean

    fun detect() {
        propagate(detected())
    }
}
