package io.github.bgavyus.lightningcamera.activities.viewfinder

import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.databinding.ActivityViewfinderBinding
import io.github.bgavyus.lightningcamera.extensions.android.view.SurfaceTextureEvent
import io.github.bgavyus.lightningcamera.extensions.android.view.surfaceTextureEvents
import io.github.bgavyus.lightningcamera.extensions.android.widget.checked
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import io.github.bgavyus.lightningcamera.logging.Logger
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ViewfinderActivity : FragmentActivity() {
    private val model: ViewfinderViewModel by viewModels()
    private val binding by lazy { ActivityViewfinderBinding.inflate(layoutInflater) }

    init {
        lifecycleScope.launchWhenCreated { onCreated() }
    }

    private suspend fun onCreated() {
        if (!model.grantPermissions()) {
            model.showMessage(R.string.error_permission_not_granted)
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        bind()
    }

    private fun bind() {
        model.surfaceTexture.value?.let(binding.textureView::setSurfaceTexture)

        lifecycleScope.launchAll(
            binding.textureView.surfaceTextureEvents().onEach {
                when (it) {
                    is SurfaceTextureEvent.Available -> model.surfaceTexture.value = it.surface
                    is SurfaceTextureEvent.SizeChanged -> model.viewSize.value = it.size
                    SurfaceTextureEvent.Updated -> model.adjustBufferSize()
                }
            },

            model.transformMatrix.onEach(binding.textureView::setTransform),
            model.detecting.onEach(::setDetectionIndicatorActive),

            binding.watchToggle.checked()
                .onEach { Logger.log("Watching? $it") }
                .reflectTo(model.watching),
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Logger.log("Screen in focus? $hasFocus")
        model.active.value = hasFocus
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        -> {
            binding.watchToggle.toggle()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun setDetectionIndicatorActive(active: Boolean) {
        binding.detectionIndicator.isInvisible = !active
    }
}
