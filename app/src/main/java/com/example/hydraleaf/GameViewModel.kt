package com.example.hydraleaf

import android.app.Application
import android.graphics.RectF
import android.os.Debug
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hydraleaf.audio.GameAudioEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

// ── Obstacle / Boost / PowerUp entities ──────────────────────────────────────

enum class ObstacleKind { LOG, ROCK }

data class ObstacleState(
    val id: Long, val x: Float, val y: Float,
    val width: Float, val height: Float,
    val warningHighlight: Float = 0f,
    val kind: ObstacleKind = ObstacleKind.LOG,
    val hurdleStyle: HurdleStyle = HurdleStyle.WOOD
)

data class BoostState(val id: Long, val x: Float, val y: Float, val radius: Float)

data class PowerUpCollectible(
    val id: Long, val x: Float, val y: Float,
    val radius: Float, val type: PowerUpType
)

// ── Particle trail state ─────────────────────────────────────────────────────

data class TrailParticle(
    val x: Float, val y: Float, val life: Float,
    val size: Float, val alpha: Float
)

// ── UI state exposed to Compose ──────────────────────────────────────────────

data class GameUiState(
    val leafX: Float = GameConstants.VIRTUAL_WIDTH * 0.5f,
    val leafY: Float = GameConstants.LEAF_BASE_Y,
    val leafVelocityX: Float = 0f,
    val leafVelocityY: Float = 0f,
    val targetX: Float = GameConstants.VIRTUAL_WIDTH * 0.5f,
    val targetY: Float = GameConstants.LEAF_BASE_Y,
    val obstacles: List<ObstacleState> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val phase: GamePhase = GamePhase.IDLE,
    val controlSettings: ControlSettings = ControlSettings(),
    val obstaclesCleared: Int = 0,
    val level: Int = 1,
    val showSettingsPanel: Boolean = false,
    val showTutorial: Boolean = true,
    val soundEnabled: Boolean = true,
    val pauseOverlayVisible: Boolean = false,
    val debugPanelVisible: Boolean = false,
    val debugTelemetry: DebugTelemetry = DebugTelemetry(),
    val lastTiltSample: TiltSample = TiltSample(),
    val lastDeltaTime: Float = 0f,
    val rawTiltX: Float = 0f,
    val rawTiltY: Float = 0f,
    val deltaSeconds: Float = 0f,
    val boosts: List<BoostState> = emptyList(),
    val boostActive: Boolean = false,
    val boostTimeRemaining: Float = 0f,
    val countdownValue: Int = 0,
    val deathAnimProgress: Float = 0f,
    val touchTargetX: Float? = null,
    val touchTargetY: Float? = null,
    // Phase 2 additions
    val activePowerUps: List<ActivePowerUp> = emptyList(),
    val powerUpCollectibles: List<PowerUpCollectible> = emptyList(),
    val activeRiverEvent: ActiveRiverEvent? = null,
    val riverDrops: Int = 0,
    val totalRiverDrops: Int = 0,
    val leafSkin: LeafSkin = LeafSkin.CLASSIC,
    val riverTheme: RiverTheme = RiverTheme.FOREST,
    val dayPhase: DayPhase = DayPhase.DAY,
    val dayCycleProgress: Float = 0f,
    val trailParticles: List<TrailParticle> = emptyList(),
    val leafBreathScale: Float = 1f,
    val leafLeanAngle: Float = 0f,
    val adaptiveDifficulty: AdaptiveDifficulty = AdaptiveDifficulty(),
    val dailyChallenge: DailyChallenge? = null,
    val runTime: Float = 0f,
    val fogAlpha: Float = 0f,
    val narrowChannelOffset: Float = 0f,
    val runDropsEarned: Int = 0,
    val sensitivitySuggestion: String? = null
) {
    val gameState: GameState get() = when (phase) {
        GamePhase.PLAYING, GamePhase.COUNTDOWN, GamePhase.CALIBRATING -> GameState.RUNNING
        GamePhase.IDLE -> GameState.PAUSED
        GamePhase.DEAD, GamePhase.GAME_OVER -> GameState.GAME_OVER
    }
}

enum class GameState { RUNNING, PAUSED, GAME_OVER }

// ── Internal pooled entities ─────────────────────────────────────────────────

private data class ObstacleEntity(
    var id: Long, var x: Float, var y: Float,
    var width: Float, var height: Float, var speed: Float,
    var kind: ObstacleKind, var style: HurdleStyle = HurdleStyle.WOOD,
    var rowToken: Int = 0,
    var counted: Boolean = false,
    var warningHighlight: Float = 0f, var warningTriggered: Boolean = false
)

private data class BoostEntity(
    var id: Long, var x: Float, var y: Float,
    var radius: Float, var speed: Float, var collected: Boolean = false
)

private data class PowerUpEntity(
    var id: Long, var x: Float, var y: Float,
    var radius: Float, var speed: Float, var type: PowerUpType, var collected: Boolean = false
)

private data class ParticleEntity(
    var x: Float, var y: Float, var life: Float,
    var maxLife: Float, var size: Float, var vx: Float, var vy: Float
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class GameViewModel @Inject constructor(
    application: Application,
    val playerSettingsStore: PlayerSettingsStore,
    val audioEngine: GameAudioEngine
) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(ControlSettings())
    val settings: StateFlow<ControlSettings> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Object pools
    private val obstaclePool = ArrayDeque<ObstacleEntity>()
    private val activeObstacles = mutableListOf<ObstacleEntity>()
    private var spawnTimer = GameConstants.OBSTACLE_SPAWN_INTERVAL
    private var nextObstacleId = 0L
    private var nextRowToken = 1
    private var nextGapOnLeft = true

    private val boostPool = ArrayDeque<BoostEntity>()
    private val activeBoosts = mutableListOf<BoostEntity>()
    private var boostSpawnTimer = GameConstants.BOOST_SPAWN_INTERVAL
    private var nextBoostId = 0L
    private var boostTimer = 0f

    private val powerUpPool = ArrayDeque<PowerUpEntity>()
    private val activePowerUpEntities = mutableListOf<PowerUpEntity>()
    private var powerUpSpawnTimer = GameConstants.POWERUP_SPAWN_INTERVAL
    private var nextPowerUpId = 0L
    private val activePowerUpTimers = mutableMapOf<PowerUpType, Float>()

    // River events
    private var eventTimer = GameConstants.EVENT_MIN_INTERVAL + Random.nextFloat() * (GameConstants.EVENT_MAX_INTERVAL - GameConstants.EVENT_MIN_INTERVAL)
    private var currentEvent: ActiveRiverEvent? = null

    // Particle trail
    private val trailParticles = mutableListOf<ParticleEntity>()
    private var trailSpawnAccum = 0f

    // Adaptive difficulty
    private val recentDodges = ArrayDeque<Boolean>()
    private var adaptiveDiff = AdaptiveDifficulty()

    // Day/night
    private var totalPlaytime = 0f
    private var runTime = 0f

    // Currency
    private var runDrops = 0
    private var tapSteerImpulse = 0f

    // Calibration / gyro gesture helpers
    private val recentTiltX = ArrayDeque<Float>()
    private var shakeToggleCount = 0
    private var lastShakeSign = 0
    private var lastShakeNanos = 0L

    // Timing
    private var latestTiltSample: TiltSample = TiltSample()
    private var countdownJob: Job? = null
    private var deathJob: Job? = null
    private var calibrationJob: Job? = null
    private var lastTimestampNanos = 0L
    private var lastSpawnY = -GameConstants.MIN_ROW_SPACING

    // Cosmetics
    private var activeSkin = LeafSkin.CLASSIC
    private var activeTheme = RiverTheme.FOREST
    private var storedDrops = 0

    // FPS tracking
    private var fpsFrameCount = 0
    private var fpsAccum = 0f
    private var currentFps = 60

    init {
        viewModelScope.launch { playerSettingsStore.settingsFlow.collectLatest { s -> _settings.value = s; _uiState.value = _uiState.value.copy(controlSettings = s) } }
        viewModelScope.launch { playerSettingsStore.highScoreFlow.collectLatest { _uiState.value = _uiState.value.copy(highScore = it) } }
        viewModelScope.launch { playerSettingsStore.soundEnabledFlow.collectLatest { _uiState.value = _uiState.value.copy(soundEnabled = it); audioEngine.soundEnabled = it } }
        viewModelScope.launch { playerSettingsStore.tutorialSeenFlow.collectLatest { _uiState.value = _uiState.value.copy(showTutorial = !it) } }
        viewModelScope.launch { playerSettingsStore.riverDropsFlow.collectLatest { storedDrops = it; _uiState.value = _uiState.value.copy(totalRiverDrops = it) } }
        viewModelScope.launch { playerSettingsStore.activeLeafSkinFlow.collectLatest { activeSkin = it; _uiState.value = _uiState.value.copy(leafSkin = it) } }
        viewModelScope.launch { playerSettingsStore.activeRiverThemeFlow.collectLatest { activeTheme = it; _uiState.value = _uiState.value.copy(riverTheme = it) } }
        viewModelScope.launch { totalPlaytime = playerSettingsStore.totalPlaytimeFlow.first() }
    }

    // ── Phase transitions ────────────────────────────────────────────────────

    fun startNewRun() {
        resetInternalState()
        _uiState.value = freshUiState().copy(
            phase = GamePhase.CALIBRATING,
            highScore = _uiState.value.highScore,
            controlSettings = _settings.value,
            soundEnabled = _uiState.value.soundEnabled,
            leafSkin = activeSkin,
            riverTheme = activeTheme,
            totalRiverDrops = storedDrops,
            dailyChallenge = resolveDailyChallenge()
        )
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch { delay(800); transitionToCountdown() }
    }

    fun continueRun() {
        val cur = _uiState.value
        if (cur.phase == GamePhase.IDLE || cur.phase == GamePhase.GAME_OVER) startNewRun()
        else _uiState.value = cur.copy(phase = GamePhase.PLAYING, pauseOverlayVisible = false)
    }

    fun togglePause() {
        val cur = _uiState.value
        when (cur.phase) {
            GamePhase.PLAYING -> _uiState.value = cur.copy(phase = GamePhase.IDLE, pauseOverlayVisible = true)
            GamePhase.IDLE -> continueRun()
            else -> {}
        }
    }

    fun resume() { _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING, pauseOverlayVisible = false) }
    fun resetGame() { startNewRun() }

    private fun transitionToCountdown() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.COUNTDOWN, countdownValue = GameConstants.COUNTDOWN_SECONDS)
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in GameConstants.COUNTDOWN_SECONDS downTo 1) { _uiState.value = _uiState.value.copy(countdownValue = i); delay(1000) }
            _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING, countdownValue = 0)
            audioEngine.start()
        }
    }

    private fun transitionToDead() {
        audioEngine.playDeath()
        _uiState.value = _uiState.value.copy(phase = GamePhase.DEAD, deathAnimProgress = 0f)
        deathJob?.cancel()
        deathJob = viewModelScope.launch {
            val steps = 20; val stepMs = ((GameConstants.DEAD_PHASE_DURATION * 1000) / steps).toLong()
            for (i in 1..steps) { _uiState.value = _uiState.value.copy(deathAnimProgress = i / steps.toFloat()); delay(stepMs) }
            transitionToGameOver()
        }
    }

    private fun transitionToGameOver() {
        val cur = _uiState.value
        if (cur.score > cur.highScore) viewModelScope.launch { playerSettingsStore.setHighScore(cur.score) }
        // Award currency
        viewModelScope.launch {
            playerSettingsStore.addRiverDrops(runDrops)
            playerSettingsStore.addPlaytime(runTime)
        }
        audioEngine.stop()
        val suggestion = generateSensitivitySuggestion()
        _uiState.value = cur.copy(
            phase = GamePhase.GAME_OVER, pauseOverlayVisible = false,
            runDropsEarned = runDrops,
            sensitivitySuggestion = suggestion
        )
    }

    // ── Touch / Tilt input ───────────────────────────────────────────────────

    fun onTouchMove(x: Float, y: Float) { _uiState.value = _uiState.value.copy(touchTargetX = x, touchTargetY = y) }
    fun onTouchUp() { /* keep position */ }

    /** TAP mode: tap left/right halves to steer */
    fun onTapSteer(leftSide: Boolean) {
        tapSteerImpulse = if (leftSide) -1f else 1f
    }

    fun onTiltSample(rawTiltX: Float, rawTiltY: Float, timestampNanos: Long) {
        val dt = if (lastTimestampNanos == 0L) 0f else (timestampNanos - lastTimestampNanos) / 1_000_000_000f
        lastTimestampNanos = timestampNanos
        val s = _settings.value
        val tx = mapTiltToTargetX(rawTiltX, s); val ty = mapTiltToTargetY(rawTiltY, s)
        val sample = TiltSample(rawX = rawTiltX, smoothedX = rawTiltX, rawY = rawTiltY, smoothedY = rawTiltY, timestampNanos = timestampNanos)
        latestTiltSample = sample

        recentTiltX.addLast(rawTiltX)
        while (recentTiltX.size > 24) recentTiltX.removeFirst()

        if (s.controlMode == ControlMode.GYROSCOPE) {
            val signNow = when {
                rawTiltX > 0.72f -> 1
                rawTiltX < -0.72f -> -1
                else -> 0
            }
            if (signNow != 0 && signNow != lastShakeSign) {
                val dt = if (lastShakeNanos == 0L) Long.MAX_VALUE else timestampNanos - lastShakeNanos
                if (dt <= 450_000_000L) {
                    shakeToggleCount += 1
                } else {
                    shakeToggleCount = 1
                }
                lastShakeNanos = timestampNanos
                lastShakeSign = signNow
                if (shakeToggleCount >= 4) {
                    shakeToggleCount = 0
                    viewModelScope.launch {
                        playerSettingsStore.setCalibrationOffset(rawTiltX)
                    }
                }
            }
        }

        _uiState.value = _uiState.value.copy(targetX = tx, targetY = ty, rawTiltX = rawTiltX, rawTiltY = rawTiltY, deltaSeconds = dt, lastTiltSample = sample)
    }

    // ── Settings mutations ───────────────────────────────────────────────────

    fun toggleSound() = viewModelScope.launch { val n = !_uiState.value.soundEnabled; _uiState.value = _uiState.value.copy(soundEnabled = n); playerSettingsStore.setSoundEnabled(n); audioEngine.soundEnabled = n }
    fun dismissTutorial() = viewModelScope.launch { playerSettingsStore.setTutorialSeen(true) }
    fun setSensitivityMultiplier(v: Float) = viewModelScope.launch { playerSettingsStore.setSensitivityMultiplier(v.coerceIn(0.2f, 6f)) }
    fun setCurve(v: SensitivityCurve) = viewModelScope.launch { playerSettingsStore.setCurve(v) }
    fun setInvertTilt(v: Boolean) = viewModelScope.launch { playerSettingsStore.setInvertTilt(v) }
    fun setStiffness(v: Float) = viewModelScope.launch { playerSettingsStore.setStiffness(v.coerceIn(4f, 32f)) }
    fun setDamping(v: Float) = viewModelScope.launch { playerSettingsStore.setDamping(v.coerceIn(0.7f, 0.98f)) }
    fun setTiltResponse(v: Float) = setStiffness(v)
    fun setLeafMomentum(v: Float) = setDamping(v)
    fun setDeadZone(v: Float) = viewModelScope.launch { playerSettingsStore.setDeadZone(v.coerceIn(0f, 0.08f)) }
    fun setHitboxShrink(v: Float) = viewModelScope.launch { playerSettingsStore.setHitboxShrink(v.coerceIn(0.4f, 0.95f)) }
    fun setInstantSnap(v: Boolean) = viewModelScope.launch { playerSettingsStore.setInstantSnap(v) }
    fun setControlMode(m: ControlMode) = viewModelScope.launch { playerSettingsStore.setControlMode(m) }
    fun applyPreset(p: SensitivityPreset) = viewModelScope.launch { playerSettingsStore.applyPreset(p) }
    fun calibrate() = viewModelScope.launch {
        val values = recentTiltX.toList()
        if (values.size < 8) return@launch
        val mean = values.average().toFloat()
        val variance = values.sumOf { sample ->
            val diff = sample - mean
            (diff * diff).toDouble()
        }.toFloat() / values.size
        if (variance > 0.018f) return@launch
        playerSettingsStore.setCalibrationOffset(_uiState.value.rawTiltX)
    }
    fun resetSettings() = viewModelScope.launch { playerSettingsStore.resetSettingsToDefaults(); playerSettingsStore.setTutorialSeen(false) }

    // ── Shop ─────────────────────────────────────────────────────────────────

    fun purchaseSkin(skin: LeafSkin) = viewModelScope.launch {
        if (playerSettingsStore.spendRiverDrops(skin.cost)) { playerSettingsStore.unlockSkin(skin); playerSettingsStore.setActiveSkin(skin) }
    }
    fun selectSkin(skin: LeafSkin) = viewModelScope.launch { playerSettingsStore.setActiveSkin(skin) }
    fun purchaseTheme(theme: RiverTheme) = viewModelScope.launch {
        if (playerSettingsStore.spendRiverDrops(theme.cost)) { playerSettingsStore.unlockTheme(theme); playerSettingsStore.setActiveTheme(theme) }
    }
    fun selectTheme(theme: RiverTheme) = viewModelScope.launch { playerSettingsStore.setActiveTheme(theme) }

    // ── Tilt mapping ─────────────────────────────────────────────────────────

    fun mapTiltToTargetX(tiltRaw: Float, settings: ControlSettings = _settings.value): Float {
        val calibrated = (tiltRaw - settings.calibrationOffset) * if (settings.invertTilt) -1f else 1f
        val deadZoned = if (abs(calibrated) < settings.deadZone) 0f else calibrated
        val curved = when (settings.curve) {
            SensitivityCurve.LINEAR -> deadZoned * settings.sensitivityMultiplier
            SensitivityCurve.EXPONENTIAL -> sign(deadZoned) * abs(deadZoned).pow(1.6f) * settings.sensitivityMultiplier
        }
        return GameConstants.VIRTUAL_WIDTH * 0.5f + curved.coerceIn(-1f, 1f) * GameConstants.VIRTUAL_WIDTH * 0.48f
    }

    fun mapTiltToTargetY(tiltRaw: Float, settings: ControlSettings = _settings.value): Float {
        val adjusted = tiltRaw * if (settings.invertTilt) -1f else 1f
        val dead = if (abs(adjusted) < settings.deadZone) 0f else adjusted
        val curved = when (settings.curve) {
            SensitivityCurve.LINEAR -> dead * (settings.sensitivityMultiplier * 0.85f)
            SensitivityCurve.EXPONENTIAL -> sign(dead) * abs(dead).pow(1.35f) * (settings.sensitivityMultiplier * 0.85f)
        }
        val centerY = (GameConstants.LEAF_VERTICAL_MIN + GameConstants.LEAF_VERTICAL_MAX) * 0.5f
        return centerY + curved.coerceIn(-1f, 1f) * GameConstants.LEAF_VERTICAL_RANGE
    }

    // ── Per-frame game update ────────────────────────────────────────────────

    fun updateGameState(deltaTime: Float, tiltSample: TiltSample) {
        latestTiltSample = tiltSample
        if (deltaTime <= 0f || deltaTime > 0.25f) return

        val cur = _uiState.value
        if (cur.phase != GamePhase.PLAYING) {
            val tx = mapTiltToTargetX(tiltSample.rawX, cur.controlSettings)
            val ty = mapTiltToTargetY(tiltSample.rawY, cur.controlSettings)
            _uiState.value = cur.copy(targetX = tx, targetY = ty, lastTiltSample = tiltSample, lastDeltaTime = deltaTime)
            return
        }

        val settings = cur.controlSettings
        val dt = deltaTime

        // Time tracking
        runTime += dt
        totalPlaytime += dt

        // FPS
        fpsFrameCount++; fpsAccum += dt
        if (fpsAccum >= 1f) { currentFps = fpsFrameCount; fpsFrameCount = 0; fpsAccum = 0f }

        // ── Day/night cycle ──────────────────────────────────────────────────
        val dayProgress = (totalPlaytime % GameConstants.DAY_CYCLE_PERIOD) / GameConstants.DAY_CYCLE_PERIOD
        val dayPhase = when {
            dayProgress < GameConstants.DAWN_END -> DayPhase.DAWN
            dayProgress < GameConstants.DAY_END  -> DayPhase.DAY
            dayProgress < GameConstants.DUSK_END -> DayPhase.DUSK
            else -> DayPhase.NIGHT
        }

        // ── Determine target position ────────────────────────────────────────
        val slowTimeFactor = if (activePowerUpTimers.containsKey(PowerUpType.SLOW_TIME)) GameConstants.SLOW_TIME_FACTOR else 1f
        val effectiveDt = dt * slowTimeFactor

        val targetX: Float; val targetY: Float
        when {
            settings.controlMode == ControlMode.TAP && cur.touchTargetX != null -> { targetX = cur.touchTargetX; targetY = cur.touchTargetY ?: cur.leafY }
            settings.controlMode == ControlMode.TOUCH && cur.touchTargetX != null -> { targetX = cur.touchTargetX; targetY = cur.touchTargetY ?: cur.leafY }
            else -> { targetX = mapTiltToTargetX(tiltSample.rawX, settings); targetY = mapTiltToTargetY(tiltSample.rawY, settings) }
        }

        // ── River event update ───────────────────────────────────────────────
        eventTimer -= dt
        if (eventTimer <= 0f && currentEvent == null) {
            val eventType = RiverEventType.entries[Random.nextInt(RiverEventType.entries.size)]
            currentEvent = ActiveRiverEvent(eventType, eventType.baseDuration)
            eventTimer = 0f
        }
        currentEvent?.let { ev ->
            val remaining = ev.remainingTime - dt
            if (remaining <= 0f) {
                runDrops += GameConstants.DROPS_PER_EVENT_SURVIVE
                currentEvent = null
                eventTimer = GameConstants.EVENT_MIN_INTERVAL + Random.nextFloat() * (GameConstants.EVENT_MAX_INTERVAL - GameConstants.EVENT_MIN_INTERVAL)
            } else {
                currentEvent = ev.copy(remainingTime = remaining)
            }
        }

        // Event effects
        val speedMult = when (currentEvent?.type) {
            RiverEventType.SPEED_SURGE -> GameConstants.SPEED_SURGE_MULTIPLIER
            RiverEventType.CALM_WATERS -> GameConstants.CALM_SPEED_MULTIPLIER
            else -> 1f
        } * adaptiveDiff.speedMultiplier
        val fogAlpha = if (currentEvent?.type == RiverEventType.FOG) GameConstants.FOG_MAX_ALPHA * (currentEvent?.intensity ?: 0f) else 0f
        val narrowOffset = if (currentEvent?.type == RiverEventType.NARROW_CHANNEL) (GameConstants.VIRTUAL_WIDTH - GameConstants.NARROW_CHANNEL_WIDTH) * 0.5f else 0f

        // ── Power-up timers ──────────────────────────────────────────────────
        val expiredPowerUps = mutableListOf<PowerUpType>()
        activePowerUpTimers.entries.removeAll { entry ->
            entry.setValue(entry.value - dt)
            if (entry.value <= 0f) { expiredPowerUps.add(entry.key); true } else false
        }
        val activePowerUpList = activePowerUpTimers.map { (t, rem) -> ActivePowerUp(t, rem, t.durationSec) }

        // ── Power-up spawning ────────────────────────────────────────────────
        powerUpSpawnTimer -= dt
        if (powerUpSpawnTimer <= 0f) {
            spawnPowerUp()
            powerUpSpawnTimer = GameConstants.POWERUP_SPAWN_INTERVAL + Random.nextFloat() * GameConstants.POWERUP_SPAWN_VARIATION - adaptiveDiff.powerUpFrequencyBonus
        }

        // ── Boost spawn ──────────────────────────────────────────────────────
        boostSpawnTimer -= dt
        if (boostSpawnTimer <= 0f) { spawnBoost(); boostSpawnTimer = GameConstants.BOOST_SPAWN_INTERVAL + Random.nextFloat() * GameConstants.BOOST_SPAWN_VARIATION }
        boostTimer = max(0f, boostTimer - dt)

        // ── Leaf physics ─────────────────────────────────────────────────────
        var leafX = cur.leafX; var vx = cur.leafVelocityX
        var leafY = cur.leafY; var vy = cur.leafVelocityY
        val speedBoostActive = activePowerUpTimers.containsKey(PowerUpType.SPEED_BOOST)
        val stiffMul = if (speedBoostActive) GameConstants.SPEED_BOOST_MULTIPLIER else 1f

        when (settings.controlMode) {
            ControlMode.GYROSCOPE -> {
                if (settings.instantSnap) {
                    leafX = targetX; vx = 0f; leafY = targetY; vy = 0f
                } else {
                    vx += (targetX - leafX) * settings.stiffness * stiffMul * effectiveDt
                    leafX += vx * effectiveDt
                    vx *= settings.damping
                    vy += (targetY - leafY) * settings.stiffness * 0.8f * effectiveDt
                    leafY += vy * effectiveDt
                    vy *= settings.damping
                }
            }
            ControlMode.TOUCH -> {
                leafX = targetX
                leafY = targetY
                vx = 0f
                vy = 0f
            }
            ControlMode.TAP -> {
                val tapAccel = GameConstants.VIRTUAL_WIDTH * 2.8f
                val tapFriction = 0.86f
                vx += tapSteerImpulse * tapAccel * effectiveDt
                tapSteerImpulse = 0f
                leafX += vx * effectiveDt
                vx *= tapFriction
                vy += (GameConstants.LEAF_BASE_Y - leafY) * 6.5f * effectiveDt
                leafY += vy * effectiveDt
                vy *= 0.82f
            }
        }

        // Narrow channel clamping
        val leftBound = narrowOffset
        val rightBound = GameConstants.VIRTUAL_WIDTH - narrowOffset

        val boostActiveNow = boostTimer > 0f
        val shieldActive = activePowerUpTimers.containsKey(PowerUpType.SHIELD)
        val hitboxScale = settings.hitboxShrink * if (boostActiveNow) GameConstants.BOOST_HITBOX_SCALE else GameConstants.BASE_HITBOX_SCALE
        val halfW = GameConstants.LEAF_WIDTH * 0.5f * hitboxScale
        val halfH = GameConstants.LEAF_HEIGHT * 0.5f * hitboxScale
        leafX = leafX.coerceIn(max(leftBound, halfW), min(rightBound, GameConstants.VIRTUAL_WIDTH) - halfW)
        leafY = leafY.coerceIn(max(GameConstants.LEAF_VERTICAL_MIN, halfH), min(GameConstants.LEAF_VERTICAL_MAX, GameConstants.VIRTUAL_HEIGHT - halfH))

        val leafRect = RectF(leafX - halfW, leafY - halfH, leafX + halfW, leafY + halfH)

        // ── Magnet pull ──────────────────────────────────────────────────────
        val magnetActive = activePowerUpTimers.containsKey(PowerUpType.MAGNET)

        // ── Update collectibles ──────────────────────────────────────────────
        val boostCollected = updateBoosts(effectiveDt, leafRect, magnetActive)
        if (boostCollected) boostTimer = GameConstants.BOOST_DURATION
        val boostActive = boostTimer > 0f

        val powerUpCollected = updatePowerUps(effectiveDt, leafRect, magnetActive)
        // Update obstacles
        val obstResult = updateObstacles(effectiveDt, leafRect, cur.level, cur.score, speedMult)
        val collided = obstResult.collided && !boostActive && !shieldActive

        // ── Scoring ──────────────────────────────────────────────────────────
        val doublePoints = activePowerUpTimers.containsKey(PowerUpType.DOUBLE_POINTS)
        val pointsMul = if (doublePoints) 2 else 1
        val newScore = cur.score + obstResult.pointsEarned * pointsMul
        val clearedTotal = cur.obstaclesCleared + obstResult.cleared
        val newLevel = 1 + clearedTotal / GameConstants.HURDLES_PER_LEVEL
        runDrops += obstResult.cleared * GameConstants.DROPS_PER_CLEAR

        // ── Adaptive difficulty ──────────────────────────────────────────────
        if (obstResult.cleared > 0) {
            repeat(obstResult.cleared) { recentDodges.addLast(true) }
            while (recentDodges.size > GameConstants.ADAPTIVE_WINDOW) recentDodges.removeFirst()
        }
        if (collided) { recentDodges.addLast(false); while (recentDodges.size > GameConstants.ADAPTIVE_WINDOW) recentDodges.removeFirst() }
        adaptiveDiff = computeAdaptiveDifficulty()

        // ── Particle trail ───────────────────────────────────────────────────
        trailSpawnAccum += dt
        while (trailSpawnAccum >= GameConstants.TRAIL_SPAWN_RATE) {
            trailSpawnAccum -= GameConstants.TRAIL_SPAWN_RATE
            if (trailParticles.size < GameConstants.TRAIL_MAX_PARTICLES) {
                trailParticles.add(ParticleEntity(
                    x = leafX + (Random.nextFloat() - 0.5f) * GameConstants.LEAF_WIDTH * 0.5f,
                    y = leafY + GameConstants.LEAF_HEIGHT * 0.3f,
                    life = GameConstants.TRAIL_PARTICLE_LIFE,
                    maxLife = GameConstants.TRAIL_PARTICLE_LIFE,
                    size = GameConstants.TRAIL_PARTICLE_SIZE * (0.7f + Random.nextFloat() * 0.6f),
                    vx = (Random.nextFloat() - 0.5f) * 30f,
                    vy = 40f + Random.nextFloat() * 20f
                ))
            }
        }
        val pIter = trailParticles.iterator()
        while (pIter.hasNext()) { val p = pIter.next(); p.life -= dt; p.x += p.vx * dt; p.y += p.vy * dt; if (p.life <= 0f) pIter.remove() }

        // ── Leaf animations ──────────────────────────────────────────────────
        val breathScale = 1f + GameConstants.LEAF_BREATH_AMPLITUDE * sin(runTime * 2f * Math.PI.toFloat() / GameConstants.LEAF_BREATH_PERIOD)
        val leanAngle = (vx / 600f).coerceIn(-1f, 1f) * GameConstants.LEAF_MAX_LEAN_DEG

        // ── Audio intensity ──────────────────────────────────────────────────
        val audioIntensity = (newScore / 500f).coerceIn(0f, 1f)
        audioEngine.intensity = audioIntensity
        audioEngine.speedFactor = speedMult

        // ── High score ───────────────────────────────────────────────────────
        var highScore = cur.highScore
        if (newScore > highScore) { highScore = newScore; viewModelScope.launch { playerSettingsStore.setHighScore(highScore) } }

        // ── Debug telemetry ──────────────────────────────────────────────────
        val memMb = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)
        val debugTelemetry = DebugTelemetry(
            rawTilt = tiltSample.rawX, smoothedTilt = tiltSample.smoothedX,
            targetX = targetX, leafX = leafX, deltaTime = dt, viewportScale = 1f,
            fps = currentFps, memoryUsedMb = memMb,
            activeObstacles = activeObstacles.size, activeParticles = trailParticles.size,
            activePowerUps = activePowerUpTimers.size,
            currentEvent = currentEvent?.type?.name ?: "none",
            adaptiveDifficulty = adaptiveDiff.speedMultiplier,
            dayPhase = dayPhase.name,
            audioLayers = audioEngine.activeLayerCount,
            controlMode = settings.controlMode.name
        )

        _uiState.value = cur.copy(
            leafX = leafX, leafY = leafY, leafVelocityX = vx, leafVelocityY = vy,
            targetX = targetX, targetY = targetY,
            obstacles = activeObstacles.map { o -> ObstacleState(o.id, o.x, o.y, o.width, o.height, o.warningHighlight, o.kind, o.style) },
            score = newScore, highScore = highScore,
            obstaclesCleared = clearedTotal, level = newLevel,
            phase = if (collided) cur.phase else GamePhase.PLAYING,
            lastTiltSample = tiltSample, lastDeltaTime = dt,
            debugTelemetry = debugTelemetry,
            boosts = activeBoosts.map { BoostState(it.id, it.x, it.y, it.radius) },
            boostActive = boostActive, boostTimeRemaining = boostTimer,
            activePowerUps = activePowerUpList,
            powerUpCollectibles = activePowerUpEntities.map { PowerUpCollectible(it.id, it.x, it.y, it.radius, it.type) },
            activeRiverEvent = currentEvent,
            riverDrops = runDrops,
            dayPhase = dayPhase, dayCycleProgress = dayProgress,
            trailParticles = trailParticles.map { TrailParticle(it.x, it.y, it.life / it.maxLife, it.size, (it.life / it.maxLife).coerceIn(0f, 1f)) },
            leafBreathScale = breathScale, leafLeanAngle = leanAngle,
            adaptiveDifficulty = adaptiveDiff,
            runTime = runTime, fogAlpha = fogAlpha, narrowChannelOffset = narrowOffset
        )

        if (collided) { audioEngine.playDeath(); transitionToDead() }
    }

    // ── Obstacle logic ───────────────────────────────────────────────────────

    private fun updateObstacles(dt: Float, leafRect: RectF, level: Int, score: Int, speedMult: Float): ObstacleUpdateResult {
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            if (canSpawnRow(level)) {
                spawnSafeRow(level)
                val baseInterval = GameConstants.OBSTACLE_SPAWN_INTERVAL
                val minInterval = GameConstants.SPAWN_INTERVAL_FLOOR
                val scoreFactor = score / 600f
                val levelFactor = (level - 1) * 0.05f
                val diffScale = max(GameConstants.SPEED_HARD_FLOOR, 1f - scoreFactor - levelFactor) * adaptiveDiff.spawnRateMultiplier
                spawnTimer = max(minInterval, baseInterval * diffScale)
            } else {
                spawnTimer = 0.08f
            }
        }
        var collided = false; var pts = 0; var cleared = 0
        val warningY = GameConstants.VIRTUAL_HEIGHT * GameConstants.WARNING_ZONE_RATIO
        val iter = activeObstacles.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            o.y += o.speed * speedMult * dt
            if (!o.warningTriggered && o.y >= warningY) { o.warningTriggered = true; o.warningHighlight = 1f }
            if (o.warningHighlight > 0f) o.warningHighlight = max(0f, o.warningHighlight - dt * 1.25f)
            val oR = RectF(o.x - o.width * 0.5f, o.y - o.height * 0.5f, o.x + o.width * 0.5f, o.y + o.height * 0.5f)
            if (!collided && rectIntersects(leafRect, oR)) collided = true
            if (!o.counted && o.y > leafRect.bottom) {
                o.counted = true; pts += 10; cleared++
                audioEngine.playDodge(Random.nextInt(5))
            }
            if (o.y - o.height * 0.5f > GameConstants.VIRTUAL_HEIGHT + 100f) { iter.remove(); obstaclePool.addLast(o) }
        }
        return ObstacleUpdateResult(pts, collided, cleared)
    }

    private fun canSpawnRow(level: Int): Boolean {
        val maxRowsVisible = when {
            level <= 1 -> 2
            level <= 4 -> 3
            else -> 4
        }
        val visibleRows = activeObstacles
            .filter { it.y + it.height * 0.5f >= 0f && it.y - it.height * 0.5f <= GameConstants.VIRTUAL_HEIGHT }
            .map { it.rowToken }
            .toSet()
            .size
        if (visibleRows >= maxRowsVisible) return false

        val latestRowY = activeObstacles.minOfOrNull { it.y }
        if (latestRowY != null) {
            val requiredSpacing = if (level <= 1) GameConstants.LEVEL1_MIN_ROW_SPACING else GameConstants.MIN_ROW_SPACING
            val spawnCenterY = -GameConstants.OBSTACLE_HEIGHT
            if (latestRowY - spawnCenterY < requiredSpacing) return false
        }
        return true
    }

    private fun spawnSafeRow(level: Int) {
        val vw = GameConstants.VIRTUAL_WIDTH
        val gap = GameConstants.SAFE_GAP_MIN_WIDTH.coerceAtLeast(160f)
        val count = if (level <= 1) 2 else Random.nextInt(1, GameConstants.MAX_OBSTACLES_PER_ROW + 1)
        val rowToken = nextRowToken++
        // Pick one of 4 hurdle styles based on level progression
        val style = HurdleStyle.entries[((level - 1) / 3).coerceIn(0, HurdleStyle.entries.size - 1)]
        val sidePadding = 80f
        val leftCenterRange = (sidePadding + gap * 0.5f)..(vw * 0.42f)
        val rightCenterRange = (vw * 0.58f)..(vw - sidePadding - gap * 0.5f)
        val gapCenter = if (nextGapOnLeft) {
            lerp(leftCenterRange.start, leftCenterRange.endInclusive, Random.nextFloat())
        } else {
            lerp(rightCenterRange.start, rightCenterRange.endInclusive, Random.nextFloat())
        }
        nextGapOnLeft = !nextGapOnLeft
        val gapLeft = gapCenter - gap * 0.5f; val gapRight = gapCenter + gap * 0.5f
        if (count >= 1 && gapLeft > GameConstants.OBSTACLE_MIN_WIDTH * 0.6f) spawnObstacleInRange(0f, gapLeft, level, style, rowToken)
        if (count >= 2 && (vw - gapRight) > GameConstants.OBSTACLE_MIN_WIDTH * 0.6f) spawnObstacleInRange(gapRight, vw, level, style, rowToken)
    }

    private fun spawnObstacleInRange(minX: Float, maxX: Float, level: Int, style: HurdleStyle, rowToken: Int) {
        val kind = if (Random.nextFloat() < GameConstants.ROCK_SPAWN_CHANCE) ObstacleKind.ROCK else ObstacleKind.LOG
        val (w, h) = if (kind == ObstacleKind.LOG)
            lerp(GameConstants.OBSTACLE_MIN_WIDTH, GameConstants.OBSTACLE_MAX_WIDTH, Random.nextFloat()) to GameConstants.OBSTACLE_HEIGHT.coerceAtMost(80f)
        else lerp(GameConstants.ROCK_MIN_WIDTH, GameConstants.ROCK_MAX_WIDTH, Random.nextFloat()) to GameConstants.ROCK_HEIGHT
        val cw = min(w, maxX - minX)
        val x = lerp(minX + cw * 0.5f, maxX - cw * 0.5f, Random.nextFloat())
        val baseSpeed = lerp(GameConstants.OBSTACLE_MIN_SPEED, GameConstants.OBSTACLE_MAX_SPEED, Random.nextFloat())
        val speed = min(GameConstants.SPEED_CAP, baseSpeed + (level - 1) * GameConstants.LEVEL_SPEED_BONUS + if (kind == ObstacleKind.ROCK) GameConstants.ROCK_SPEED_BONUS else 0f)
        val entity = if (obstaclePool.isEmpty()) ObstacleEntity(nextObstacleId++, x, -h, cw, h, speed, kind, style, rowToken)
        else obstaclePool.removeFirst().apply { id = nextObstacleId++; this.x = x; y = -h; width = cw; height = h; this.speed = speed; this.kind = kind; this.style = style; this.rowToken = rowToken; counted = false; warningHighlight = 0f; warningTriggered = false }
        activeObstacles.add(entity)
    }

    // ── Boost logic ──────────────────────────────────────────────────────────

    private fun updateBoosts(dt: Float, leafRect: RectF, magnetPull: Boolean): Boolean {
        var collected = false
        val iter = activeBoosts.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.y += b.speed * dt
            if (magnetPull) { val dx = leafRect.centerX() - b.x; val dy = leafRect.centerY() - b.y; val dist = kotlin.math.sqrt(dx * dx + dy * dy); if (dist < GameConstants.MAGNET_PULL_RADIUS) { b.x += dx / dist * 200f * dt; b.y += dy / dist * 200f * dt } }
            if (!collected && circleIntersectsRect(b.x, b.y, b.radius, leafRect)) { collected = true; b.collected = true; audioEngine.playCollect() }
            if (b.collected || b.y - b.radius > GameConstants.VIRTUAL_HEIGHT + 80f) { iter.remove(); b.collected = false; boostPool.addLast(b) }
        }
        return collected
    }

    private fun spawnBoost() {
        val r = GameConstants.BOOST_RADIUS
        val x = lerp(r, GameConstants.VIRTUAL_WIDTH - r, Random.nextFloat())
        val e = if (boostPool.isEmpty()) BoostEntity(nextBoostId++, x, -r, r, GameConstants.BOOST_DRIFT_SPEED)
        else boostPool.removeFirst().apply { id = nextBoostId++; this.x = x; y = -r; radius = r; speed = GameConstants.BOOST_DRIFT_SPEED; collected = false }
        activeBoosts.add(e)
    }

    // ── Power-up logic ───────────────────────────────────────────────────────

    private fun updatePowerUps(dt: Float, leafRect: RectF, magnetPull: Boolean): Boolean {
        var collected = false
        val iter = activePowerUpEntities.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.y += p.speed * dt
            if (magnetPull) { val dx = leafRect.centerX() - p.x; val dy = leafRect.centerY() - p.y; val dist = kotlin.math.sqrt(dx * dx + dy * dy); if (dist < GameConstants.MAGNET_PULL_RADIUS) { p.x += dx / dist * 200f * dt; p.y += dy / dist * 200f * dt } }
            if (!collected && circleIntersectsRect(p.x, p.y, p.radius, leafRect)) {
                collected = true; p.collected = true
                activePowerUpTimers[p.type] = p.type.durationSec
                runDrops += GameConstants.DROPS_PER_POWERUP
                audioEngine.playPowerUp()
            }
            if (p.collected || p.y - p.radius > GameConstants.VIRTUAL_HEIGHT + 80f) { iter.remove(); powerUpPool.addLast(p) }
        }
        return collected
    }

    private fun spawnPowerUp() {
        val type = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
        val r = GameConstants.POWERUP_RADIUS
        val x = lerp(r, GameConstants.VIRTUAL_WIDTH - r, Random.nextFloat())
        val e = if (powerUpPool.isEmpty()) PowerUpEntity(nextPowerUpId++, x, -r, r, GameConstants.POWERUP_DRIFT_SPEED, type)
        else powerUpPool.removeFirst().apply { id = nextPowerUpId++; this.x = x; y = -r; radius = r; speed = GameConstants.POWERUP_DRIFT_SPEED; this.type = type; this.collected = false }
        activePowerUpEntities.add(e)
    }

    // ── Adaptive difficulty ──────────────────────────────────────────────────

    private fun computeAdaptiveDifficulty(): AdaptiveDifficulty {
        if (recentDodges.size < 5) return adaptiveDiff
        val rate = recentDodges.count { it }.toFloat() / recentDodges.size
        var sm = adaptiveDiff.speedMultiplier; var sr = adaptiveDiff.spawnRateMultiplier; var pf = adaptiveDiff.powerUpFrequencyBonus
        if (rate > GameConstants.ADAPTIVE_EASY_THRESH) { sm += GameConstants.ADAPTIVE_SPEED_STEP; sr -= GameConstants.ADAPTIVE_SPAWN_STEP }
        if (rate < GameConstants.ADAPTIVE_HARD_THRESH) { sm -= GameConstants.ADAPTIVE_SPEED_STEP; sr += GameConstants.ADAPTIVE_SPAWN_STEP; pf += 1f }
        return AdaptiveDifficulty(rate, sm.coerceIn(0.6f, 1.5f), sr.coerceIn(0.6f, 1.5f), pf.coerceIn(0f, 5f))
    }

    // ── Daily challenge ──────────────────────────────────────────────────────

    private fun resolveDailyChallenge(): DailyChallenge {
        val dayIndex = (System.currentTimeMillis() / 86400000L).toInt()
        val type = ChallengeType.entries[dayIndex % ChallengeType.entries.size]
        return DailyChallenge(type, dayIndex = dayIndex)
    }

    // ── Sensitivity auto-tune suggestion ─────────────────────────────────────

    private fun generateSensitivitySuggestion(): String? {
        val rate = if (recentDodges.isNotEmpty()) recentDodges.count { it }.toFloat() / recentDodges.size else return null
        return when {
            rate < 0.3f && _settings.value.sensitivityMultiplier > 2.5f -> "Try lowering sensitivity for more control."
            rate < 0.3f && _settings.value.stiffness > 20f -> "Try reducing stiffness for smoother movement."
            rate > 0.9f && _settings.value.sensitivityMultiplier < 2f -> "You might enjoy higher sensitivity for a challenge!"
            else -> null
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun resetInternalState() {
        activeObstacles.clear(); obstaclePool.clear()
        activeBoosts.clear(); boostPool.clear()
        activePowerUpEntities.clear(); powerUpPool.clear()
        activePowerUpTimers.clear()
        trailParticles.clear(); trailSpawnAccum = 0f
        recentDodges.clear(); adaptiveDiff = AdaptiveDifficulty()
        currentEvent = null
        eventTimer = GameConstants.EVENT_MIN_INTERVAL + Random.nextFloat() * (GameConstants.EVENT_MAX_INTERVAL - GameConstants.EVENT_MIN_INTERVAL)
        spawnTimer = GameConstants.OBSTACLE_SPAWN_INTERVAL
        boostSpawnTimer = GameConstants.BOOST_SPAWN_INTERVAL
        powerUpSpawnTimer = GameConstants.POWERUP_SPAWN_INTERVAL
        nextObstacleId = 0L; nextBoostId = 0L; nextPowerUpId = 0L; boostTimer = 0f
        lastSpawnY = -GameConstants.MIN_ROW_SPACING
        lastTimestampNanos = 0L; runTime = 0f; runDrops = 0
        nextRowToken = 1
        nextGapOnLeft = true
        tapSteerImpulse = 0f
        recentTiltX.clear()
        shakeToggleCount = 0
        lastShakeSign = 0
        lastShakeNanos = 0L
        fpsFrameCount = 0; fpsAccum = 0f; currentFps = 60
        countdownJob?.cancel(); deathJob?.cancel(); calibrationJob?.cancel()
    }

    private fun freshUiState() = GameUiState(
        leafX = GameConstants.VIRTUAL_WIDTH * 0.5f, leafY = GameConstants.LEAF_BASE_Y,
        targetX = GameConstants.VIRTUAL_WIDTH * 0.5f, targetY = GameConstants.LEAF_BASE_Y
    )

    fun detectCollision(leafRect: RectF, obstacleRect: RectF): Boolean = rectIntersects(leafRect, obstacleRect)

    private fun rectIntersects(a: RectF, b: RectF): Boolean =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    private fun circleIntersectsRect(cx: Float, cy: Float, r: Float, rect: RectF): Boolean {
        val closestX = cx.coerceIn(rect.left, rect.right); val closestY = cy.coerceIn(rect.top, rect.bottom)
        val dx = cx - closestX; val dy = cy - closestY; return dx * dx + dy * dy <= r * r
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)

    private data class ObstacleUpdateResult(val pointsEarned: Int, val collided: Boolean, val cleared: Int)

    override fun onCleared() { super.onCleared(); audioEngine.release() }
}
