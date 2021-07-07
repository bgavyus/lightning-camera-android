package io.github.bgavyus.lightningcamera.ui.activities.viewfinder

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.bgavyus.lightningcamera.detection.MotionDetectorFactory
import io.github.bgavyus.lightningcamera.extensions.android.graphics.setDefaultBufferSize
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.and
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.getCompletedOrNull
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.graphics.SurfaceDuplicatorFactory
import io.github.bgavyus.lightningcamera.graphics.TransformMatrixFactory
import io.github.bgavyus.lightningcamera.hardware.camera.CameraCaptureSessionFactory
import io.github.bgavyus.lightningcamera.hardware.camera.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.hardware.camera.CameraMetadataProvider
import io.github.bgavyus.lightningcamera.media.EncoderFactory
import io.github.bgavyus.lightningcamera.media.RecorderFactory
import io.github.bgavyus.lightningcamera.media.SamplesQueue
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Rotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ViewfinderModel @Inject constructor(
    private val motionDetectorFactory: MotionDetectorFactory,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val surfaceDuplicatorFactory: SurfaceDuplicatorFactory,
    private val encoderFactory: EncoderFactory,
    private val recorderFactory: RecorderFactory,
    private val cameraConnectionFactory: CameraConnectionFactory,
    private val cameraCaptureSessionFactory: CameraCaptureSessionFactory,
) : ViewModel() {
    private val deferScope = DeferScope()

    private val activeDeferScope = DeferScope()
        .apply { deferScope.defer(::close) }

    val displayRotation = MutableStateFlow(Rotation.Natural)
    val active = MutableStateFlow(false)
    val detecting = MutableStateFlow(false)
    val viewSize = MutableStateFlow(Size(1, 1))
    val watching = MutableStateFlow(false)
    val transformMatrix = MutableStateFlow(Matrix())
    val surfaceTexture = MutableStateFlow(null as SurfaceTexture?)

    private val recorderOrientation = displayRotation
        .map { deferredMetadata.await().orientation - it }

    private val deferredMetadata = viewModelScope.async(Dispatchers.IO) {
        cameraMetadataProvider.collect()
    }

    private val deferredDetector = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        motionDetectorFactory.create(metadata.frameSize, metadata.previewRate)
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

        encoderFactory.create(metadata.frameSize, metadata.captureRate)
            .apply { deferScope.defer(::close) }
    }

    private val deferredQueue = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()
        SamplesQueue(metadata.frameSize, metadata.captureRate)
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
                TransformMatrixFactory.create(displayRotation, metadata.frameSize, viewSize, true)
            }
                .reflectTo(transformMatrix),
        )
    }

    private suspend fun activeChanged(active: Boolean) = withContext(Dispatchers.IO) {
        activeDeferScope.close()
        watching.value = false

        if (active) {
            activate()
        }
    }

    private suspend fun activate() {
        val encoder = deferredEncoder.await()
        val metadata = deferredMetadata.await()
        val queue = deferredQueue.await()
        val detectionEndExtraMillis = if (metadata.captureRate.isHighSpeed) 50L else 500L

        val recording = (watching and detecting.debounce { if (it) 0 else detectionEndExtraMillis })
            .distinctUntilChanged()

        recorderFactory.create(
            encoder,
            queue,
            recording,
            recorderOrientation,
        )
            .apply { activeDeferScope.defer(::close) }

        val cameraDevice = cameraConnectionFactory.open(metadata.id)
            .apply { activeDeferScope.defer(::close) }

        val duplicator = deferredDuplicator.await()
        val surfaces = listOf(encoder.surface, duplicator.surface)

        cameraCaptureSessionFactory.create(cameraDevice, surfaces, metadata.captureRate)
            .apply { activeDeferScope.defer(::close) }
    }

    fun adjustBufferSize() {
        val metadata = deferredMetadata.getCompletedOrNull() ?: return
        surfaceTexture.value?.setDefaultBufferSize(metadata.frameSize)
    }

    override fun onCleared() {
        deferScope.close()
    }
}
