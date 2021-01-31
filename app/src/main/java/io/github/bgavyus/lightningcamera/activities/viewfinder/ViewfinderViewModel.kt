package io.github.bgavyus.lightningcamera.activities.viewfinder

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.annotation.StringRes
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.capture.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.capture.CameraMetadataProvider
import io.github.bgavyus.lightningcamera.capture.CameraSessionFactory
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Degrees
import io.github.bgavyus.lightningcamera.common.Display
import io.github.bgavyus.lightningcamera.common.MessageShower
import io.github.bgavyus.lightningcamera.extensions.android.graphics.setDefaultBufferSize
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.and
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.graphics.SurfaceDuplicatorFactory
import io.github.bgavyus.lightningcamera.graphics.TransformMatrixFactory
import io.github.bgavyus.lightningcamera.graphics.detection.MotionDetector
import io.github.bgavyus.lightningcamera.graphics.media.Encoder
import io.github.bgavyus.lightningcamera.graphics.media.Recorder
import io.github.bgavyus.lightningcamera.permissions.PermissionsManager
import io.github.bgavyus.lightningcamera.storage.Storage
import io.github.bgavyus.lightningcamera.storage.StorageCharacteristics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ViewfinderViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val permissionsManager: PermissionsManager,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val storage: Storage,
    private val messageShower: MessageShower,
) : ViewModel() {
    private val deferScope = DeferScope()

    private val surfaceDuplicatorFactory = SurfaceDuplicatorFactory()
        .apply { deferScope.defer(::close) }

    private val activeDeferScope = DeferScope()
        .apply { deferScope.defer(::close) }

    private val display = Display(context)
        .apply { deferScope.defer(::close) }

    private val cameraConnectionFactory = CameraConnectionFactory(context)
        .apply { deferScope.defer(::close) }

    private val cameraSessionFactory = CameraSessionFactory()
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

    private val deferredMetadata = viewModelScope.async {
        cameraMetadataProvider.collect()
    }

    private val deferredDetector = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        MotionDetector(context, metadata.frameSize)
            .apply { deferScope.defer(::close) }
    }

    private val deferredDuplicator = viewModelScope.async {
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

        Encoder(metadata.frameSize, metadata.framesPerSecond)
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

    suspend fun grantPermissions(): Boolean {
        val permissions = CameraConnectionFactory.permissions + StorageCharacteristics.permissions
        return permissionsManager.requestMissing(permissions)
    }

    fun showMessage(@StringRes message: Int) = messageShower.show(message)

    private suspend fun activeChanged(active: Boolean) {
        activeDeferScope.close()

        if (active) {
            activate()
        }
    }

    private suspend fun activate() {
        val encoder = deferredEncoder.await()
        val metadata = deferredMetadata.await()

        Recorder(
            storage,
            encoder,
            metadata.frameSize,
            metadata.framesPerSecond,
            recorderOrientation,
            recording,
        )
            .apply { activeDeferScope.defer(::close) }

        val cameraDevice = cameraConnectionFactory.open(metadata.id)
            .apply { activeDeferScope.defer(::close) }

        val duplicator = deferredDuplicator.await()
        val surfaces = listOf(encoder.surface, duplicator.surface)

        cameraSessionFactory.create(cameraDevice, surfaces, metadata.framesPerSecond)
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
