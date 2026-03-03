package com.example.hydraleaf

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs

private const val LOW_PASS_ALPHA = 0.1f

data class TiltSample(
    val rawX: Float = 0f,     val smoothedX: Float = 0f,
    val rawY: Float = 0f,     val smoothedY: Float = 0f,
    val timestampNanos: Long = 0L
) {
    val raw: Float get() = rawX
    val smoothed: Float get() = smoothedX
}

/**
 * Manages device sensors, with drift correction and calibration profiles.
 *
 * Priority: TYPE_GAME_ROTATION_VECTOR > TYPE_ROTATION_VECTOR > Accel+Mag fallback.
 * Features:
 *  - Auto-calibration with sample averaging
 *  - Continuous gyroscope drift correction
 *  - Low-pass noise filter
 *  - Physics-ready normalised -1..1 tilt on both axes
 */
class SensorController(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gameRotationSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer        = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer         = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix    = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravityValues     = FloatArray(3)
    private val geomagneticValues = FloatArray(3)

    private var smoothedTiltX = 0f
    private var smoothedTiltY = 0f

    // Calibration
    private var calibOffsetX = 0f;  private var calibOffsetY = 0f
    private var calibrationSamples = 0
    private var calibSumX = 0f;     private var calibSumY = 0f
    private var isCalibrating = false

    // Drift correction: continuously nudges offset toward recent average
    private var driftAvgX = 0f; private var driftAvgY = 0f
    private val driftAlpha = 0.002f  // very slow convergence
    private var driftCorrectionEnabled = true

    private val smoothAlpha = 0.14f

    private val _tiltState = MutableStateFlow(TiltSample())
    val tiltState: StateFlow<TiltSample> = _tiltState

    /** Which sensor was actually registered */
    var activeSensorName: String = "none"; private set

    fun setupSensors() {
        when {
            gameRotationSensor != null -> {
                sensorManager.registerListener(this, gameRotationSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorName = "GAME_ROTATION_VECTOR"
            }
            rotationVectorSensor != null -> {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
                activeSensorName = "ROTATION_VECTOR"
            }
            else -> { registerFallback(); activeSensorName = "ACCEL+MAG" }
        }
    }

    fun tearDownSensors() { sensorManager.unregisterListener(this) }

    fun beginCalibration() { calibSumX = 0f; calibSumY = 0f; calibrationSamples = 0; isCalibrating = true }

    fun finishCalibration(): Pair<Float, Float> {
        isCalibrating = false
        if (calibrationSamples > 0) {
            calibOffsetX = calibSumX / calibrationSamples
            calibOffsetY = calibSumY / calibrationSamples
        }
        // Reset drift averages after fresh calibration
        driftAvgX = 0f; driftAvgY = 0f
        return calibOffsetX to calibOffsetY
    }

    fun setCalibrationOffset(x: Float, y: Float) { calibOffsetX = x; calibOffsetY = y }
    fun setDriftCorrection(enabled: Boolean) { driftCorrectionEnabled = enabled }

    // ── SensorEventListener ─────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR ->
                handleRotationVector(event.values, event.timestamp)
            Sensor.TYPE_ACCELEROMETER -> { lowPassFilter(event.values, gravityValues); computeFallbackTilt(event.timestamp) }
            Sensor.TYPE_MAGNETIC_FIELD -> { lowPassFilter(event.values, geomagneticValues); computeFallbackTilt(event.timestamp) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ── Internal ────────────────────────────────────────────────────────────

    private fun handleRotationVector(values: FloatArray, timestamp: Long) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        publishTilt(orientationAngles[2], orientationAngles[1], timestamp)
    }

    private fun computeFallbackTilt(timestamp: Long) {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            publishTilt(orientationAngles[2], orientationAngles[1], timestamp)
        }
    }

    private fun publishTilt(rollRadians: Float, pitchRadians: Float, timestamp: Long) {
        val rawX = (rollRadians / (PI / 2)).toFloat().coerceIn(-1f, 1f)
        val rawY = (pitchRadians / (PI / 2)).toFloat().coerceIn(-1f, 1f)

        if (isCalibrating) { calibSumX += rawX; calibSumY += rawY; calibrationSamples++ }

        var cx = rawX - calibOffsetX
        var cy = rawY - calibOffsetY

        // Continuous drift correction
        if (driftCorrectionEnabled && !isCalibrating) {
            driftAvgX += (cx - driftAvgX) * driftAlpha
            driftAvgY += (cy - driftAvgY) * driftAlpha
            // Only correct if drift is small (avoids correcting real tilt)
            if (abs(driftAvgX) < 0.08f) cx -= driftAvgX
            if (abs(driftAvgY) < 0.08f) cy -= driftAvgY
        }

        smoothedTiltX += (cx - smoothedTiltX) * smoothAlpha
        smoothedTiltY += (cy - smoothedTiltY) * smoothAlpha

        _tiltState.value = TiltSample(
            rawX = cx, smoothedX = smoothedTiltX,
            rawY = cy, smoothedY = smoothedTiltY,
            timestampNanos = timestamp
        )
    }

    private fun registerFallback() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }
}

fun lowPassFilter(input: FloatArray, output: FloatArray, alpha: Float = LOW_PASS_ALPHA) {
    for (i in input.indices) output[i] = output[i] + alpha * (input[i] - output[i])
}
