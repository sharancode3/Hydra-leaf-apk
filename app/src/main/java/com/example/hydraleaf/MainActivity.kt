package com.example.hydraleaf

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.example.hydraleaf.ui.HydraLeafTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var sensorController: SensorController
    private lateinit var inputHandler: InputHandler
    private var modeCollectorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sensorController = SensorController(this)
        inputHandler = InputHandler(sensorController) { sample ->
            gameViewModel.onTiltSample(sample.rawX, sample.rawY, sample.timestampNanos)
        }

        setContent {
            HydraLeafTheme {
                HydraLeafApp(viewModel = gameViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inputHandler.start(lifecycleScope)
        modeCollectorJob = lifecycleScope.launch {
            gameViewModel.settings.collect { settings ->
                inputHandler.updateMode(this, settings.controlMode)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        modeCollectorJob?.cancel()
        modeCollectorJob = null
        lifecycleScope.launch {
            inputHandler.stop()
        }
    }
}
