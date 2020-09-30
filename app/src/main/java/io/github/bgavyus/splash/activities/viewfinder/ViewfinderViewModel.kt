package io.github.bgavyus.splash.activities.viewfinder

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.renderscript.RenderScript
import android.util.Size
import android.view.Surface
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.splash.capture.CameraConnectionFactory
import io.github.bgavyus.splash.capture.CameraMetadata
import io.github.bgavyus.splash.capture.CameraMetadataProvider
import io.github.bgavyus.splash.capture.CameraSessionFactory
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Display
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.extensions.and
import io.github.bgavyus.splash.common.extensions.launchAll
import io.github.bgavyus.splash.common.extensions.onToggle
import io.github.bgavyus.splash.common.extensions.reflectTo
import io.github.bgavyus.splash.graphics.SurfaceDuplicator
import io.github.bgavyus.splash.graphics.TransformMatrixFactory
import io.github.bgavyus.splash.graphics.detection.LightningDetector
import io.github.bgavyus.splash.graphics.media.Beeper
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.permissions.PermissionMissingException
import io.github.bgavyus.splash.permissions.PermissionsManager
import io.github.bgavyus.splash.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ViewfinderViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val permissionsManager: PermissionsManager,
    private val cameraMetadataProvider: CameraMetadataProvider,
    private val renderScript: RenderScript,
    private val storage: Storage,
    private val beeper: Beeper
) : ViewModel() {
    private val deferScope = DeferScope()

    private val activeDeferScope = DeferScope()
        .apply { deferScope.defer(::close) }

    private val activeCoroutineScope = CoroutineScope(viewModelScope.coroutineContext)

    private val display = Display(context)
        .apply { deferScope.defer(::close) }

    private val cameraConnectionFactory = CameraConnectionFactory(context)
        .apply { deferScope.defer(::close) }

    private val cameraSessionFactory = CameraSessionFactory()
        .apply { deferScope.defer(::close) }

    private var cameraMetadata: CameraMetadata? = null

    private val displayRotation = MutableStateFlow(Rotation.Natural)
    val active = MutableStateFlow(false)
    val detecting = MutableStateFlow(false)
    val viewSize = MutableStateFlow(Size(1, 1))
    val watching = MutableStateFlow(false)
    val transformMatrix = MutableStateFlow(Matrix())
    val surfaceTexture = MutableStateFlow(null as SurfaceTexture?)
    val lastException = MutableStateFlow(null as Exception?)

    private val deferredMetadata = viewModelScope.async {
        cameraMetadataProvider.highSpeed()
            .also {
                cameraMetadata = it
                Logger.debug("Stream configurations: ${it.streamConfigurations}")
            }
    }

    private val deferredDetector = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        LightningDetector(renderScript, metadata.frameSize)
            .apply { deferScope.defer(::close) }
    }

    private val deferredDuplicator = viewModelScope.async {
        val metadata = deferredMetadata.await()
        val detector = deferredDetector.await()

        val surfaceTexture = surfaceTexture.filterNotNull().first()
            .apply { deferScope.defer(::release) }

        SurfaceDuplicator().apply {
            deferScope.defer(::close)
            addSurface(detector.surface)
            addSurface(Surface(surfaceTexture))
            start()
            setBufferSize(metadata.frameSize)
        }
    }

    private val deferredRecorder = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        Recorder(storage, metadata.frameSize, metadata.framesPerSecond)
            .apply { deferScope.defer(::close) }
    }

    init {
        bind()
    }

    private fun bind() = viewModelScope.launch {
        val metadata = deferredMetadata.await()
        val recorder = deferredRecorder.await()
        val detector = deferredDetector.await()

        launchAll(
            active.onToggle(on = ::activate, off = ::deactivate),
            detector.detectingStates().reflectTo(detecting),
            (active and detecting).onToggle(on = beeper::start, off = beeper::stop),
            recorder.lastException.reflectTo(lastException),

            displayRotation
                .onEach { metadata.orientation - it }
                .reflectTo(recorder.rotation),

            (active and detecting and watching)
                .onToggle(on = recorder::record, off = recorder::lose),

            combine(viewSize, displayRotation) { viewSize, displayRotation ->
                TransformMatrixFactory.create(viewSize, metadata.frameSize, displayRotation)
            }
                .reflectTo(transformMatrix)
        )
    }

    suspend fun grantPermissions() = try {
        permissionsManager.grantAll()
    } catch (exception: PermissionMissingException) {
        lastException.value = exception
    }

    private fun activate() = activeCoroutineScope.launch {
        try {
            display.rotations()
                .reflectTo(displayRotation)
                .launchIn(activeCoroutineScope)

            val recorder = deferredRecorder.await().apply {
                activeDeferScope.defer(::stop)
                start()
            }

            val metadata = deferredMetadata.await()

            val device = cameraConnectionFactory.open(metadata.id)
                .apply { activeDeferScope.defer(::close) }

            val duplicator = deferredDuplicator.await()
            val surfaces = listOf(recorder.surface, duplicator.surface)

            cameraSessionFactory.create(device, surfaces, metadata.framesPerSecond)
                .apply { activeDeferScope.defer(::close) }
        } catch (exception: Exception) {
            Logger.error("Failed to activate", exception)
            lastException.value = exception
        }
    }

    private fun deactivate() = activeDeferScope.close()

    fun adjustBufferSize() {
        val size = cameraMetadata?.frameSize ?: return
        surfaceTexture.value?.setDefaultBufferSize(size.width, size.height)
    }

    override fun onCleared() = deferScope.close()
}
