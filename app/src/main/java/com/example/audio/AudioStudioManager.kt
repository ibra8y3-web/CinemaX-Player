package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AudioPreset(val titleAr: String, val bass: Short, val virtualizer: Short, val gains: List<Short>) {
    FLAT("متوازن (Flat)", 0, 0, listOf(0, 0, 0, 0, 0)),
    CINEMA_3D("سينمائي ثلاثي الأبعاد", 600, 800, listOf(300, 100, -100, 200, 400)),
    BASS_BOOST("تعزيز الباس العميق", 950, 400, listOf(600, 400, 100, 0, 100)),
    VOCAL_CLARITY("وضوح الحوار والأصوات", 100, 200, listOf(-200, 100, 500, 600, 300)),
    ROCK("موسيقى الروك الحيوية", 700, 500, listOf(500, 300, -100, 400, 600)),
    POP("موسيقى البوب المشرقة", 400, 300, listOf(-100, 200, 500, 300, -100))
}

data class AudioStudioState(
    val isEnabled: Boolean = true,
    val currentPreset: AudioPreset = AudioPreset.CINEMA_3D,
    val bassBoostStrength: Int = 600, // 0 - 1000
    val virtualizerStrength: Int = 800, // 0 - 1000
    val volumeBoostMb: Int = 0, // 0 - 1000 (0 to 10dB)
    val bandLevels: List<Int> = listOf(300, 100, -100, 200, 400) // 5 bands
)

class AudioStudioManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(AudioStudioState())
    val state: StateFlow<AudioStudioState> = _state

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        try {
            releaseEffects()

            // 1. Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            // 2. Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                    enabled = _state.value.bassBoostStrength > 0
                }
            }

            // 3. Virtualizer (3D surround effect)
            virtualizer = Virtualizer(0, audioSessionId).apply {
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                    enabled = _state.value.virtualizerStrength > 0
                }
            }

            // 4. Loudness Enhancer (super volume booster)
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(_state.value.volumeBoostMb)
                enabled = _state.value.volumeBoostMb > 0
            }

            applyPreset(_state.value.currentPreset)
        } catch (_: Exception) {}
    }

    fun applyPreset(preset: AudioPreset) {
        _state.value = _state.value.copy(
            currentPreset = preset,
            bassBoostStrength = preset.bass.toInt(),
            virtualizerStrength = preset.virtualizer.toInt(),
            bandLevels = preset.gains.map { it.toInt() }
        )

        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.bass)
                    it.enabled = preset.bass > 0
                }
            }

            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.virtualizer)
                    it.enabled = preset.virtualizer > 0
                }
            }

            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt().coerceAtMost(preset.gains.size)
                for (i in 0 until numBands) {
                    val gain = preset.gains[i].coerceIn(eq.bandLevelRange[0], eq.bandLevelRange[1])
                    eq.setBandLevel(i.toShort(), gain)
                }
            }
        } catch (_: Exception) {}
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassBoostStrength = clamped)
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                    it.enabled = clamped > 0
                }
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(virtualizerStrength = clamped)
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                    it.enabled = clamped > 0
                }
            }
        } catch (_: Exception) {}
    }

    fun setVolumeBoost(boostMb: Int) {
        val clamped = boostMb.coerceIn(0, 1000)
        _state.value = _state.value.copy(volumeBoostMb = clamped)
        try {
            loudnessEnhancer?.apply {
                setTargetGain(clamped)
                enabled = clamped > 0
            }
        } catch (_: Exception) {}
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val currentLevels = _state.value.bandLevels.toMutableList()
        if (bandIndex in currentLevels.indices) {
            currentLevels[bandIndex] = levelMb
            _state.value = _state.value.copy(bandLevels = currentLevels)
            try {
                equalizer?.let { eq ->
                    if (bandIndex < eq.numberOfBands) {
                        val clamped = levelMb.toShort().coerceIn(eq.bandLevelRange[0], eq.bandLevelRange[1])
                        eq.setBandLevel(bandIndex.toShort(), clamped)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
