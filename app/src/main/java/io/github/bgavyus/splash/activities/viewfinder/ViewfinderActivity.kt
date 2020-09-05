package io.github.bgavyus.splash.activities.viewfinder

import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.ResourceCapable
import io.github.bgavyus.splash.common.extensions.callOnEach
import io.github.bgavyus.splash.common.extensions.launchAll
import io.github.bgavyus.splash.common.extensions.reflectTo
import io.github.bgavyus.splash.databinding.ActivityViewfinderBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import java.io.IOException
import java.util.*

// TODO: Use images in toggle button
// TODO: Replace visual indicator dot with red frame
// TODO: Replace with fragment
@AndroidEntryPoint
class ViewfinderActivity : FragmentActivity(), TextureView.SurfaceTextureListener {
    private val viewModel: ViewfinderViewModel by viewModels()
    private lateinit var binding: ActivityViewfinderBinding

    init {
        lifecycleScope.launchWhenCreated {
            enterFullScreen()
            inflateView()
            grantPermissions()
            bind()
        }
    }

    private fun enterFullScreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun inflateView() {
        binding = ActivityViewfinderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private suspend fun grantPermissions() = viewModel.grantPermissions()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Logger.debug("onWindowFocusChanged(hasFocus = $hasFocus)")
        viewModel.active.value = hasFocus
    }

    private fun bind() {
        binding.textureView.surfaceTextureListener = this
        viewModel.surfaceTexture.value?.let { binding.textureView.surfaceTexture = it }

        lifecycleScope.launchAll(
            viewModel.transformMatrix.callOnEach(binding.textureView::setTransform),
            viewModel.detecting.callOnEach(::setDetectionIndicatorActive),
            binding.watchToggle.checked().reflectTo(viewModel.watching),
            viewModel.lastException.filterNotNull().callOnEach(::onException)
        )
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        viewModel.surfaceTexture.value = surface
        onSurfaceTextureSizeChanged(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        viewModel.viewSize.value = Size(width, height)
    }

    // TODO: Find less frequent trigger for buffer adjustments
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = viewModel.adjustBufferSize()

    private fun setDetectionIndicatorActive(active: Boolean) {
        binding.detectionIndicator.isInvisible = !active
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = /* shouldRelease = */ false

    private fun onException(exception: Exception) = finishWithMessage(
        when (exception) {
            is ResourceCapable -> exception.resourceId
            is IOException -> R.string.error_io
            else -> R.string.error_uncaught
        }
    )

    private fun finishWithMessage(resourceId: Int) {
        showMessage(resourceId)
        finish()
    }

    private fun showMessage(resourceId: Int) {
        Logger.debug("Showing message: ${getDefaultString(resourceId)}")

        Toast.makeText(applicationContext, resourceId, Toast.LENGTH_LONG).run {
            setGravity(Gravity.CENTER, /* xOffset = */ 0, /* yOffset = */ 0)
            show()
        }
    }

    private fun getDefaultString(resourceId: Int): String {
        val config = Configuration().apply { setLocale(Locale.ROOT) }
        return applicationContext.createConfigurationContext(config).getString(resourceId)
    }
}

private fun ToggleButton.checked(): Flow<Boolean> = callbackFlow {
    setOnCheckedChangeListener { _, checked -> sendBlocking(checked) }
    awaitClose { setOnCheckedChangeListener(null) }
}
