package io.github.bgavyus.lightningcamera.activities.viewfinder

import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.databinding.ActivityViewfinderBinding
import io.github.bgavyus.lightningcamera.extensions.android.view.SurfaceTextureEvent
import io.github.bgavyus.lightningcamera.extensions.android.view.surfaceTextureEvents
import io.github.bgavyus.lightningcamera.extensions.android.widget.checked
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.launchAll
import io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines.reflectTo
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ViewfinderActivity : FragmentActivity() {
    private val viewModel: ViewfinderViewModel by viewModels()
    private lateinit var binding: ActivityViewfinderBinding

    init {
        lifecycleScope.launchWhenCreated { onCreated() }
    }

    private suspend fun onCreated() {
        enterFullScreen()
        inflateView()

        val permissionsGranted = grantPermissions()

        if (!permissionsGranted) {
            finishWithMessage(R.string.error_permission_not_granted)
            return
        }

        bind()
    }

    private fun enterFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun inflateView() {
        binding = ActivityViewfinderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private suspend fun grantPermissions() = viewModel.grantPermissions()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Logger.info("Screen in focus? $hasFocus")
        viewModel.active.value = hasFocus
    }

    private fun bind() {
        viewModel.surfaceTexture.value?.let(binding.textureView::setSurfaceTexture)

        lifecycleScope.launchAll(
            binding.textureView.surfaceTextureEvents().onEach {
                when (it) {
                    is SurfaceTextureEvent.Available -> viewModel.surfaceTexture.value = it.surface
                    is SurfaceTextureEvent.SizeChanged -> viewModel.viewSize.value = it.size
                    SurfaceTextureEvent.Updated -> viewModel.adjustBufferSize()
                }
            },

            viewModel.transformMatrix.onEach(binding.textureView::setTransform),
            viewModel.detecting.onEach(::setDetectionIndicatorActive),

            binding.watchToggle.checked()
                .onEach { Logger.info("Watching? $it") }
                .reflectTo(viewModel.watching),
        )
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

    private fun finishWithMessage(@StringRes message: Int) {
        showMessage(message)
        finish()
    }

    private fun showMessage(@StringRes message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
