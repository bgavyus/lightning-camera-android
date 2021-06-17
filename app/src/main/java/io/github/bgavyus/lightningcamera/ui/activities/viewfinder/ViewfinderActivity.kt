package io.github.bgavyus.lightningcamera.ui.activities.viewfinder

import android.hardware.display.DisplayManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenCreated
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.databinding.ActivityViewfinderBinding
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.display.metricsChanges
import io.github.bgavyus.lightningcamera.extensions.android.view.SurfaceTextureEvent
import io.github.bgavyus.lightningcamera.extensions.android.view.rotateLayout
import io.github.bgavyus.lightningcamera.extensions.android.view.surfaceTextureEvents
import io.github.bgavyus.lightningcamera.extensions.android.widget.checked
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.hardware.camera.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.permissions.PermissionsRequester
import io.github.bgavyus.lightningcamera.storage.StorageCharacteristics
import io.github.bgavyus.lightningcamera.ui.MessageShower
import io.github.bgavyus.lightningcamera.utilities.Rotation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ViewfinderActivity : FragmentActivity() {
    companion object {
        private val requiredPermissions =
            CameraConnectionFactory.permissions + StorageCharacteristics.permissions
    }

    @Inject
    lateinit var permissionsRequester: PermissionsRequester

    @Inject
    lateinit var messageShower: MessageShower

    private val model: ViewfinderViewModel by viewModels()
    private val binding by lazy { ActivityViewfinderBinding.inflate(layoutInflater) }

    private val fixedPositionViews by lazy {
        listOf(
            binding.watchToggle,
        )
    }

    init {
        bindLifecycle()
    }

    private fun bindLifecycle() = lifecycleScope.launch {
        whenCreated { onCreated() }
        repeatOnLifecycle(Lifecycle.State.STARTED) { bindDisplayRotation() }
    }

    private suspend fun onCreated() {
        if (!permissionsRequester.requestMissing(requiredPermissions)) {
            messageShower.show(R.string.error_permission_not_granted)
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        disableWindowRotationAnimation()
        includeCutouts()
        bindViews()
    }

    private fun disableWindowRotationAnimation() {
        window.attributes.rotationAnimation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
        } else {
            WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE
        }
    }

    private fun includeCutouts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        window.attributes.layoutInDisplayCutoutMode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
    }

    private fun bindViews() {
        model.surfaceTexture.value?.let(binding.textureView::setSurfaceTexture)

        lifecycleScope.launchAll(
            binding.textureView.surfaceTextureEvents().onEach(::handleSurfaceTextureEvent),
            model.transformMatrix.onEach(binding.textureView::setTransform),
            model.detecting.onEach(::setDetectionIndicatorActive),

            binding.watchToggle.checked()
                .onEach { Logger.log("Watching? $it") }
                .reflectTo(model.watching),
        )
    }

    private suspend fun bindDisplayRotation() {
        systemService<DisplayManager>()
            .metricsChanges()
            .filter { it == binding.root.display.displayId }
            .map { Rotation.fromSurfaceRotation(binding.root.display.rotation) }
            .onEach { Logger.log("Rotation changed: $it") }
            .onEach { rotateFixedPositionViews(model.displayRotation.value - it) }
            .reflectTo(model.displayRotation)
            .collect()
    }

    private fun rotateFixedPositionViews(rotation: Rotation) {
        fixedPositionViews.forEach { it.rotateLayout(rotation) }
    }

    private fun handleSurfaceTextureEvent(event: SurfaceTextureEvent) = when (event) {
        is SurfaceTextureEvent.Available -> model.surfaceTexture.value = event.surface
        is SurfaceTextureEvent.SizeChanged -> model.viewSize.value = event.size
        SurfaceTextureEvent.Updated -> model.adjustBufferSize()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Logger.log("Screen in focus? $hasFocus")
        model.active.value = hasFocus
    }

    private fun setDetectionIndicatorActive(active: Boolean) {
        binding.detectionIndicator.isInvisible = !active
    }
}
