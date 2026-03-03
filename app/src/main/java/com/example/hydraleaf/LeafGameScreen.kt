package com.example.hydraleaf

import android.graphics.PointF
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.input.pointer.pointerInteropFilter
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ── Theme color palettes for river themes ────────────────────────────────────

private object ThemeColors {
    fun waterGradient(theme: RiverTheme, dayPhase: DayPhase): List<Color> = when (theme) {
        RiverTheme.FOREST  -> when (dayPhase) {
            DayPhase.DAWN  -> listOf(Color(0xFF0A2520), Color(0xFF1A5040), Color(0xFF208060))
            DayPhase.DAY   -> listOf(Color(0xFF03111A), Color(0xFF053A4A), Color(0xFF0C6B5F))
            DayPhase.DUSK  -> listOf(Color(0xFF1A1008), Color(0xFF3A2510), Color(0xFF4A3818))
            DayPhase.NIGHT -> listOf(Color(0xFF020810), Color(0xFF061828), Color(0xFF0A2838))
        }
        RiverTheme.ARCTIC  -> listOf(Color(0xFF0A1828), Color(0xFF1A3858), Color(0xFF3070A0))
        RiverTheme.VOLCANIC -> listOf(Color(0xFF1A0808), Color(0xFF3A1010), Color(0xFF602020))
        RiverTheme.CRYSTAL -> listOf(Color(0xFF100820), Color(0xFF281848), Color(0xFF483080))
        RiverTheme.MIDNIGHT -> listOf(Color(0xFF020208), Color(0xFF080818), Color(0xFF101030))
    }

    fun rippleColor(theme: RiverTheme): Color = when (theme) {
        RiverTheme.FOREST  -> Color(0xFF59F0FF)
        RiverTheme.ARCTIC  -> Color(0xFFA0D8FF)
        RiverTheme.VOLCANIC -> Color(0xFFFF6040)
        RiverTheme.CRYSTAL -> Color(0xFFB080FF)
        RiverTheme.MIDNIGHT -> Color(0xFF4040FF)
    }

    fun leafColors(skin: LeafSkin): Pair<Color, Color> = when (skin) {
        LeafSkin.CLASSIC -> Color(0xFF9AD85F) to Color(0xFF6BA030)
        LeafSkin.GOLDEN  -> Color(0xFFFFD740) to Color(0xFFBF9F20)
        LeafSkin.FROST   -> Color(0xFFA0E8FF) to Color(0xFF60A8D0)
        LeafSkin.FIRE    -> Color(0xFFFF6040) to Color(0xFFBF3020)
        LeafSkin.NEON    -> Color(0xFF40FF80) to Color(0xFF20C060)
        LeafSkin.COSMIC  -> Color(0xFFA060FF) to Color(0xFF6030C0)
        LeafSkin.RAINBOW -> Color(0xFFFF80C0) to Color(0xFF8040FF)
    }
}

// ── Main composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LeafGameScreen(
    viewModel: GameViewModel,
    onRequestCalibrate: () -> Unit,
    showSettingsOnLaunch: Boolean = false,
    onSettingsPanelConsumed: () -> Unit = {},
    onBackToMenu: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var lastThreeFingerTapMs by remember { mutableStateOf(0L) }
    var maxPointersInGesture by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val reusableLeafPath = remember { Path() }
    val latestTilt by rememberUpdatedState(uiState.lastTiltSample)

    // Haptic on collision
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.DEAD) {
            try {
                val vibrator = context.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showSettingsOnLaunch) { if (showSettingsOnLaunch) { showSettings = true; onSettingsPanelConsumed() } }

    LaunchedEffect(uiState.showTutorial, uiState.phase, uiState.controlSettings.controlMode) {
        if (uiState.showTutorial && uiState.phase == GamePhase.PLAYING && uiState.controlSettings.controlMode == ControlMode.TOUCH) {
            delay(2500)
            viewModel.dismissTutorial()
        }
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (isActive) {
            val frameTime = withFrameNanos { it }
            if (lastFrameTime != 0L) { viewModel.updateGameState((frameTime - lastFrameTime) / 1_000_000_000f, latestTilt) }
            lastFrameTime = frameTime
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Touch / Tap input
        val inputMod = when (uiState.controlSettings.controlMode) {
            ControlMode.TOUCH -> Modifier.pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    viewModel.onTouchMove(change.position.x * GameConstants.VIRTUAL_WIDTH / size.width, change.position.y * GameConstants.VIRTUAL_HEIGHT / size.height)
                }
            }
            ControlMode.TAP -> Modifier.pointerInput(Unit) {
                detectTapGestures { offset -> viewModel.onTapSteer(offset.x < size.width / 2f) }
            }
            else -> Modifier
        }

        val tapMod = if (BuildConfig.SHOW_DEBUG_OVERLAY) {
            Modifier.pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        maxPointersInGesture = 1
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        maxPointersInGesture = max(maxPointersInGesture, event.pointerCount)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (maxPointersInGesture >= 3) {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastThreeFingerTapMs <= 360L) {
                                showDebug = !showDebug
                                lastThreeFingerTapMs = 0L
                            } else {
                                lastThreeFingerTapMs = now
                            }
                        }
                        maxPointersInGesture = 0
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        maxPointersInGesture = 0
                    }
                }
                false
            }
        } else {
            Modifier
        }

        // ── Canvas ───────────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize().then(inputMod).then(tapMod)) {
            val vp = IdentityViewport

            // 5-layer parallax background
            drawParallaxBackground(uiState, vp)

            // Procedural light rays
            drawLightRays(uiState.runTime, uiState.dayPhase, vp)

            // Narrow channel walls
            if (uiState.narrowChannelOffset > 0f) drawNarrowChannel(uiState.narrowChannelOffset, vp)

            // Trail particles
            drawTrailParticles(uiState.trailParticles, uiState.leafSkin, vp)

            // Obstacles with procedural textures
            drawObstacles(uiState, vp)

            // Power-up collectibles
            drawPowerUpCollectibles(uiState.powerUpCollectibles, vp)

            // Boosts
            drawBoosts(uiState.boosts, vp)

            // Leaf with breathing + lean
            drawLeaf(uiState, vp, reusableLeafPath)

            // Shield visual
            if (uiState.activePowerUps.any { it.type == PowerUpType.SHIELD }) {
                val c = logicalToScreen(PointF(uiState.leafX, uiState.leafY), vp)
                drawCircle(Color(0x5500AAFF), GameConstants.SHIELD_FLASH_RADIUS * vp.scale, Offset(c.x, c.y), style = Stroke(4f * vp.scale))
            }

            // Fog overlay
            if (uiState.fogAlpha > 0f) drawRect(Color.White.copy(alpha = uiState.fogAlpha), size = size)

            // Water ripple overlay
            drawWaterRipples(uiState, vp)
        }

        // ── HUD ──────────────────────────────────────────────────────────────
        AnimatedVisibility(visible = !showSettings && uiState.phase == GamePhase.PLAYING) {
            IconHud(Modifier.align(Alignment.TopCenter), 0.9f, true, uiState.soundEnabled,
                { viewModel.togglePause() }, { showSettings = true }, { viewModel.toggleSound() })
        }

        // Power-up HUD timers
        AnimatedVisibility(visible = uiState.activePowerUps.isNotEmpty() && uiState.phase == GamePhase.PLAYING) {
            PowerUpHud(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 80.dp), uiState.activePowerUps)
        }

        // River event indicator
        AnimatedVisibility(visible = uiState.activeRiverEvent != null && uiState.phase == GamePhase.PLAYING) {
            uiState.activeRiverEvent?.let { ev ->
                RiverEventBanner(Modifier.align(Alignment.TopCenter).padding(top = 70.dp), ev)
            }
        }

        // Countdown
        AnimatedVisibility(visible = uiState.phase == GamePhase.COUNTDOWN, enter = fadeIn() + scaleIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Text(if (uiState.countdownValue > 0) uiState.countdownValue.toString() else "GO!", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 72.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Calibrating
        AnimatedVisibility(visible = uiState.phase == GamePhase.CALIBRATING) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calibrating...", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Hold device steady", style = MaterialTheme.typography.bodyMedium, color = Color(0xAAFFFFFF))
                }
            }
        }

        // Tutorial
        AnimatedVisibility(visible = uiState.showTutorial && uiState.phase == GamePhase.PLAYING && uiState.controlSettings.controlMode == ControlMode.TOUCH) {
            TutorialOverlay(
                Modifier.align(Alignment.Center).pointerInput(Unit) {
                    detectTapGestures(onTap = { viewModel.dismissTutorial() })
                },
                uiState.controlSettings.controlMode
            )
        }

        // Pause
        if (uiState.pauseOverlayVisible || (uiState.phase == GamePhase.IDLE && uiState.score > 0)) {
            PauseOverlay({ viewModel.continueRun() }, { viewModel.startNewRun() }, { showSettings = true }, { showSettings = false; viewModel.resume(); onBackToMenu() })
        }

        // Game Over
        AnimatedVisibility(visible = uiState.phase == GamePhase.GAME_OVER, enter = fadeIn(tween(400)) + scaleIn(tween(400)), exit = fadeOut()) {
            GameOverScreen(uiState.score, uiState.highScore, uiState.level, uiState.obstaclesCleared,
                uiState.runDropsEarned, uiState.sensitivitySuggestion, { viewModel.startNewRun() }, onBackToMenu)
        }

        // Settings
        if (showSettings) {
            SettingsPanel(uiState.controlSettings, { viewModel.setSensitivityMultiplier(it) }, { viewModel.setCurve(it) }, { viewModel.setInvertTilt(it) },
                { viewModel.setTiltResponse(it) }, { viewModel.setLeafMomentum(it) }, { viewModel.setHitboxShrink(it) }, { viewModel.setDeadZone(it) },
                { viewModel.setInstantSnap(it) }, { viewModel.setControlMode(it) }, { viewModel.applyPreset(it) },
                onRequestCalibrate, { showSettings = false }, { viewModel.resetSettings() })
        }

        // Debug
        if (BuildConfig.SHOW_DEBUG_OVERLAY && showDebug) DebugPanel(Modifier.align(Alignment.BottomStart), uiState.debugTelemetry)

        // Score + Drops
        AnimatedVisibility(visible = !showSettings && uiState.phase == GamePhase.PLAYING) {
            ScoreChip(Modifier.align(Alignment.BottomCenter), uiState.score, uiState.highScore, uiState.riverDrops)
        }

        // Boost meter
        AnimatedVisibility(visible = uiState.boostActive) {
            BoostMeter(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 90.dp), uiState.boostActive, uiState.boostTimeRemaining)
        }
    }
}

// ── Draw: 5-layer parallax background ────────────────────────────────────────

private fun DrawScope.drawParallaxBackground(ui: GameUiState, vp: ViewportMapping) {
    val colors = ThemeColors.waterGradient(ui.riverTheme, ui.dayPhase)
    val baseOffset = ui.runTime * 50f
    for (layer in 0 until GameConstants.PARALLAX_LAYER_COUNT) {
        val speed = GameConstants.PARALLAX_SPEEDS[layer]
        val scrollY = (baseOffset * speed) % size.height
        val alpha = 0.15f + layer * 0.17f
        val layerColor = if (layer < colors.size) colors[layer.coerceAtMost(colors.size - 1)] else colors.last()
        // Draw scrolling gradient band
        val bandHeight = size.height / GameConstants.PARALLAX_LAYER_COUNT
        val yStart = (layer * bandHeight + scrollY) % (size.height + bandHeight) - bandHeight
        drawRect(
            color = layerColor.copy(alpha = alpha),
            topLeft = Offset(0f, yStart),
            size = Size(size.width, bandHeight * 1.5f)
        )
        // Animated wave line per layer
        val wavePath = Path()
        val waveY = yStart + bandHeight * 0.5f
        wavePath.moveTo(0f, waveY)
        for (x in 0..size.width.toInt() step 20) {
            val wx = x.toFloat()
            val sinVal = sin((wx * 0.02f + ui.runTime * speed * 2f).toDouble()).toFloat()
            wavePath.lineTo(wx, waveY + sinVal * (8f + layer * 3f))
        }
        drawPath(wavePath, color = layerColor.copy(alpha = alpha * 0.5f), style = Stroke(1.5f))
    }
    // Base gradient fill
    drawRect(brush = Brush.verticalGradient(colors), size = size, alpha = 0.6f)
}

// ── Draw: procedural light rays ──────────────────────────────────────────────

private fun DrawScope.drawLightRays(time: Float, dayPhase: DayPhase, vp: ViewportMapping) {
    if (dayPhase == DayPhase.NIGHT) return
    val alphaBase = when (dayPhase) {
        DayPhase.DAWN -> GameConstants.LIGHT_RAY_MAX_ALPHA * 0.7f
        DayPhase.DAY  -> GameConstants.LIGHT_RAY_MAX_ALPHA
        DayPhase.DUSK -> GameConstants.LIGHT_RAY_MAX_ALPHA * 0.5f
        else -> 0f
    }
    for (i in 0 until GameConstants.LIGHT_RAY_COUNT) {
        val angle = -15f + i * 8f + sin(time * 0.3f + i * 1.2f).toFloat() * 5f
        val rayWidth = 40f + sin(time * 0.5f + i * 0.8f).toFloat() * 20f
        val x = size.width * (0.1f + i.toFloat() / GameConstants.LIGHT_RAY_COUNT * 0.8f)
        val rayColor = Color(0xFFFFEE88).copy(alpha = alphaBase * (0.6f + sin(time + i.toFloat()).toFloat() * 0.4f))
        rotate(angle, pivot = Offset(x, 0f)) {
            drawRect(rayColor, topLeft = Offset(x - rayWidth / 2, -50f), size = Size(rayWidth, size.height * 1.2f))
        }
    }
}

// ── Draw: narrow channel walls ───────────────────────────────────────────────

private fun DrawScope.drawNarrowChannel(offset: Float, vp: ViewportMapping) {
    val wallColor = Color(0xFF1A3020).copy(alpha = 0.85f)
    val px = offset * vp.scale
    drawRect(wallColor, topLeft = Offset(0f, 0f), size = Size(px, size.height))
    drawRect(wallColor, topLeft = Offset(size.width - px, 0f), size = Size(px, size.height))
}

// ── Draw: particle trail ─────────────────────────────────────────────────────

private fun DrawScope.drawTrailParticles(particles: List<TrailParticle>, skin: LeafSkin, vp: ViewportMapping) {
    val (c1, _) = ThemeColors.leafColors(skin)
    particles.forEach { p ->
        val pos = logicalToScreen(PointF(p.x, p.y), vp)
        val r = p.size * vp.scale * p.alpha
        drawCircle(c1.copy(alpha = p.alpha * 0.6f), r, Offset(pos.x, pos.y))
    }
}

// ── Draw: water ripples ──────────────────────────────────────────────────────

private fun DrawScope.drawWaterRipples(ui: GameUiState, vp: ViewportMapping) {
    val rippleColor = ThemeColors.rippleColor(ui.riverTheme)
    val cx = size.width * 0.5f; val cy = size.height * 0.65f
    repeat(4) { i ->
        val t = i / 4f
        val radius = (GameConstants.VIRTUAL_WIDTH * (0.35f + 0.35f * t)) * vp.scale
        val phase = ui.runTime * 0.5f + i * 0.3f
        val animated = radius + sin(phase.toDouble()).toFloat() * 20f * vp.scale
        drawCircle(rippleColor.copy(alpha = 0.06f - t * 0.01f), animated,
            Offset(vp.offsetX + cx, vp.offsetY + cy), style = Stroke(2.4f * vp.scale))
    }
}

// ── Draw: leaf with breathing + lean ─────────────────────────────────────────

private fun DrawScope.drawLeaf(ui: GameUiState, vp: ViewportMapping, reusablePath: Path) {
    val vs = GameConstants.LEAF_VISUAL_SCALE * ui.leafBreathScale
    val wPx = GameConstants.LEAF_WIDTH * vp.scale * vs
    val hPx = GameConstants.LEAF_HEIGHT * vp.scale * vs
    val tl = logicalToScreen(PointF(ui.leafX - (GameConstants.LEAF_WIDTH * 0.5f) * vs, ui.leafY - (GameConstants.LEAF_HEIGHT * 0.5f) * vs), vp)
    val center = Offset(tl.x + wPx * 0.5f, tl.y + hPx * 0.5f)
    val (fillColor, strokeColor) = ThemeColors.leafColors(ui.leafSkin)

    withTransform({
        rotate(ui.leafLeanAngle, center)
    }) {
        // Shadow
        drawOval(Color(0x33000000), Offset(center.x - wPx * 0.45f, center.y + hPx * 0.35f), Size(wPx * 0.9f, hPx * 0.5f))
        // Leaf shape
        reusablePath.reset()
        reusablePath.moveTo(tl.x + wPx * 0.5f, tl.y)
        reusablePath.quadraticBezierTo(tl.x + wPx * 0.98f, tl.y + hPx * 0.35f, tl.x + wPx * 0.5f, tl.y + hPx)
        reusablePath.quadraticBezierTo(tl.x + wPx * 0.02f, tl.y + hPx * 0.35f, tl.x + wPx * 0.5f, tl.y)
        // Boost glow
        if (ui.boostActive) drawCircle(Color(0x448CFFF1), wPx, center)
        drawPath(reusablePath, fillColor)
        drawPath(reusablePath, strokeColor.copy(alpha = 0.4f), style = Stroke(2f * vp.scale))
        // Vein
        drawLine(Color(0x44006B3C), Offset(center.x, tl.y + hPx * 0.15f), Offset(center.x, tl.y + hPx * 0.85f), strokeWidth = 2f)
        // Side veins
        for (i in 1..3) {
            val vy = tl.y + hPx * (0.2f + i * 0.18f)
            drawLine(Color(0x33006B3C), Offset(center.x, vy), Offset(center.x - wPx * 0.25f, vy + hPx * 0.06f), strokeWidth = 1.2f)
            drawLine(Color(0x33006B3C), Offset(center.x, vy), Offset(center.x + wPx * 0.25f, vy + hPx * 0.06f), strokeWidth = 1.2f)
        }
    }
}

// ── Draw: obstacles with procedural textures ─────────────────────────────────

private fun DrawScope.drawObstacles(ui: GameUiState, vp: ViewportMapping) {
    ui.obstacles.forEach { o ->
        val tl = logicalToScreen(PointF(o.x - o.width * 0.5f, o.y - o.height * 0.5f), vp)
        val sz = Size(o.width * vp.scale, o.height * vp.scale)
        when (o.kind) {
            ObstacleKind.LOG -> drawHurdleTextured(tl, sz, o.hurdleStyle, o.warningHighlight, vp)
            ObstacleKind.ROCK -> {
                val rPx = min(sz.width, sz.height) * 0.5f
                val ctr = Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f)
                drawCircle(Color(0xFF3B4C5B), rPx, ctr)
                // Stone cracks
                for (i in 0..4) {
                    val angle = i * 72f + 15f
                    val rad = angle * PI.toFloat() / 180f
                    val endX = ctr.x + cos(rad) * rPx * 0.7f
                    val endY = ctr.y + sin(rad) * rPx * 0.7f
                    drawLine(Color(0x664B5F6D), ctr, Offset(endX, endY), strokeWidth = 1.5f * vp.scale)
                }
                drawCircle(Color(0x3320272E), rPx, ctr, style = Stroke(4f * vp.scale))
            }
        }
    }
}

/** 4 procedural hurdle textures: WOOD, STONE, ICE, LILY_PAD */
private fun DrawScope.drawHurdleTextured(tl: PointF, sz: Size, style: HurdleStyle, warning: Float, vp: ViewportMapping) {
    val cr = CornerRadius(12f * vp.scale)
    val offset = Offset(tl.x, tl.y)
    when (style) {
        HurdleStyle.WOOD -> {
            drawRoundRect(Color(0xFF5E3B2C), offset, sz, cr)
            // Wood grain lines
            for (i in 0..((sz.height / (8f * vp.scale)).toInt())) {
                val y = tl.y + i * 8f * vp.scale
                drawLine(Color(0x33000000), Offset(tl.x + 4f, y), Offset(tl.x + sz.width - 4f, y), strokeWidth = 0.8f)
            }
            // Knots
            drawCircle(Color(0x44000000), 4f * vp.scale, Offset(tl.x + sz.width * 0.3f, tl.y + sz.height * 0.4f))
            drawCircle(Color(0x44000000), 3f * vp.scale, Offset(tl.x + sz.width * 0.7f, tl.y + sz.height * 0.7f))
        }
        HurdleStyle.STONE -> {
            drawRoundRect(Color(0xFF5A6570), offset, sz, cr)
            // Cracks
            val cx = tl.x + sz.width * 0.5f; val cy = tl.y + sz.height * 0.5f
            for (i in 0..3) {
                val a = i * 90f + 20f; val r = a * PI.toFloat() / 180f
                val ex = cx + cos(r) * sz.width * 0.35f; val ey = cy + sin(r) * sz.height * 0.35f
                drawLine(Color(0x88000000), Offset(cx, cy), Offset(ex, ey), strokeWidth = 1.5f * vp.scale)
            }
            drawRoundRect(Color(0x22000000), offset, sz, cr, style = Stroke(2f * vp.scale))
        }
        HurdleStyle.ICE -> {
            drawRoundRect(Color(0xAA80D0FF), offset, sz, cr)
            // Refraction highlights
            drawRoundRect(Color(0x44FFFFFF), Offset(tl.x + 3f, tl.y + 3f), Size(sz.width - 6f, sz.height * 0.3f), cr)
            // Internal fracture lines
            drawLine(Color(0x33FFFFFF), Offset(tl.x + sz.width * 0.2f, tl.y + sz.height * 0.3f), Offset(tl.x + sz.width * 0.8f, tl.y + sz.height * 0.7f), strokeWidth = 1f)
            drawLine(Color(0x33FFFFFF), Offset(tl.x + sz.width * 0.6f, tl.y + sz.height * 0.2f), Offset(tl.x + sz.width * 0.3f, tl.y + sz.height * 0.8f), strokeWidth = 1f)
            drawRoundRect(Color(0x44A0E0FF), offset, sz, cr, style = Stroke(2f * vp.scale))
        }
        HurdleStyle.LILY_PAD -> {
            // Circular lily pad shape
            val r = min(sz.width, sz.height) * 0.48f
            val cx = tl.x + sz.width * 0.5f; val cy = tl.y + sz.height * 0.5f
            drawCircle(Color(0xFF2D8040), r, Offset(cx, cy))
            // Radial veins
            for (i in 0..7) {
                val angle = i * 45f * PI.toFloat() / 180f
                drawLine(Color(0x44004020), Offset(cx, cy), Offset(cx + cos(angle) * r * 0.85f, cy + sin(angle) * r * 0.85f), strokeWidth = 1.2f)
            }
            // Center dot + notch
            drawCircle(Color(0xFF3A9050), r * 0.15f, Offset(cx, cy))
            drawLine(Color(0xFF0A3020), Offset(cx, cy), Offset(cx + r, cy), strokeWidth = 3f * vp.scale)
        }
    }
    // Warning glow
    if (warning > 0f) drawRoundRect(Color(0xFFFFC107).copy(alpha = warning * 0.6f), offset, sz, cr, style = Stroke(3f * vp.scale))
}

// ── Draw: power-up collectibles ──────────────────────────────────────────────

private fun DrawScope.drawPowerUpCollectibles(collectibles: List<PowerUpCollectible>, vp: ViewportMapping) {
    collectibles.forEach { p ->
        val c = logicalToScreen(PointF(p.x, p.y), vp)
        val r = p.radius * vp.scale
        val color = when (p.type) {
            PowerUpType.SHIELD -> Color(0xFF4488FF)
            PowerUpType.SPEED_BOOST -> Color(0xFFFFCC00)
            PowerUpType.MAGNET -> Color(0xFFFF4488)
            PowerUpType.SLOW_TIME -> Color(0xFF88CCFF)
            PowerUpType.DOUBLE_POINTS -> Color(0xFFFFAA00)
        }
        drawCircle(color.copy(alpha = 0.3f), r * 1.3f, Offset(c.x, c.y))
        drawCircle(color, r, Offset(c.x, c.y))
        drawCircle(Color.White.copy(alpha = 0.5f), r * 0.4f, Offset(c.x, c.y))
    }
}

// ── Draw: boosts ─────────────────────────────────────────────────────────────

private fun DrawScope.drawBoosts(boosts: List<BoostState>, vp: ViewportMapping) {
    boosts.forEach { b ->
        val c = logicalToScreen(PointF(b.x, b.y), vp)
        val r = b.radius * vp.scale
        drawCircle(Color(0x3385FFF6), r * 1.4f, Offset(c.x, c.y))
        drawCircle(Color(0xFF6CF4FF), r, Offset(c.x, c.y), style = Stroke(6f * vp.scale))
        drawCircle(Color(0xFF48D9FF), r * 0.35f, Offset(c.x, c.y))
    }
}

// ── Game Over Screen with Confetti + Drops + Suggestion ──────────────────────

@Composable
private fun GameOverScreen(
    score: Int, highScore: Int, level: Int, obstaclesCleared: Int,
    dropsEarned: Int, suggestion: String?,
    onNewRun: () -> Unit, onBackToMenu: () -> Unit
) {
    val isNewHigh = score >= highScore && score > 0
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        if (isNewHigh) ConfettiAnimation(Modifier.fillMaxSize())
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (isNewHigh) "New High Score!" else "Game Over", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = if (isNewHigh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text("$score", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 56.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    StatColumn("Level", "$level"); StatColumn("Cleared", "$obstaclesCleared"); StatColumn("Best", "$highScore")
                }
                // River Drops earned
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDCA7", fontSize = 20.sp); Spacer(Modifier.width(8.dp))
                        Text("+$dropsEarned River Drops", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                // Sensitivity suggestion
                suggestion?.let {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                        Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Button(onClick = onNewRun, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Filled.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Play Again")
                }
                OutlinedButton(onClick = onBackToMenu, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Filled.Home, null); Spacer(Modifier.width(8.dp)); Text("Menu")
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConfettiAnimation(modifier: Modifier = Modifier) {
    val particles = remember {
        List(GameConstants.CONFETTI_COUNT) {
            ConfettiParticle(Random.nextFloat(), Random.nextFloat() * -1f, 0.2f + Random.nextFloat() * 0.5f, Random.nextFloat() * 360f,
                listOf(Color(0xFF26C596), Color(0xFFFFD54F), Color(0xFF42A5F5), Color(0xFFEF5350), Color(0xFFAB47BC)).random(), 6f + Random.nextFloat() * 10f)
        }
    }
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animProgress.animateTo(1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)) }
    Canvas(modifier) {
        val t = animProgress.value
        particles.forEach { p ->
            val px = p.x * size.width + sin(p.angle + t * 6f) * 30f
            val py = ((p.y + t * p.speed * 2f) % 1.2f) * size.height
            drawCircle(p.color, p.size, Offset(px, py))
        }
    }
}

private data class ConfettiParticle(val x: Float, val y: Float, val speed: Float, val angle: Float, val color: Color, val size: Float)

// ── Power-up HUD ─────────────────────────────────────────────────────────────

@Composable
private fun PowerUpHud(modifier: Modifier, powerUps: List<ActivePowerUp>) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        powerUps.forEach { pu ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pu.type.icon, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(pu.type.displayName, style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(progress = { pu.progress }, Modifier.width(60.dp).height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color(0x33000000))
                    }
                }
            }
        }
    }
}

// ── River event banner ───────────────────────────────────────────────────────

@Composable
private fun RiverEventBanner(modifier: Modifier, event: ActiveRiverEvent) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(event.type.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            LinearProgressIndicator(progress = { event.progress }, Modifier.width(80.dp).height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color(0x33000000))
        }
    }
}

// ── UI: HUD, Pause, Tutorial, Settings, Score, Boost, Debug ─────────────────

@Composable
private fun BoxScope.IconHud(modifier: Modifier, iconScale: Float, isRunning: Boolean, soundEnabled: Boolean, onPauseToggle: () -> Unit, onSettingsRequested: () -> Unit, onSoundToggle: () -> Unit) {
    val ts = (48f * iconScale).dp; val is2 = (28f * iconScale).dp
    Row(modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        FilledIconButton(onPauseToggle, Modifier.size(ts), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))) {
            Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(is2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onSettingsRequested, Modifier.size(ts)) { Icon(Icons.Filled.Tune, "Settings", Modifier.size(is2)) }
            IconButton(onSoundToggle, Modifier.size(ts)) { Icon(if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, "Sound", Modifier.size(is2)) }
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onSettings: () -> Unit, onBackToMenu: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            LargeIconButton(Icons.Filled.PlayArrow, onResume)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { LargeIconButton(Icons.Filled.RestartAlt, onRestart); LargeIconButton(Icons.Filled.Settings, onSettings) }
            LargeIconButton(Icons.Filled.Home, onBackToMenu)
        }
    }
}

@Composable
private fun TutorialOverlay(modifier: Modifier = Modifier, controlMode: ControlMode) {
    Card(modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (controlMode == ControlMode.TOUCH) Icons.Filled.TouchApp else Icons.Filled.Tune, null)
            Text(when (controlMode) {
                ControlMode.TOUCH -> "Drag to steer the leaf."
                ControlMode.TAP -> "Tap left/right to steer."
                else -> "Tilt device to steer."
            }, style = MaterialTheme.typography.bodyLarge)
            Text("Tap anywhere to dismiss", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsPanel(
    settings: ControlSettings,
    onSensitivityChanged: (Float) -> Unit, onCurveChanged: (SensitivityCurve) -> Unit,
    onInvertChanged: (Boolean) -> Unit, onStiffnessChanged: (Float) -> Unit,
    onDampingChanged: (Float) -> Unit, onHitboxChanged: (Float) -> Unit,
    onDeadZoneChanged: (Float) -> Unit, onInstantSnapChanged: (Boolean) -> Unit,
    onControlModeChanged: (ControlMode) -> Unit, onPresetSelected: (SensitivityPreset) -> Unit,
    onCalibrate: () -> Unit, onClose: () -> Unit, onReset: () -> Unit
) {
    Surface(Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(28.dp), tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            // Control mode
            SettingsSection("Control Mode", "Choose touch drag, tap-steer, or gyroscope.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (mode in ControlMode.entries) {
                        AssistChip(onClick = { onControlModeChanged(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (settings.controlMode == mode) { { Icon(if (mode == ControlMode.TOUCH) Icons.Filled.TouchApp else Icons.Filled.Tune, null, Modifier.size(16.dp)) } } else null,
                            colors = AssistChipDefaults.assistChipColors(containerColor = if (settings.controlMode == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
            // Presets
            SettingsSection("Sensitivity Presets") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in SensitivityPreset.entries) {
                        AssistChip(onClick = { onPresetSelected(p) },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = if (settings.preset == p) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
            SettingsSection("Motion Feel", "Dial in how reactive steering feels.") {
                SettingsSlider("Sensitivity ${settings.sensitivityMultiplier.fmt(1)}", settings.sensitivityMultiplier, 0.2f..6f, onSensitivityChanged)
                SettingsSlider("Tilt Response ${settings.stiffness.fmt(0)}", settings.stiffness, 4f..32f, onStiffnessChanged)
                SettingsSlider("Leaf Momentum ${settings.damping.fmt(2)}", settings.damping, 0.7f..0.98f, onDampingChanged)
            }
            SettingsSection("Survivability") {
                SettingsSlider("Hitbox ${settings.hitboxShrink.fmt(2)}", settings.hitboxShrink, 0.4f..0.95f, onHitboxChanged)
                SettingsSlider("Dead Zone ${settings.deadZone.fmt(3)}", settings.deadZone, 0f..0.08f, onDeadZoneChanged)
            }
            SettingsSection("Advanced") {
                CurveSelector(settings.curve, onCurveChanged)
                LabeledSwitch("Invert horizontal tilt", settings.invertTilt, onInvertChanged)
                LabeledSwitch("Instant snap", settings.instantSnap, onInstantSnapChanged)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCalibrate) { Text("Calibrate") }
                OutlinedButton(onClick = onReset) { Text("Reset") }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = onClose) { Icon(Icons.Filled.PlayArrow, null) }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Text("Tip: EXPONENTIAL curve + moderate damping is great for precise dodging.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
            }
        }
    }
}

@Composable
private fun DebugPanel(modifier: Modifier, t: DebugTelemetry) {
    ElevatedCard(modifier.padding(8.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("DEBUG", style = MaterialTheme.typography.labelLarge, color = Color.Red)
            Text("FPS: ${t.fps} | dt: ${t.deltaTime.fmt(3)}", fontSize = 11.sp)
            Text("Mem: ${t.memoryUsedMb.fmt(1)} MB", fontSize = 11.sp)
            Text("Obstacles: ${t.activeObstacles} | Particles: ${t.activeParticles}", fontSize = 11.sp)
            Text("PowerUps: ${t.activePowerUps} | Event: ${t.currentEvent}", fontSize = 11.sp)
            Text("Adaptive: ${t.adaptiveDifficulty.fmt(2)} | ${t.dayPhase}", fontSize = 11.sp)
            Text("Audio layers: ${t.audioLayers} | Control: ${t.controlMode}", fontSize = 11.sp)
            Text("Tilt: ${t.rawTilt.fmt(3)} -> ${t.targetX.fmt(0)} | Leaf: ${t.leafX.fmt(0)}", fontSize = 11.sp)
        }
    }
}

@Composable
private fun ScoreChip(modifier: Modifier, score: Int, highScore: Int, drops: Int) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Leaderboard, null)
            Column {
                Text("Score $score", fontWeight = FontWeight.Bold)
                Text("Best $highScore", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Text("\uD83D\uDCA7$drops", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BoostMeter(modifier: Modifier, boostActive: Boolean, remaining: Float) {
    val progress = if (boostActive) (remaining / GameConstants.BOOST_DURATION).coerceIn(0f, 1f) else 0f
    ElevatedCard(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (boostActive) "Boost!" else "Boost ready", style = MaterialTheme.typography.labelLarge, color = if (boostActive) Color(0xFF5BFFE3) else MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(), color = if (boostActive) Color(0xFF5BFFE3) else MaterialTheme.colorScheme.outlineVariant, trackColor = Color(0x33000000))
        }
    }
}

@Composable private fun LargeIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(onClick, modifier.size(64.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(icon, null, tint = Color.White) }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                    thumbSize = androidx.compose.ui.unit.DpSize(28.dp, 28.dp)
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            )
        )
    }
}
@Composable private fun CurveSelector(current: SensitivityCurve, onCurveChange: (SensitivityCurve) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (c in SensitivityCurve.entries) {
            AssistChip(onClick = { onCurveChange(c) }, label = { Text(c.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon = if (current == c) { { Icon(Icons.Filled.Tune, null, Modifier.size(16.dp)) } } else null,
                colors = AssistChipDefaults.assistChipColors(containerColor = if (current == c) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
@Composable private fun SettingsSection(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); description?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
        content()
    }
}
@Composable private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
    }
}

private fun Float.fmt(d: Int): String = "%.${d}f".format(this)
