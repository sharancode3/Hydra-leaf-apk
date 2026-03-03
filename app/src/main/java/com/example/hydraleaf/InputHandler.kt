package com.example.hydraleaf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class InputHandler(
    private val sensorController: SensorController,
    private val onTiltSample: (TiltSample) -> Unit
) {
    private var currentMode: ControlMode = ControlMode.GYROSCOPE
    private var sensorsRunning = false
    private var tiltCollectorJob: Job? = null

    suspend fun stop() {
        tiltCollectorJob?.cancelAndJoin()
        tiltCollectorJob = null
        stopSensors()
    }

    fun start(scope: CoroutineScope) {
        applyMode(scope, currentMode)
    }

    fun updateMode(scope: CoroutineScope, mode: ControlMode) {
        if (mode == currentMode && tiltCollectorJob != null) return
        currentMode = mode
        applyMode(scope, mode)
    }

    private fun applyMode(scope: CoroutineScope, mode: ControlMode) {
        when (mode) {
            ControlMode.GYROSCOPE -> {
                ensureSensors()
                if (tiltCollectorJob == null) {
                    tiltCollectorJob = scope.launch {
                        sensorController.tiltState.collect { sample ->
                            onTiltSample(sample)
                        }
                    }
                }
            }
            ControlMode.TOUCH, ControlMode.TAP -> {
                tiltCollectorJob?.cancel()
                tiltCollectorJob = null
                stopSensors()
            }
        }
    }

    private fun ensureSensors() {
        if (sensorsRunning) return
        sensorController.setupSensors()
        sensorsRunning = true
    }

    private fun stopSensors() {
        if (!sensorsRunning) return
        sensorController.tearDownSensors()
        sensorsRunning = false
    }
}
