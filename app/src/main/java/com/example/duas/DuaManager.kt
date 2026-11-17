package com.example.duas

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.example.duas.catalog.AuthenticDuasCatalog
import com.example.duas.model.DuaCategory
import com.example.duas.model.DuaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DuaManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val prefs = context.getSharedPreferences("nova_duas_prefs", Context.MODE_PRIVATE)

    private val _duas = MutableStateFlow<List<DuaItem>>(AuthenticDuasCatalog.duasList)
    val duas: StateFlow<List<DuaItem>> = _duas.asStateFlow()

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        loadFavorites()
        loadCounts()
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            tts?.setSpeechRate(0.85f) // Calmer, reverent recitation pace
        }
    }

    fun playAudio(dua: DuaItem) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(context, "محرك النطق الصوتي العربي غير متوفر حالياً على هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }

        if (_isSpeaking.value && _currentSpeakingId.value == dua.id) {
            stopAudio()
            return
        }

        tts?.stop()
        _isSpeaking.value = true
        _currentSpeakingId.value = dua.id

        val textToSpeak = "${dua.title}. ${dua.arabicText}"
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "dua_${dua.id}")
    }

    fun stopAudio() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun incrementCount(duaId: String, targetCount: Int) {
        val current = _counts.value[duaId] ?: 0
        if (current < targetCount) {
            val newCount = current + 1
            val updated = _counts.value.toMutableMap()
            updated[duaId] = newCount
            _counts.value = updated
            saveCount(duaId, newCount)

            // Haptic vibration feedback
            triggerHapticFeedback(isCompletion = newCount == targetCount)
        }
    }

    fun resetCount(duaId: String) {
        val updated = _counts.value.toMutableMap()
        updated[duaId] = 0
        _counts.value = updated
        saveCount(duaId, 0)
    }

    fun resetAllCounts() {
        _counts.value = emptyMap()
        prefs.edit().apply {
            val keys = prefs.all.keys.filter { it.startsWith("count_") }
            keys.forEach { remove(it) }
            apply()
        }
        triggerHapticFeedback(isCompletion = true)
        Toast.makeText(context, "تمت إعادة ضبط عدادات الأذكار لليوم بنجاح", Toast.LENGTH_SHORT).show()
    }

    fun toggleFavorite(duaId: String) {
        val currentFavs = _favorites.value.toMutableSet()
        if (currentFavs.contains(duaId)) {
            currentFavs.remove(duaId)
            Toast.makeText(context, "تمت الإزالة من الأدعية المفضلة", Toast.LENGTH_SHORT).show()
        } else {
            currentFavs.add(duaId)
            Toast.makeText(context, "تمت الإضافة إلى المفضلة ❤️", Toast.LENGTH_SHORT).show()
        }
        _favorites.value = currentFavs
        prefs.edit().putStringSet("dua_favorites", currentFavs).apply()
    }

    fun shareDua(dua: DuaItem) {
        val shareBody = """
            🤲 *${dua.title}*
            
            ${dua.arabicText}
            
            📖 *المصدر والتخريج:* ${dua.sourceTakhrij}
            ✨ *الفضل:* ${dua.virtue}
            
            ـ تم الإرسال عبر تطبيق CinemaX Player (مكتبة الأدعية وحصن المسلم)
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, dua.title)
            putExtra(Intent.EXTRA_TEXT, shareBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الدعاء المبارك").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun copyToClipboard(dua: DuaItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Dua", "${dua.arabicText}\n\n[المصدر: ${dua.sourceTakhrij}]")
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ الدعاء والمصدر إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun triggerHapticFeedback(isCompletion: Boolean) {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val duration = if (isCompletion) 90L else 30L
                    val amplitude = if (isCompletion) VibrationEffect.DEFAULT_AMPLITUDE else 100
                    v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(if (isCompletion) 90L else 30L)
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadFavorites() {
        val set = prefs.getStringSet("dua_favorites", emptySet()) ?: emptySet()
        _favorites.value = set
    }

    private fun loadCounts() {
        val map = mutableMapOf<String, Int>()
        for (item in AuthenticDuasCatalog.duasList) {
            val c = prefs.getInt("count_${item.id}", 0)
            if (c > 0) map[item.id] = c
        }
        _counts.value = map
    }

    private fun saveCount(duaId: String, count: Int) {
        prefs.edit().putInt("count_$duaId", count).apply()
    }

    fun loadOnlineQuranDuas(onComplete: (count: Int) -> Unit = {}) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val onlineDuas = com.example.duas.api.AuthenticQuranDuaApiClient.fetchQuranicDuasFromApi()
            if (onlineDuas.isNotEmpty()) {
                val combined = (AuthenticDuasCatalog.duasList + onlineDuas).distinctBy { it.id }
                _duas.value = combined
            }
            withContext(Dispatchers.Main) {
                onComplete(onlineDuas.size)
            }
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
