package com.example.hydraleaf

object GameConstants {
    // ── Virtual coordinate system ────────────────────────────────────────────
    const val VIRTUAL_WIDTH  = 1080f
    const val VIRTUAL_HEIGHT = 1920f

    // ── Leaf ─────────────────────────────────────────────────────────────────
    const val LEAF_WIDTH  = 140f
    const val LEAF_HEIGHT = 210f
    const val LEAF_BASE_Y = 1500f
    const val LEAF_VISUAL_SCALE = 1.08f
    const val LEAF_VERTICAL_MIN = LEAF_BASE_Y - 320f
    const val LEAF_VERTICAL_MAX = LEAF_BASE_Y + 220f
    const val LEAF_VERTICAL_RANGE = (LEAF_VERTICAL_MAX - LEAF_VERTICAL_MIN) * 0.5f
    /** Leaf breathing amplitude (scale oscillation) */
    const val LEAF_BREATH_AMPLITUDE = 0.04f
    /** Leaf breathing period in seconds */
    const val LEAF_BREATH_PERIOD = 2.0f
    /** Max lean angle in degrees driven by horizontal velocity */
    const val LEAF_MAX_LEAN_DEG = 18f

    // ── Obstacle (log) ──────────────────────────────────────────────────────
    const val OBSTACLE_MIN_WIDTH  = LEAF_WIDTH * 1.6f
    const val OBSTACLE_MAX_WIDTH  = LEAF_WIDTH * 2.8f
    const val OBSTACLE_HEIGHT     = 80f
    const val OBSTACLE_MIN_SPEED  = 220f
    const val OBSTACLE_MAX_SPEED  = 420f
    const val OBSTACLE_SPAWN_INTERVAL = 1.2f

    // ── Levels ──────────────────────────────────────────────────────────────
    const val HURDLES_PER_LEVEL = 6
    const val LEVEL_SPEED_BONUS = 20f

    // ── Warning indicator ───────────────────────────────────────────────────
    const val WARNING_ZONE_RATIO = 0.18f

    // ── Rock obstacles ──────────────────────────────────────────────────────
    const val ROCK_MIN_WIDTH  = LEAF_WIDTH * 1.5f
    const val ROCK_MAX_WIDTH  = LEAF_WIDTH * 2.4f
    const val ROCK_HEIGHT     = LEAF_HEIGHT * 1.3f
    const val ROCK_SPEED_BONUS = 60f
    const val ROCK_SPAWN_CHANCE = 0.35f

    // ── Hitbox ──────────────────────────────────────────────────────────────
    const val BASE_HITBOX_SCALE  = 0.78f
    const val BOOST_HITBOX_SCALE = 0.55f

    // ── Boost collectible ───────────────────────────────────────────────────
    const val BOOST_SPAWN_INTERVAL  = 7f
    const val BOOST_SPAWN_VARIATION = 4f
    const val BOOST_DRIFT_SPEED     = 140f
    const val BOOST_RADIUS          = 95f
    const val BOOST_DURATION        = 3.5f

    // ── Safe-path hurdle ────────────────────────────────────────────────────
    const val SAFE_GAP_MIN_WIDTH    = 160f
    const val LEVEL1_MIN_ROW_SPACING = 320f
    const val MIN_ROW_SPACING       = 420f
    const val MAX_OBSTACLES_PER_ROW = 2

    // ── Difficulty scaling ──────────────────────────────────────────────────
    const val SPEED_HARD_FLOOR       = 0.5f
    const val SPAWN_INTERVAL_FLOOR   = 0.45f
    const val SPEED_CAP              = 680f

    // ── Countdown / death ───────────────────────────────────────────────────
    const val COUNTDOWN_SECONDS   = 3
    const val DEAD_PHASE_DURATION = 1.0f

    // ── Confetti ────────────────────────────────────────────────────────────
    const val CONFETTI_COUNT = 80

    // ── Power-ups ───────────────────────────────────────────────────────────
    const val POWERUP_SPAWN_INTERVAL     = 12f
    const val POWERUP_SPAWN_VARIATION    = 5f
    const val POWERUP_RADIUS             = 60f
    const val POWERUP_DRIFT_SPEED        = 160f
    const val SHIELD_FLASH_RADIUS        = 200f
    const val MAGNET_PULL_RADIUS         = 300f
    const val SLOW_TIME_FACTOR           = 0.45f
    const val SPEED_BOOST_MULTIPLIER     = 1.5f

    // ── River events ────────────────────────────────────────────────────────
    const val EVENT_MIN_INTERVAL    = 18f
    const val EVENT_MAX_INTERVAL    = 35f
    const val NARROW_CHANNEL_WIDTH  = VIRTUAL_WIDTH * 0.55f
    const val FOG_MAX_ALPHA         = 0.72f
    const val SPEED_SURGE_MULTIPLIER = 1.6f
    const val CALM_SPEED_MULTIPLIER  = 0.5f

    // ── Currency ────────────────────────────────────────────────────────────
    /** River Drops earned per obstacle cleared */
    const val DROPS_PER_CLEAR = 2
    /** Bonus drops for surviving a river event */
    const val DROPS_PER_EVENT_SURVIVE = 15
    /** Drops per power-up collected */
    const val DROPS_PER_POWERUP = 5

    // ── Parallax ────────────────────────────────────────────────────────────
    val PARALLAX_SPEEDS = floatArrayOf(0.15f, 0.30f, 0.50f, 0.75f, 1.0f)
    const val PARALLAX_LAYER_COUNT = 5

    // ── Particle trail ──────────────────────────────────────────────────────
    const val TRAIL_MAX_PARTICLES  = 40
    const val TRAIL_SPAWN_RATE     = 0.03f
    const val TRAIL_PARTICLE_LIFE  = 0.8f
    const val TRAIL_PARTICLE_SIZE  = 8f

    // ── Day/night cycle ─────────────────────────────────────────────────────
    /** Full day cycle period in real seconds of cumulative playtime */
    const val DAY_CYCLE_PERIOD = 300f  // 5 min
    /** Dawn: 0-0.15, Day: 0.15-0.5, Dusk: 0.5-0.65, Night: 0.65-1.0 */
    const val DAWN_END  = 0.15f
    const val DAY_END   = 0.50f
    const val DUSK_END  = 0.65f

    // ── Light rays ──────────────────────────────────────────────────────────
    const val LIGHT_RAY_COUNT    = 6
    const val LIGHT_RAY_MAX_ALPHA = 0.12f

    // ── Adaptive difficulty ─────────────────────────────────────────────────
    const val ADAPTIVE_WINDOW       = 15       // last N obstacles for dodge rate
    const val ADAPTIVE_EASY_THRESH  = 0.85f    // >85% dodge rate → harder
    const val ADAPTIVE_HARD_THRESH  = 0.45f    // <45% dodge rate → easier
    const val ADAPTIVE_SPEED_STEP   = 0.05f
    const val ADAPTIVE_SPAWN_STEP   = 0.05f

    // ── Performance ─────────────────────────────────────────────────────────
    const val MEMORY_BUDGET_BYTES = 32L * 1024 * 1024  // 32 MB
    const val THERMAL_THROTTLE_FPS = 30
    const val TARGET_FPS = 60

    // ── Audio ───────────────────────────────────────────────────────────────
    const val AUDIO_SAMPLE_RATE = 22050
    /** Pentatonic frequencies: C4 D4 E4 G4 A4 */
    val PENTATONIC_FREQS = floatArrayOf(261.63f, 293.66f, 329.63f, 392.00f, 440.00f)
    const val DODGE_TONE_DURATION   = 0.12f
    const val COLLECT_TONE_DURATION = 0.18f
    const val DEATH_TONE_DURATION   = 0.35f
    /** Base water-rush frequency; pitch shifts with speed */
    const val WATER_RUSH_BASE_FREQ  = 120f
}
