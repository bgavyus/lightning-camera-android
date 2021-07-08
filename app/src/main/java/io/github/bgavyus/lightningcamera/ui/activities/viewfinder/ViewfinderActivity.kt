package io.github.bgavyus.lightningcamera.ui.activities.viewfinder

import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.descendants
import androidx.core.view.updateMargins
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenCreated
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.databinding.ActivityViewfinderBinding
import io.github.bgavyus.lightningcamera.extensions.android.app.displayCompat
import io.github.bgavyus.lightningcamera.extensions.android.content.res.identifier
import io.github.bgavyus.lightningcamera.extensions.android.content.systemService
import io.github.bgavyus.lightningcamera.extensions.android.hardware.display.metricsChanges
import io.github.bgavyus.lightningcamera.extensions.android.view.*
import io.github.bgavyus.lightningcamera.extensions.android.widget.checked
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.onEachChange
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.hardware.camera.CameraConnectionFactory
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.permissions.PermissionsRequester
import io.github.bgavyus.lightningcamera.storage.StorageCharacteristics
import io.github.bgavyus.lightningcamera.ui.MessageShower
import io.github.bgavyus.lightningcamera.utilities.Rotation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ViewfinderActivity : FragmentActivity() {
    @Inject
    lateinit var permissionsRequester: PermissionsRequester

    @Inject
    lateinit var messageShower: MessageShower

    private val model: ViewfinderModel by viewModels()
    private val binding by lazy { ActivityViewfinderBinding.inflate(layoutInflater) }

    private val fixedPositionViews by lazy {
        binding.root.descendants.filter { it.getTag(R.id.fixed_position) != null }
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

        window.setDecorFitsSystemWindowsCompat(false)
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
        model.surfaceTexture.value?.let(binding.texture::setSurfaceTexture)

        lifecycleScope.launchAll(
            binding.texture.surfaceTextureEvents().onEach(::handleSurfaceTextureEvent),
            model.transformMatrix.onEach(binding.texture::setTransform),
            model.detecting.onEach(::setDetectionIndicatorActive),
            model.watching.onEach(::setWatchingToggleActive),
            combine(model.watching, model.detecting.debounce { if (it) 0 else 750 }, ::updateHint),

            binding.watchToggle.checked()
                .onEach { Logger.log("Watching? $it") }
                .reflectTo(model.watching),

            model.displayRotation
                .onEach { Logger.log("Display rotation: $it") }
                .onEachChange { old, new -> rotateFixedPositionViews(old - new) },
        )
    }

    private suspend fun bindDisplayRotation() {
        systemService<DisplayManager>()
            .metricsChanges()
            .collect { updateDisplayRotation() }
    }

    private fun rotateFixedPositionViews(rotation: Rotation) {
        fixedPositionViews.forEach { it.rotateLayout(rotation) }
    }

    private fun handleSurfaceTextureEvent(event: SurfaceTextureEvent) = when (event) {
        is SurfaceTextureEvent.Available -> model.surfaceTexture.value = event.surface
        is SurfaceTextureEvent.SizeChanged -> model.viewSize.value = event.size
        SurfaceTextureEvent.Updated -> model.adjustBufferSize()
    }

    override fun onAttachedToWindow() {
        updateVisibleAreaMargins()
    }

    private fun updateVisibleAreaMargins() {
        val types = WindowInsetsCompat.Type.displayCutout() + WindowInsetsCompat.Type.systemBars()
        val insets = window.decorView.rootWindowInsetsCompat.getInsets(types)

        val longEdgePaddingResourceId =
            resources.identifier("android:dimen/status_bar_height_landscape")
                ?: resources.identifier("android:dimen/status_bar_height")
                ?: throw NoSuchElementException()

        val longEdgePadding = resources.getDimensionPixelSize(longEdgePaddingResourceId)

        binding.visibleArea.updateConstraintLayoutParams {
            updateMargins(
                left = insets.left + longEdgePadding,
                top = insets.top,
                right = insets.right + longEdgePadding,
                bottom = insets.bottom,
            )
        }
    }

    override fun onResume() {
        Logger.log("Resumed")
        super.onResume()
        model.active.value = true
    }

    override fun onPause() {
        Logger.log("Paused")
        super.onPause()
        model.active.value = false
    }

    private fun setDetectionIndicatorActive(active: Boolean) {
        binding.detectionIndicator.isEnabled = active
    }

    private fun setWatchingToggleActive(active: Boolean) {
        binding.watchToggle.isChecked = active
    }

    private fun updateHint(watching: Boolean, detecting: Boolean) {
        binding.hint.text = getText(hintText(watching, detecting))
    }

    @StringRes
    private fun hintText(watching: Boolean, detecting: Boolean) = when {
        watching && detecting -> R.string.watching_detecting_hint
        watching && !detecting -> R.string.watching_not_detecting_hint
        !watching && detecting -> R.string.not_watching_detecting_hint
        else -> R.string.not_watching_not_detecting_hint
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDisplayRotation()
    }

    private fun updateDisplayRotation() {
        val display = displayCompat ?: return
        model.displayRotation.value = Rotation.fromSurfaceRotation(display.rotation)
    }

    companion object {
        private val requiredPermissions =
            CameraConnectionFactory.permissions + StorageCharacteristics.permissions
    }
}
