package io.github.bgavyus.lightningcamera.activities.viewfinder

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.bgavyus.lightningcamera.capture.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.capture.CameraMetadataProvider
import io.github.bgavyus.lightningcamera.capture.CameraSessionFactory
import io.github.bgavyus.lightningcamera.detection.MotionDetectorFactory
import io.github.bgavyus.lightningcamera.extensions.android.graphics.setDefaultBufferSize
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.and
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.graphics.SurfaceDuplicatorFactory
import io.github.bgavyus.lightningcamera.graphics.TransformMatrixFactory
import io.github.bgavyus.lightningcamera.hardware.Display
import io.github.bgavyus.lightningcamera.media.EncoderFactory
import io.github.bgavyus.lightningcamera.media.RecorderFactory
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Degrees
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ViewfinderViewModel @Inject constructor(
    private val motionDetectorFactory: MotionDetectorFactory,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val surfaceDuplicatorFactory: SurfaceDuplicatorFactory,
    private val encoderFactory: EncoderFactory,
    private val recorderFactory: RecorderFactory,
    private val cameraConnectionFactory: CameraConnectionFactory,
    private val cameraSessionFactory: CameraSessionFactory,
    private val display: Display,
) : ViewModel() {
    private val deferScope = DeferScope()

    private val activeDeferScope = DeferScope()
        .apply { deferScope.defer(::close) }

    private val displayRotation = MutableStateFlow(Degrees(0))
    val active = MutableStateFlow(false)
    val detecting = MutableStateFlow(false)
    val viewSize = MutableStateFlow(Size(1, 1))
    val watching = MutableStateFlow(false)
    val transformMatrix = MutableStateFlow(Matrix())
    val surfaceTexture = MutableStateFlow(null as SurfaceTexture?)
    private val recording = (active and watching and detecting).distinctUntilChanged()

    private val recorderOrientation = displayRotation
        .map { deferredMetadata.await().orientation - it }

    private val deferredMetadata = viewModelScope.async(Dispatchers.IO) {
        cameraMetadataProvider.collect()
    }

    private val deferredDetector = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        motionDetectorFactory.create(metadata.frameSize)
            .apply { deferScope.defer(::close) }
    }

    private val deferredDuplicator = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()
        val detector = deferredDetector.await()

        val surfaceTexture = surfaceTexture.filterNotNull().first()
            .apply { deferScope.defer(::release) }

        val surfaces = listOf(detector.surface, Surface(surfaceTexture))

        surfaceDuplicatorFactory.create(metadata.frameSize, surfaces)
            .apply { defer(::close) }
    }

    private val deferredEncoder = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        encoderFactory.create(metadata.frameSize, metadata.frameRate)
            .apply { deferScope.defer(::close) }
    }

    init {
        bind()
    }

    private fun bind() = viewModelScope.launch {
        val metadata = deferredMetadata.await()
        val detector = deferredDetector.await()

        launchAll(
            active.onEach(::activeChanged),
            detector.detectingStates().reflectTo(detecting),

            combine(viewSize, displayRotation) { viewSize, displayRotation ->
                TransformMatrixFactory.create(displayRotation, metadata.frameSize, viewSize)
            }
                .reflectTo(transformMatrix),
        )
    }

    private suspend fun activeChanged(active: Boolean) {
        activeDeferScope.close()

        if (active) {
            activate()
        }
    }

    private suspend fun activate() {
        val encoder = deferredEncoder.await()
        val metadata = deferredMetadata.await()

        recorderFactory.create(
            encoder,
            metadata.frameSize,
            metadata.frameRate,
            recorderOrientation,
            recording,
        )
            .apply { activeDeferScope.defer(::close) }

        val cameraDevice = cameraConnectionFactory.open(metadata.id)
            .apply { activeDeferScope.defer(::close) }

        val duplicator = deferredDuplicator.await()
        val surfaces = listOf(encoder.surface, duplicator.surface)

        cameraSessionFactory.create(cameraDevice, surfaces, metadata.frameRate)
            .apply { activeDeferScope.defer(::close) }

        val coroutineScope = CoroutineScope(Dispatchers.IO)
            .apply { activeDeferScope.defer(::cancel) }

        display.rotations()
            .reflectTo(displayRotation)
            .launchIn(coroutineScope)
    }

    suspend fun adjustBufferSize() {
        val metadata = deferredMetadata.await()
        surfaceTexture.value?.setDefaultBufferSize(metadata.frameSize)
    }

    override fun onCleared() = deferScope.close()
}
