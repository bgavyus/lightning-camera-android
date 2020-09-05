package io.github.bgavyus.splash.activities.viewfinder

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.renderscript.RenderScript
import android.util.Size
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.splash.capture.CameraConnection
import io.github.bgavyus.splash.capture.CameraMetadata
import io.github.bgavyus.splash.capture.CameraMetadataProvider
import io.github.bgavyus.splash.capture.CameraSession
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Display
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.extensions.launchAll
import io.github.bgavyus.splash.common.extensions.onToggle
import io.github.bgavyus.splash.common.extensions.reflectTo
import io.github.bgavyus.splash.graphics.ImageConsumerDuplicator
import io.github.bgavyus.splash.graphics.TextureHolder
import io.github.bgavyus.splash.graphics.detection.LightningDetector
import io.github.bgavyus.splash.graphics.media.Beeper
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.permissions.PermissionMissingException
import io.github.bgavyus.splash.permissions.PermissionsManager
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.StorageFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

    private var cameraMetadata: CameraMetadata? = null

    val active = MutableStateFlow(false)
    val detecting = MutableStateFlow(false)
    val viewSize = MutableStateFlow(Size(1, 1))
    val watching = MutableStateFlow(false)
    val transformMatrix = MutableStateFlow(Matrix())
    val surfaceTexture = MutableStateFlow(null as SurfaceTexture?)
    val file = MutableStateFlow(null as StorageFile?)
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

    private val deferredHolder = viewModelScope.async {
        val metadata = deferredMetadata.await()
        val surfaceTexture = surfaceTexture.filterNotNull().first()
            .apply { deferScope.defer(::release) }

        TextureHolder(surfaceTexture).apply {
            deferScope.defer(::close)
            bufferSize.value = metadata.frameSize
        }
    }

    private val deferredDuplicator = viewModelScope.async {
        val metadata = deferredMetadata.await()
        val detector = deferredDetector.await()
        val holder = deferredHolder.await()

        ImageConsumerDuplicator().apply {
            deferScope.defer(::close)
            addConsumer(detector)
            addConsumer(holder)
            start()
            setBufferSize(metadata.frameSize)
        }
    }

    private val deferredRecorder = viewModelScope.async(Dispatchers.IO) {
        val metadata = deferredMetadata.await()

        Recorder(metadata.frameSize, metadata.framesPerSecond)
            .apply { deferScope.defer(::close) }
    }

    init {
        activeDeferScope.defer(::closeFile)
        bind()
    }

    private fun bind() = viewModelScope.launch {
        val holder = deferredHolder.await()
        val recorder = deferredRecorder.await()
        val detector = deferredDetector.await()

        launchAll(
            active.onToggle(on = ::activate, off = ::deactivate),
            detector.detectingStates().reflectTo(detecting),
            detecting.onToggle(on = beeper::start, off = beeper::stop),
            watching.combine(detecting) { a, b -> a && b }
                .onToggle(on = recorder::record, off = recorder::loss),
            file.reflectTo(recorder.file),
            viewSize.reflectTo(holder.viewSize),
            holder.transformMatrix.reflectTo(transformMatrix)
        )
    }

    suspend fun grantPermissions() = try {
        permissionsManager.grantAll()
    } catch (exception: PermissionMissingException) {
        lastException.value = exception
    }

    private fun activate() = activeCoroutineScope.launch {
        val metadata = deferredMetadata.await()
        val holder = deferredHolder.await()
        val recorder = deferredRecorder.await()

        try {
            Display(context).apply {
                activeDeferScope.defer(::close)

                rotations().onEach {
                    recorder.rotation = metadata.orientation - it
                    holder.rotation.value = it
                    regenerateFile()
                }
                    .launchIn(activeCoroutineScope)
            }

            val connection = CameraConnection(context, metadata.id).apply {
                activeDeferScope.defer(::close)
                open()
            }

            val duplicator = deferredDuplicator.await()

            CameraSession(
                metadata.framesPerSecond,
                connection,
                listOf(recorder, duplicator)
            ).apply {
                activeDeferScope.defer(::close)
                open()
            }
        } catch (exception: Exception) {
            Logger.error("Failed to activate", exception)
            lastException.value = exception
        }
    }

    private suspend fun regenerateFile() = withContext(Dispatchers.IO) {
        closeFile()
        file.value = storage.generateFile()
    }

    private fun deactivate() = activeDeferScope.close()

    private fun closeFile() {
        file.value?.close()
        file.value = null
    }

    fun adjustBufferSize() {
        val size = cameraMetadata?.frameSize ?: return
        surfaceTexture.value?.setDefaultBufferSize(size.width, size.height)
    }

    override fun onCleared() = deferScope.close()
}
