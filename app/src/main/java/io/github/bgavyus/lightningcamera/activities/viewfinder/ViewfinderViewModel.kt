package io.github.bgavyus.lightningcamera.activities.viewfinder

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
import io.github.bgavyus.lightningcamera.capture.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.capture.CameraMetadata
import io.github.bgavyus.lightningcamera.capture.CameraMetadataProvider
import io.github.bgavyus.lightningcamera.capture.CameraSessionFactory
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Display
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.common.extensions.and
import io.github.bgavyus.lightningcamera.common.extensions.launchAll
import io.github.bgavyus.lightningcamera.common.extensions.onToggle
import io.github.bgavyus.lightningcamera.common.extensions.reflectTo
import io.github.bgavyus.lightningcamera.graphics.SurfaceDuplicatorFactory
import io.github.bgavyus.lightningcamera.graphics.TransformMatrixFactory
import io.github.bgavyus.lightningcamera.graphics.detection.MotionDetector
import io.github.bgavyus.lightningcamera.graphics.media.Recorder
import io.github.bgavyus.lightningcamera.permissions.PermissionsManager
import io.github.bgavyus.lightningcamera.storage.Storage
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
) : ViewModel() {
    private val deferScope = DeferScope()

    private val surfaceDuplicatorManager = SurfaceDuplicatorFactory()
        .apply { deferScope.defer(::close) }

    private val activeDeferScope = DeferScope()
        .apply { deferScope.defer(::close) }

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

    private val deferredMetadata = viewModelScope.async {
        cameraMetadataProvider.collect()
            .also { cameraMetadata = it }
    }

    private val deferredDetector = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        MotionDetector(renderScript, metadata.frameSize)
            .apply { deferScope.defer(::close) }
    }

    private val deferredDuplicator = viewModelScope.async {
        val metadata = deferredMetadata.await()
        val detector = deferredDetector.await()

        val surfaceTexture = surfaceTexture.filterNotNull().first()
            .apply { deferScope.defer(::release) }

        val surfaces = listOf(detector.surface, Surface(surfaceTexture))

        surfaceDuplicatorManager.create(metadata.frameSize, surfaces)
            .apply { defer(::close) }
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

            displayRotation
                .map { metadata.orientation - it }
                .reflectTo(recorder.rotation),

            (active and detecting and watching)
                .onToggle(on = recorder::record, off = recorder::lose),

            combine(viewSize, displayRotation) { viewSize, displayRotation ->
                TransformMatrixFactory.create(viewSize, metadata.frameSize, displayRotation)
            }
                .reflectTo(transformMatrix),
        )
    }

    suspend fun grantPermissions() =
        permissionsManager.requestMissing(CameraConnectionFactory.permissions + Storage.permissions)

    private fun activate() = viewModelScope.launch {
        display.rotations()
            .reflectTo(displayRotation)
            .launchIn(this)

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
    }
        .apply { activeDeferScope.defer(::cancel) }

    private fun deactivate() = activeDeferScope.close()

    fun adjustBufferSize() {
        val size = cameraMetadata?.frameSize ?: return
        surfaceTexture.value?.setDefaultBufferSize(size.width, size.height)
    }

    override fun onCleared() = deferScope.close()
}
