package io.github.bgavyus.lightningcamera.extensions.android.hardware

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import io.github.bgavyus.lightningcamera.common.validate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun SensorManager.samples(
    sensor: Sensor,
    samplingPeriodUs: Int,
    maxReportLatencyUs: Int,
    handler: Handler,
) = callbackFlow<SensorEvent> {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) = sendBlocking(event)
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    validate(registerListener(listener, sensor, samplingPeriodUs, maxReportLatencyUs, handler))
    awaitClose { unregisterListener(listener) }
}
