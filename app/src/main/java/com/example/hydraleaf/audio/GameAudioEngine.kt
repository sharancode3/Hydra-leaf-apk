package com.example.hydraleaf.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import com.example.hydraleaf.GameConstants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp

/**
 * Synthesised audio engine — zero external assets.
 *
 * 5-layer adaptive music: bass drone, rhythm pulse, melody, harmony, accent.
 * Pentatonic dodge tones, pitch-shifted water rush, collect/death SFX.
 */
class GameAudioEngine {

    private val sr = GameConstants.AUDIO_SAMPLE_RATE
    private val running = AtomicBoolean(false)
    @Volatile var soundEnabled = true
    @Volatile var intensity = 0f        // 0..1 drives music layers
    @Volatile var speedFactor = 1f      // drives water-rush pitch

    // Pre-generated clips
    private val dodgeClips: Array<ShortArray>
    private val collectClip: ShortArray
    private val deathClip: ShortArray
    private val powerUpClip: ShortArray

    private var musicThread: Thread? = null
    private var musicTrack: AudioTrack? = null

    // Active SFX layers
    private val sfxQueue = java.util.concurrent.ConcurrentLinkedQueue<ShortArray>()

    init {
        dodgeClips = Array(5) { i ->
            generateTone(GameConstants.PENTATONIC_FREQS[i], GameConstants.DODGE_TONE_DURATION, 0.35f, decay = true)
        }
        collectClip = generateChirp(440f, 880f, GameConstants.COLLECT_TONE_DURATION, 0.3f)
        deathClip   = generateTone(80f, GameConstants.DEATH_TONE_DURATION, 0.5f, decay = true)
        powerUpClip = generateChirp(523f, 1047f, 0.2f, 0.25f)
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun start() {
        if (running.getAndSet(true)) return
        musicThread = Thread({
            val bufSize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            musicTrack = track
            track.play()
            val chunk = ShortArray(sr / 20) // 50 ms chunks
            var phase = 0.0
            var rhythmPhase = 0.0
            var melodyIdx = 0
            var melodyTimer = 0f
            while (running.get()) {
                if (!soundEnabled) { Thread.sleep(50); continue }
                val int = intensity.coerceIn(0f, 1f)
                val spd = speedFactor.coerceIn(0.5f, 3f)
                for (i in chunk.indices) {
                    val t = i.toFloat() / sr
                    var sample = 0.0
                    // Layer 1: bass drone (always)
                    sample += sin(phase) * 0.12 * (0.3 + int * 0.7)
                    // Layer 2: rhythm pulse (int > 0.2)
                    if (int > 0.2f) {
                        val rhythmAmp = ((int - 0.2f) / 0.8f).coerceIn(0.0f, 1.0f)
                        val rp = (rhythmPhase % (2 * PI)) / (2 * PI)
                        val pulse = if (rp < 0.1) 1.0 else 0.0
                        sample += pulse * 0.08 * rhythmAmp
                    }
                    // Layer 3: melody (int > 0.4)
                    if (int > 0.4f) {
                        val melAmp = ((int - 0.4f) / 0.6f).coerceIn(0.0f, 1.0f)
                        val freq = GameConstants.PENTATONIC_FREQS[melodyIdx % 5].toDouble()
                        sample += sin(2 * PI * freq * phase / (2 * PI * GameConstants.WATER_RUSH_BASE_FREQ.toDouble())) * 0.06 * melAmp
                    }
                    // Layer 4: harmony (int > 0.6)
                    if (int > 0.6f) {
                        val hAmp = ((int - 0.6f) / 0.4f).coerceIn(0.0f, 1.0f)
                        sample += sin(phase * 1.5) * 0.04 * hAmp
                    }
                    // Layer 5: accent (int > 0.8)
                    if (int > 0.8f) {
                        val aAmp = ((int - 0.8f) / 0.2f).coerceIn(0.0f, 1.0f)
                        sample += sin(phase * 3.0) * 0.03 * aAmp
                    }
                    // Water rush noise (filtered)
                    val noiseAmp = 0.04 * (0.3 + spd * 0.3)
                    sample += (Math.random() * 2 - 1) * noiseAmp

                    phase += 2 * PI * GameConstants.WATER_RUSH_BASE_FREQ * spd / sr
                    rhythmPhase += 2 * PI * 3.0 / sr  // 3 Hz rhythm
                    melodyTimer += t
                    if (melodyTimer > 0.25f) { melodyTimer = 0f; melodyIdx++ }

                    chunk[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                // Mix in SFX from queue
                val sfx = sfxQueue.poll()
                if (sfx != null) {
                    val mixLen = minOf(chunk.size, sfx.size)
                    for (i in 0 until mixLen) {
                        val mixed = chunk[i].toInt() + sfx[i].toInt()
                        chunk[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                track.write(chunk, 0, chunk.size)
            }
            track.stop(); track.release()
        }, "HydraAudio").apply { isDaemon = true; start() }
    }

    fun stop() {
        running.set(false)
        musicThread?.join(300)
        musicThread = null
    }

    fun release() { stop() }

    fun playDodge(noteIndex: Int = 0) {
        if (!soundEnabled) return
        sfxQueue.offer(dodgeClips[noteIndex.coerceIn(0, 4)])
    }
    fun playCollect()  { if (soundEnabled) sfxQueue.offer(collectClip) }
    fun playDeath()    { if (soundEnabled) sfxQueue.offer(deathClip)   }
    fun playPowerUp()  { if (soundEnabled) sfxQueue.offer(powerUpClip) }

    /** Number of currently active music layers (1-5) based on intensity */
    val activeLayerCount: Int get() {
        val i = intensity
        return when {
            i > 0.8f -> 5; i > 0.6f -> 4; i > 0.4f -> 3; i > 0.2f -> 2; else -> 1
        }
    }

    // ── Synthesis helpers ───────────────────────────────────────────────────

    private fun generateTone(freq: Float, durationSec: Float, amp: Float, decay: Boolean = false): ShortArray {
        val n = (sr * durationSec).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sr
            val env = if (decay) exp(-3.0 * t / durationSec).toFloat() else 1f
            (sin(2.0 * PI * freq * t).toFloat() * amp * env * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateChirp(f0: Float, f1: Float, dur: Float, amp: Float): ShortArray {
        val n = (sr * dur).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toFloat() / sr
            val frac = t / dur
            val freq = f0 + (f1 - f0) * frac
            val env = 1f - frac * 0.5f
            phase += 2.0 * PI * freq / sr
            (sin(phase).toFloat() * amp * env * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
