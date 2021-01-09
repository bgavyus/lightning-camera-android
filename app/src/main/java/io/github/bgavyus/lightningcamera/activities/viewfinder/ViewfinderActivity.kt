package io.github.bgavyus.lightningcamera.activities.viewfinder

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.KeyEvent
import android.view.TextureView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.extensions.callOnEach
import io.github.bgavyus.lightningcamera.common.extensions.launchAll
import io.github.bgavyus.lightningcamera.common.extensions.reflectTo
import io.github.bgavyus.lightningcamera.databinding.ActivityViewfinderBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
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

            viewModel.transformMatrix.callOnEach(binding.textureView::setTransform),
            viewModel.detecting.callOnEach(::setDetectionIndicatorActive),

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

private fun ToggleButton.checked() = callbackFlow {
    setOnCheckedChangeListener { _, checked -> sendBlocking(checked) }
    awaitClose { setOnCheckedChangeListener(null) }
}

private sealed class SurfaceTextureEvent {
    data class Available(val surface: SurfaceTexture) : SurfaceTextureEvent()
    data class SizeChanged(val size: Size) : SurfaceTextureEvent()
    object Updated : SurfaceTextureEvent()
}

private fun TextureView.surfaceTextureEvents() = callbackFlow<SurfaceTextureEvent> {
    val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            sendBlocking(SurfaceTextureEvent.Available(surface))
            onSurfaceTextureSizeChanged(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            sendBlocking(SurfaceTextureEvent.SizeChanged(Size(width, height)))

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
            sendBlocking(SurfaceTextureEvent.Updated)

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = /* shouldRelease */ false
    }

    surfaceTexture?.let { listener.onSurfaceTextureAvailable(it, width, height) }
    surfaceTextureListener = listener
    awaitClose()
}
