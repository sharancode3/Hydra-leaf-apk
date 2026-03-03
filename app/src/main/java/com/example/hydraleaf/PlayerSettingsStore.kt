package com.example.hydraleaf

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerSettingsStore(private val dataStore: DataStore<Preferences>) {

    // ── Settings Flow ────────────────────────────────────────────────────────
    val settingsFlow: Flow<ControlSettings> = dataStore.data.map { p ->
        ControlSettings(
            sensitivityMultiplier = p[SENSITIVITY_KEY] ?: ControlDefaults.SENSITIVITY,
            curve = p[CURVE_KEY]?.let { runCatching { SensitivityCurve.valueOf(it) }.getOrNull() } ?: SensitivityCurve.EXPONENTIAL,
            invertTilt = p[INVERT_KEY] ?: false,
            stiffness = p[STIFFNESS_KEY] ?: ControlDefaults.STIFFNESS,
            damping = p[DAMPING_KEY] ?: ControlDefaults.DAMPING,
            deadZone = p[DEAD_ZONE_KEY] ?: ControlDefaults.DEAD_ZONE,
            calibrationOffset = p[CALIBRATION_KEY] ?: 0f,
            hitboxShrink = p[HITBOX_KEY] ?: ControlDefaults.HITBOX,
            instantSnap = p[SNAP_KEY] ?: false,
            iconScale = p[ICON_SCALE_KEY] ?: ControlDefaults.ICON_SCALE,
            controlMode = p[CONTROL_MODE_KEY]?.let { runCatching { ControlMode.valueOf(it) }.getOrNull() } ?: ControlMode.GYROSCOPE,
            preset = p[PRESET_KEY]?.let { runCatching { SensitivityPreset.valueOf(it) }.getOrNull() } ?: SensitivityPreset.BALANCED,
            accessibilityMode = p[ACCESSIBILITY_KEY]?.let { runCatching { AccessibilityMode.valueOf(it) }.getOrNull() } ?: AccessibilityMode.STANDARD
        )
    }

    val highScoreFlow: Flow<Int>     = dataStore.data.map { it[HIGH_SCORE_KEY] ?: 0 }
    val tutorialSeenFlow: Flow<Boolean> = dataStore.data.map { it[TUTORIAL_SEEN_KEY] ?: false }
    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { it[SOUND_KEY] ?: true }

    // ── Currency & shop ──────────────────────────────────────────────────────
    val riverDropsFlow: Flow<Int>   = dataStore.data.map { it[DROPS_KEY] ?: 0 }
    val ownedSkinsFlow: Flow<Set<String>> = dataStore.data.map {
        it[OWNED_SKINS_KEY]?.split(",")?.filter(String::isNotBlank)?.toSet() ?: setOf(LeafSkin.CLASSIC.name)
    }
    val activeLeafSkinFlow: Flow<LeafSkin> = dataStore.data.map {
        it[ACTIVE_SKIN_KEY]?.let { s -> runCatching { LeafSkin.valueOf(s) }.getOrNull() } ?: LeafSkin.CLASSIC
    }
    val ownedThemesFlow: Flow<Set<String>> = dataStore.data.map {
        it[OWNED_THEMES_KEY]?.split(",")?.filter(String::isNotBlank)?.toSet() ?: setOf(RiverTheme.FOREST.name)
    }
    val activeRiverThemeFlow: Flow<RiverTheme> = dataStore.data.map {
        it[ACTIVE_THEME_KEY]?.let { s -> runCatching { RiverTheme.valueOf(s) }.getOrNull() } ?: RiverTheme.FOREST
    }

    // ── Cumulative playtime (for day/night cycle) ────────────────────────────
    val totalPlaytimeFlow: Flow<Float> = dataStore.data.map { it[PLAYTIME_KEY] ?: 0f }

    // ── Daily challenge ──────────────────────────────────────────────────────
    val dailyChallengeDay: Flow<Int> = dataStore.data.map { it[DAILY_DAY_KEY] ?: 0 }
    val dailyChallengeCompleted: Flow<Boolean> = dataStore.data.map { it[DAILY_DONE_KEY] ?: false }

    // ── Basic setters ────────────────────────────────────────────────────────
    suspend fun setSensitivityMultiplier(v: Float) { dataStore.edit { it[SENSITIVITY_KEY] = v } }
    suspend fun setCurve(v: SensitivityCurve)      { dataStore.edit { it[CURVE_KEY] = v.name } }
    suspend fun setInvertTilt(v: Boolean)           { dataStore.edit { it[INVERT_KEY] = v } }
    suspend fun setStiffness(v: Float)              { dataStore.edit { it[STIFFNESS_KEY] = v } }
    suspend fun setDamping(v: Float)                { dataStore.edit { it[DAMPING_KEY] = v } }
    suspend fun setDeadZone(v: Float)               { dataStore.edit { it[DEAD_ZONE_KEY] = v } }
    suspend fun setCalibrationOffset(v: Float)      { dataStore.edit { it[CALIBRATION_KEY] = v } }
    suspend fun setHitboxShrink(v: Float)           { dataStore.edit { it[HITBOX_KEY] = v } }
    suspend fun setInstantSnap(v: Boolean)          { dataStore.edit { it[SNAP_KEY] = v } }
    suspend fun setIconScale(v: Float)              { dataStore.edit { it[ICON_SCALE_KEY] = v } }
    suspend fun setHighScore(v: Int)                { dataStore.edit { it[HIGH_SCORE_KEY] = v } }
    suspend fun setTutorialSeen(v: Boolean)         { dataStore.edit { it[TUTORIAL_SEEN_KEY] = v } }
    suspend fun setSoundEnabled(v: Boolean)         { dataStore.edit { it[SOUND_KEY] = v } }
    suspend fun setControlMode(v: ControlMode)      { dataStore.edit { it[CONTROL_MODE_KEY] = v.name } }
    suspend fun setPreset(v: SensitivityPreset)     { dataStore.edit { it[PRESET_KEY] = v.name } }
    suspend fun setAccessibilityMode(v: AccessibilityMode) { dataStore.edit { it[ACCESSIBILITY_KEY] = v.name } }

    // ── Currency ─────────────────────────────────────────────────────────────
    suspend fun addRiverDrops(amount: Int) {
        dataStore.edit { it[DROPS_KEY] = (it[DROPS_KEY] ?: 0) + amount }
    }
    suspend fun spendRiverDrops(amount: Int): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val cur = prefs[DROPS_KEY] ?: 0
            if (cur >= amount) { prefs[DROPS_KEY] = cur - amount; success = true }
        }
        return success
    }

    // ── Shop ─────────────────────────────────────────────────────────────────
    suspend fun unlockSkin(skin: LeafSkin) {
        dataStore.edit { prefs ->
            val owned = prefs[OWNED_SKINS_KEY]?.split(",")?.toMutableSet() ?: mutableSetOf(LeafSkin.CLASSIC.name)
            owned.add(skin.name)
            prefs[OWNED_SKINS_KEY] = owned.joinToString(",")
        }
    }
    suspend fun setActiveSkin(skin: LeafSkin) { dataStore.edit { it[ACTIVE_SKIN_KEY] = skin.name } }
    suspend fun unlockTheme(theme: RiverTheme) {
        dataStore.edit { prefs ->
            val owned = prefs[OWNED_THEMES_KEY]?.split(",")?.toMutableSet() ?: mutableSetOf(RiverTheme.FOREST.name)
            owned.add(theme.name)
            prefs[OWNED_THEMES_KEY] = owned.joinToString(",")
        }
    }
    suspend fun setActiveTheme(theme: RiverTheme) { dataStore.edit { it[ACTIVE_THEME_KEY] = theme.name } }

    // ── Playtime ─────────────────────────────────────────────────────────────
    suspend fun addPlaytime(seconds: Float) {
        dataStore.edit { it[PLAYTIME_KEY] = (it[PLAYTIME_KEY] ?: 0f) + seconds }
    }

    // ── Daily challenge ──────────────────────────────────────────────────────
    suspend fun setDailyChallenge(day: Int, completed: Boolean) {
        dataStore.edit { it[DAILY_DAY_KEY] = day; it[DAILY_DONE_KEY] = completed }
    }

    // ── Presets & resets ─────────────────────────────────────────────────────
    suspend fun applyPreset(preset: SensitivityPreset) {
        val s = ControlDefaults.presetSettings(preset)
        dataStore.edit { prefs ->
            prefs[SENSITIVITY_KEY] = s.sensitivityMultiplier
            prefs[STIFFNESS_KEY]   = s.stiffness
            prefs[DAMPING_KEY]     = s.damping
            prefs[DEAD_ZONE_KEY]   = s.deadZone
            prefs[PRESET_KEY]      = preset.name
        }
    }

    suspend fun resetSettingsToDefaults() {
        dataStore.edit { prefs ->
            prefs[SENSITIVITY_KEY]   = ControlDefaults.SENSITIVITY
            prefs[CURVE_KEY]         = SensitivityCurve.EXPONENTIAL.name
            prefs[INVERT_KEY]        = false
            prefs[STIFFNESS_KEY]     = ControlDefaults.STIFFNESS
            prefs[DAMPING_KEY]       = ControlDefaults.DAMPING
            prefs[DEAD_ZONE_KEY]     = ControlDefaults.DEAD_ZONE
            prefs[CALIBRATION_KEY]   = 0f
            prefs[HITBOX_KEY]        = ControlDefaults.HITBOX
            prefs[SNAP_KEY]          = false
            prefs[ICON_SCALE_KEY]    = ControlDefaults.ICON_SCALE
            prefs[CONTROL_MODE_KEY]  = ControlMode.GYROSCOPE.name
            prefs[PRESET_KEY]        = SensitivityPreset.BALANCED.name
            prefs[ACCESSIBILITY_KEY] = AccessibilityMode.STANDARD.name
        }
    }

    private companion object Keys {
        val SENSITIVITY_KEY   = floatPreferencesKey("sensitivity_multiplier")
        val CURVE_KEY         = stringPreferencesKey("sensitivity_curve")
        val INVERT_KEY        = booleanPreferencesKey("invert_tilt")
        val STIFFNESS_KEY     = floatPreferencesKey("stiffness")
        val DAMPING_KEY       = floatPreferencesKey("damping")
        val DEAD_ZONE_KEY     = floatPreferencesKey("dead_zone")
        val CALIBRATION_KEY   = floatPreferencesKey("calibration_offset")
        val HITBOX_KEY        = floatPreferencesKey("hitbox_shrink")
        val SNAP_KEY          = booleanPreferencesKey("instant_snap")
        val ICON_SCALE_KEY    = floatPreferencesKey("icon_scale")
        val HIGH_SCORE_KEY    = intPreferencesKey("high_score")
        val TUTORIAL_SEEN_KEY = booleanPreferencesKey("tutorial_seen")
        val SOUND_KEY         = booleanPreferencesKey("sound_enabled")
        val CONTROL_MODE_KEY  = stringPreferencesKey("control_mode")
        val PRESET_KEY        = stringPreferencesKey("sensitivity_preset")
        val ACCESSIBILITY_KEY = stringPreferencesKey("accessibility_mode")
        val DROPS_KEY         = intPreferencesKey("river_drops")
        val OWNED_SKINS_KEY   = stringPreferencesKey("owned_skins")
        val ACTIVE_SKIN_KEY   = stringPreferencesKey("active_skin")
        val OWNED_THEMES_KEY  = stringPreferencesKey("owned_themes")
        val ACTIVE_THEME_KEY  = stringPreferencesKey("active_theme")
        val PLAYTIME_KEY      = floatPreferencesKey("total_playtime_sec")
        val DAILY_DAY_KEY     = intPreferencesKey("daily_challenge_day")
        val DAILY_DONE_KEY    = booleanPreferencesKey("daily_challenge_done")
    }
}
