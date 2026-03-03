package com.example.hydraleaf

data class DebugTelemetry(
    val rawTilt: Float = 0f,
    val smoothedTilt: Float = 0f,
    val targetX: Float = 0f,
    val leafX: Float = 0f,
    val deltaTime: Float = 0f,
    val viewportScale: Float = 1f,
    val fps: Int = 0,
    val memoryUsedMb: Float = 0f,
    val activeObstacles: Int = 0,
    val activeParticles: Int = 0,
    val activePowerUps: Int = 0,
    val currentEvent: String = "none",
    val adaptiveDifficulty: Float = 1f,
    val dayPhase: String = "DAY",
    val thermalStatus: String = "OK",
    val audioLayers: Int = 0,
    val controlMode: String = "GYRO"
)
