package io.github.bgavyus.lightningcamera.extensions.android.hardware

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

@Suppress("BlockingMethodInNonBlockingContext")
fun SensorManager.samples(
    sensor: Sensor,
    samplingPeriodUs: Int,
    maxReportLatencyUs: Int = 0,
    handler: Handler? = null,
) = callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySendBlocking(event)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    check(registerListener(listener, sensor, samplingPeriodUs, maxReportLatencyUs, handler))
    awaitClose { unregisterListener(listener) }
}
