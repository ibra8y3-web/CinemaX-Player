package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_player_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "AMOLED") ?: "AMOLED")
    val themeMode: StateFlow<String> = _themeMode

    private val _language = MutableStateFlow(prefs.getString("language", "ar") ?: "ar")
    val language: StateFlow<String> = _language

    private val _nightShield = MutableStateFlow(prefs.getBoolean("night_shield", false))
    val nightShield: StateFlow<Boolean> = _nightShield

    private val _soundPreset = MutableStateFlow(prefs.getString("sound_preset", "normal") ?: "normal")
    val soundPreset: StateFlow<String> = _soundPreset

    private val _aiEnhance = MutableStateFlow(prefs.getString("ai_enhance", "Vivid") ?: "Vivid")
    val aiEnhance: StateFlow<String> = _aiEnhance

    private val _isVaultPinSet = MutableStateFlow(!prefs.getString("vault_pin_hash", "").isNullOrEmpty())
    val isVaultPinSet: StateFlow<Boolean> = _isVaultPinSet

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setNightShield(enabled: Boolean) {
        prefs.edit().putBoolean("night_shield", enabled).apply()
        _nightShield.value = enabled
    }

    fun setSoundPreset(preset: String) {
        prefs.edit().putString("sound_preset", preset).apply()
        _soundPreset.value = preset
    }

    fun setAiEnhance(mode: String) {
        prefs.edit().putString("ai_enhance", mode).apply()
        _aiEnhance.value = mode
    }

    fun getVaultPinHash(): String? = prefs.getString("vault_pin_hash", null)

    fun setVaultPinHash(hash: String) {
        prefs.edit().putString("vault_pin_hash", hash).apply()
        _isVaultPinSet.value = true
    }

    fun clearVaultPin() {
        prefs.edit().remove("vault_pin_hash").apply()
        _isVaultPinSet.value = false
    }

    fun getLocalServerPort(): Int = prefs.getInt("local_share_port", 8080)
}
