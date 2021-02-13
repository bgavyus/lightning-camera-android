package io.github.bgavyus.lightningcamera.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.samples
import io.github.bgavyus.lightningcamera.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class Display @Inject constructor(
    @ApplicationContext private val context: Context,
    private val handler: Handler,
) {
    fun rotations(): Flow<Degrees> {
        val sensorManager = context.systemService<SensorManager>()

        val display = context.systemService<DisplayManager>().displays.first()

        return sensorManager.samples(
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            samplingPeriodUs = SensorManager.SENSOR_DELAY_UI,
            maxReportLatencyUs = 100_000,
            handler = handler,
        )
            .map { display.rotation }
            .distinctUntilChanged()
            .map(Degrees::fromSurfaceRotation)
            .onEach { Logger.log("Rotation changed: $it") }
    }
}
