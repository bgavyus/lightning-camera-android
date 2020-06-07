package io.github.bgavyus.splash.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.bgavyus.splash.capture.CameraError
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.resourceId
import io.github.bgavyus.splash.databinding.ActivityViewfinderBinding
import io.github.bgavyus.splash.flow.DetectionRecorder
import io.github.bgavyus.splash.graphics.detection.DetectionListener
import io.github.bgavyus.splash.graphics.media.Beeper
import io.github.bgavyus.splash.permissions.PermissionError
import io.github.bgavyus.splash.permissions.PermissionsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

// TODO: Handle rotation
// TODO: Use images in toggle button
// TODO: Replace visual indicator dot with red frame
// TODO: Replace with fragment
class ViewfinderActivity : FragmentActivity(),
    DetectionListener, CompoundButton.OnCheckedChangeListener {
    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate(savedInstanceState = $savedInstanceState)")
        super.onCreate(savedInstanceState)

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

    private fun initToggleButton() =
        binding.toggleButton.setOnCheckedChangeListener(this@ViewfinderActivity)

    private fun grantPermissions() = lifecycleScope.launch {
        try {
            PermissionsManager.grantAll(supportFragmentManager)
        } catch (error: PermissionError) {
            finishWithMessage(error.resourceId)
        }
    }

    private var job: Job? = null
    private val focusDeferScope = DeferScope()

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

    private lateinit var recorder: DetectionRecorder

    private fun init() = lifecycleScope.launch {
        try {
            recorder = DetectionRecorder.init(binding.textureView, this@ViewfinderActivity)
                .apply { focusDeferScope.defer(::close) }
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

    override fun onDetectionStateChanged(detecting: Boolean) {
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
            recorder.record()
        } else {
            recorder.loss()
        }
    }

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${App.defaultString(resourceId)}")
        App.showMessage(resourceId)
        finish()
    }
}
