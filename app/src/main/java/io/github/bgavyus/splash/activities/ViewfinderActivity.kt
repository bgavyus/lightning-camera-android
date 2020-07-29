package io.github.bgavyus.splash.activities

import android.content.res.Configuration
import android.os.Bundle
import android.renderscript.RenderScript
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.bgavyus.splash.capture.CameraError
import io.github.bgavyus.splash.common.Application
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Device
import io.github.bgavyus.splash.common.resourceId
import io.github.bgavyus.splash.databinding.ActivityViewfinderBinding
import io.github.bgavyus.splash.graphics.media.Beeper
import io.github.bgavyus.splash.permissions.PermissionError
import io.github.bgavyus.splash.permissions.PermissionsManager
import io.github.bgavyus.splash.permissions.PermissionsRequester
import io.github.bgavyus.splash.storage.Storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

// TODO: Handle rotation
// TODO: Use images in toggle button
// TODO: Replace visual indicator dot with red frame
// TODO: Replace with fragment
class ViewfinderActivity : FragmentActivity(), CompoundButton.OnCheckedChangeListener {
    companion object {
        // TODO: Replace TAGs with logger
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private lateinit var storage: Storage
    private lateinit var permissionsRequester: PermissionsRequester
    private lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate(savedInstanceState = $savedInstanceState)")
        super.onCreate(savedInstanceState)

        storage = Storage(contentResolver)
        permissionsRequester = PermissionsRequester(supportFragmentManager)
        permissionsManager = PermissionsManager(Application.context, storage, permissionsRequester)

        enterFullScreen()
        inflateView()
        initToggleButton()
        grantPermissions()
    }

    private fun enterFullScreen() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private lateinit var binding: ActivityViewfinderBinding

    private fun inflateView() {
        binding = ActivityViewfinderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initToggleButton() = binding.toggleButton.setOnCheckedChangeListener(this)

    private fun grantPermissions() = lifecycleScope.launch {
        try {
            permissionsManager.grantAll()
        } catch (error: PermissionError) {
            finishWithMessage(error.resourceId)
        }
    }

    private var job: Job? = null
    private val focusDeferScope = DeferScope()

    // TODO: Simplify flow
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.d(TAG, "onWindowFocusChanged(hasFocus = $hasFocus)")

        if (hasFocus) {
            job = init()
        } else {
            job?.run {
                if (isActive) {
                    cancel()
                }
            }

            focusDeferScope.close()
        }
    }

    private lateinit var viewModel: ViewfinderViewModel

    private fun init() = lifecycleScope.launch {
        try {
            viewModel = ViewfinderViewModel(
                context = Application.context,
                storage = storage,
                device = Device(Application.context),
                textureView = binding.textureView,
                renderScript = RenderScript.create(Application.context)
            ).apply {
                create()
                focusDeferScope.defer(::close)

                detectingStates()
                    .onEach { onDetectionStateChanged(it) }
                    .launchIn(lifecycleScope)
            }
        } catch (error: PermissionError) {
            finishWithMessage(error.resourceId)
        } catch (error: CameraError) {
            finishWithMessage(error.resourceId)
        } catch (error: IOException) {
            Log.e(TAG, "IOException", error)
            finishWithMessage(error.resourceId)
        }
    }

    private var detecting = false

    private fun onDetectionStateChanged(detecting: Boolean) {
        Log.v(TAG, "Detecting = $detecting")
        setDetectionIndicatorsActive(detecting)
        this.detecting = detecting
        onStateChanged()
    }

    private fun setDetectionIndicatorsActive(active: Boolean) {
        setVisualDetectionIndicatorActive(active)
        setAudibleDetectionIndicatorActive(active)
    }

    private fun setVisualDetectionIndicatorActive(active: Boolean) = lifecycleScope.launch {
        binding.detectionIndicator.isInvisible = !active
    }

    private val beeper = Beeper()

    private fun setAudibleDetectionIndicatorActive(active: Boolean) {
        if (active) {
            beeper.start()
        } else {
            beeper.stop()
        }
    }

    private var watching = false

    override fun onCheckedChanged(buttonView: CompoundButton?, watching: Boolean) {
        Log.i(TAG, "Watching = $watching")
        this.watching = watching
        onStateChanged()
    }

    private fun onStateChanged() {
        if (watching && detecting) {
            viewModel.record()
        } else {
            viewModel.loss()
        }
    }

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${getDefaultString(resourceId)}")
        showMessage(resourceId)
        finish()
    }

    private fun getDefaultString(resourceId: Int): String {
        val config = Configuration().apply { setLocale(Locale.ROOT) }
        return Application.context.createConfigurationContext(config).getString(resourceId)
    }

    private fun showMessage(resourceId: Int) {
        Toast.makeText(Application.context, resourceId, Toast.LENGTH_LONG).run {
            setGravity(Gravity.CENTER, /* xOffset = */ 0, /* yOffset = */ 0)
            show()
        }
    }
}
