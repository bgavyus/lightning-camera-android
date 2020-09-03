package io.github.bgavyus.splash.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.view.WindowManager
import io.github.bgavyus.splash.common.extensions.systemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class Display(private val context: Context) : DeferScope() {
    companion object {
        private val TAG = Display::class.simpleName
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    fun rotations(): Flow<Rotation> {
        val sensorManager = context.systemService(SensorManager::class)
        val defaultDisplay = context.systemService(WindowManager::class).defaultDisplay

        return sensorManager.samples(
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            samplingPeriodUs = SensorManager.SENSOR_DELAY_UI,
            maxReportLatencyUs = 100000,
            handler = handler
        )
            .map { defaultDisplay.rotation }
            .distinctUntilChanged()
            .map { Rotation.fromSurfaceRotation(it) }
    }
}

private fun SensorManager.samples(
    sensor: Sensor,
    samplingPeriodUs: Int,
    maxReportLatencyUs: Int,
    handler: Handler
) = callbackFlow<SensorEvent> {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) = sendBlocking(event)
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    registerListener(listener, sensor, samplingPeriodUs, maxReportLatencyUs, handler)
    awaitClose { unregisterListener(listener) }
}
