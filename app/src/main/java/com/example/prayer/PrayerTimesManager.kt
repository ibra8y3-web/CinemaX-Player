package com.example.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.AdhanAlarmType
import com.example.data.model.AdhanLogEntry
import com.example.data.model.AdhanVoice
import com.example.data.model.CalculationMethod
import com.example.data.model.CityLocation
import com.example.data.model.DhikrPhrase
import com.example.data.model.PrayerTimeEntry
import com.example.data.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@UnstableApi
class PrayerTimesManager(private val context: Context) : SensorEventListener, LocationListener {

    companion object {
        const val ACTION_TRIGGER_ADHAN = "com.example.cinemax.ACTION_TRIGGER_ADHAN"
        const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
        const val EXTRA_CITY_NAME = "EXTRA_CITY_NAME"
        const val EXTRA_VOICE_ID = "EXTRA_VOICE_ID"
        const val EXTRA_ALARM_TYPE = "EXTRA_ALARM_TYPE"
        const val EXTRA_OFFSET_MINUTES = "EXTRA_OFFSET_MINUTES"

        private const val REQ_CODE_ADHAN_BASE = 1000
        private const val REQ_CODE_PRE_ADHAN_BASE = 2000
        private const val REQ_CODE_POST_ADHAN_BASE = 3000
        private const val REQ_CODE_TEST_ADHAN = 9999
        private const val REQ_CODE_TEST_PRE = 9998
        private const val REQ_CODE_TEST_POST = 9997
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var countdownJob: Job? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private val _selectedCity = MutableStateFlow(PrayerTimesCalculator.defaultCities.first { it.nameAr == "القاهرة" })
    val selectedCity: StateFlow<CityLocation> = _selectedCity.asStateFlow()

    private val _isUsingGps = MutableStateFlow(false)
    val isUsingGps: StateFlow<Boolean> = _isUsingGps.asStateFlow()

    private val _currentLat = MutableStateFlow(30.0444)
    val currentLat: StateFlow<Double> = _currentLat.asStateFlow()

    private val _currentLon = MutableStateFlow(31.2357)
    val currentLon: StateFlow<Double> = _currentLon.asStateFlow()

    private val _prayerTimes = MutableStateFlow<List<PrayerTimeEntry>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTimeEntry>> = _prayerTimes.asStateFlow()

    private val _qiblaBearing = MutableStateFlow(136.0f)
    val qiblaBearing: StateFlow<Float> = _qiblaBearing.asStateFlow()

    private val _compassHeading = MutableStateFlow(0.0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    private val _distanceToKaabaKm = MutableStateFlow(1280)
    val distanceToKaabaKm: StateFlow<Int> = _distanceToKaabaKm.asStateFlow()

    private val _selectedAdhanVoice = MutableStateFlow(AdhanVoicesCatalog.voices.first())
    val selectedAdhanVoice: StateFlow<AdhanVoice> = _selectedAdhanVoice.asStateFlow()

    private val _isAutoMuezzinEnabled = MutableStateFlow(false)
    val isAutoMuezzinEnabled: StateFlow<Boolean> = _isAutoMuezzinEnabled.asStateFlow()

    private val _hasUserChosenMuezzin = MutableStateFlow(false)
    val hasUserChosenMuezzin: StateFlow<Boolean> = _hasUserChosenMuezzin.asStateFlow()

    private val _perPrayerVoices = MutableStateFlow<Map<PrayerType, String>>(emptyMap())
    val perPrayerVoices: StateFlow<Map<PrayerType, String>> = _perPrayerVoices.asStateFlow()

    private val _preAdhanMinutesBefore = MutableStateFlow(10)
    val preAdhanMinutesBefore: StateFlow<Int> = _preAdhanMinutesBefore.asStateFlow()

    private val _postAdhanMinutesAfter = MutableStateFlow(10)
    val postAdhanMinutesAfter: StateFlow<Int> = _postAdhanMinutesAfter.asStateFlow()

    private val _adhanVolumePercent = MutableStateFlow(90)
    val adhanVolumePercent: StateFlow<Int> = _adhanVolumePercent.asStateFlow()

    private val _adhanVibrationEnabled = MutableStateFlow(true)
    val adhanVibrationEnabled: StateFlow<Boolean> = _adhanVibrationEnabled.asStateFlow()

    private val _adhanLogs = MutableStateFlow<List<AdhanLogEntry>>(emptyList())
    val adhanLogs: StateFlow<List<AdhanLogEntry>> = _adhanLogs.asStateFlow()

    // Tasbih & Post-Adhan Dhikr state
    val dhikrCatalog: List<DhikrPhrase> = AdhanLogStore.defaultDhikrCatalog

    private val _selectedDhikr = MutableStateFlow(dhikrCatalog.first())
    val selectedDhikr: StateFlow<DhikrPhrase> = _selectedDhikr.asStateFlow()

    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _tasbihTarget = MutableStateFlow(33)
    val tasbihTarget: StateFlow<Int> = _tasbihTarget.asStateFlow()

    private val _totalTasbihCount = MutableStateFlow(0)
    val totalTasbihCount: StateFlow<Int> = _totalTasbihCount.asStateFlow()

    private val _tasbihCompletedCycles = MutableStateFlow(0)
    val tasbihCompletedCycles: StateFlow<Int> = _tasbihCompletedCycles.asStateFlow()

    private val _isPlayingAdhanPreview = MutableStateFlow(false)
    val isPlayingAdhanPreview: StateFlow<Boolean> = _isPlayingAdhanPreview.asStateFlow()

    private val _previewingVoiceId = MutableStateFlow<String?>(null)
    val previewingVoiceId: StateFlow<String?> = _previewingVoiceId.asStateFlow()

    private val _nextPrayerCountdownSeconds = MutableStateFlow(0L)
    val nextPrayerCountdownSeconds: StateFlow<Long> = _nextPrayerCountdownSeconds.asStateFlow()

    private val _nextPrayerTitle = MutableStateFlow("الفجر")
    val nextPrayerTitle: StateFlow<String> = _nextPrayerTitle.asStateFlow()

    private var adhanPlayer: ExoPlayer? = null
    private val prefs = context.getSharedPreferences("nova_prayer_prefs", Context.MODE_PRIVATE)

    private val _calculationMethod = MutableStateFlow(CalculationMethod.EGYPT)
    val calculationMethod: StateFlow<CalculationMethod> = _calculationMethod.asStateFlow()

    init {
        loadSavedSettings()
        recalculatePrayerTimes()
        startCountdownTimer()
    }

    private fun loadSavedSettings() {
        val cityName = prefs.getString("selected_city_name", "القاهرة") ?: "القاهرة"
        PrayerTimesCalculator.defaultCities.find { it.nameAr == cityName }?.let {
            _selectedCity.value = it
            _currentLat.value = it.latitude
            _currentLon.value = it.longitude
        }

        // Default to manual user choice so a single Sheikh is NEVER forced on the user
        val autoEnabled = prefs.getBoolean("is_auto_muezzin", false)
        _isAutoMuezzinEnabled.value = autoEnabled
        _hasUserChosenMuezzin.value = prefs.getBoolean("user_selected_muezzin", false)

        val voiceId = prefs.getString("selected_adhan_voice", "") ?: ""
        if (voiceId.isNotBlank()) {
            val resolved = AdhanVoicesCatalog.getVoiceById(voiceId)
            _selectedAdhanVoice.value = resolved
            if (resolved.id != voiceId) {
                prefs.edit().putString("selected_adhan_voice", resolved.id).apply()
            }
        } else if (autoEnabled) {
            autoSelectVoiceForLocation()
        } else {
            _selectedAdhanVoice.value = AdhanVoicesCatalog.voices.first()
        }

        // Load per-prayer voice map
        val perMap = mutableMapOf<PrayerType, String>()
        PrayerType.entries.filter { it != PrayerType.SUNRISE }.forEach { pType ->
            val vId = prefs.getString("adhan_voice_${pType.id}", "") ?: ""
            if (vId.isNotBlank()) {
                val resolved = AdhanVoicesCatalog.getVoiceById(vId)
                perMap[pType] = resolved.id
            }
        }
        _perPrayerVoices.value = perMap

        val methodStr = prefs.getString("calc_method", "") ?: ""
        CalculationMethod.entries.find { it.name == methodStr }?.let {
            _calculationMethod.value = it
        }

        _preAdhanMinutesBefore.value = prefs.getInt("pre_adhan_minutes", 10).coerceIn(0, 45)
        _postAdhanMinutesAfter.value = prefs.getInt("post_adhan_minutes", 10).coerceIn(0, 45)
        _adhanVolumePercent.value = prefs.getInt("adhan_volume_percent", 90).coerceIn(20, 100)
        _adhanVibrationEnabled.value = prefs.getBoolean("adhan_vibration_enabled", true)

        // Load Tasbih state
        val savedDhikrId = prefs.getString("tasbih_dhikr_id", dhikrCatalog.first().id) ?: dhikrCatalog.first().id
        val phrase = dhikrCatalog.find { it.id == savedDhikrId } ?: dhikrCatalog.first()
        _selectedDhikr.value = phrase
        _tasbihCount.value = prefs.getInt("tasbih_current_count", 0).coerceAtLeast(0)
        _tasbihTarget.value = prefs.getInt("tasbih_target", phrase.defaultTarget).coerceAtLeast(0)
        _totalTasbihCount.value = prefs.getInt("tasbih_total_count", 0).coerceAtLeast(0)
        _tasbihCompletedCycles.value = prefs.getInt("tasbih_completed_cycles", 0).coerceAtLeast(0)

        _adhanLogs.value = AdhanLogStore.getLogs(context)
    }

    fun refreshLogs() {
        _adhanLogs.value = AdhanLogStore.getLogs(context)
        _totalTasbihCount.value = prefs.getInt("tasbih_total_count", _totalTasbihCount.value).coerceAtLeast(0)
    }

    fun clearAdhanLogs() {
        _adhanLogs.value = AdhanLogStore.clearLogs(context)
    }

    fun setCalculationMethod(method: CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit().putString("calc_method", method.name).apply()
        recalculatePrayerTimes()
    }

    fun togglePrayerAdhan(prayerType: PrayerType) {
        val current = isPrayerAdhanEnabled(prayerType)
        val newEnabled = !current
        prefs.edit().putBoolean("adhan_enabled_${prayerType.id}", newEnabled).apply()
        _prayerTimes.value = _prayerTimes.value.map {
            if (it.type == prayerType) it.copy(adhanEnabled = newEnabled) else it
        }
        scheduleExactAdhanAlarms()
    }

    fun isPrayerAdhanEnabled(prayerType: PrayerType): Boolean {
        return prefs.getBoolean("adhan_enabled_${prayerType.id}", true)
    }

    fun setPreAdhanMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 45)
        _preAdhanMinutesBefore.value = clamped
        prefs.edit().putInt("pre_adhan_minutes", clamped).apply()
        scheduleExactAdhanAlarms()
    }

    fun setPostAdhanMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 45)
        _postAdhanMinutesAfter.value = clamped
        prefs.edit().putInt("post_adhan_minutes", clamped).apply()
        scheduleExactAdhanAlarms()
    }

    fun setAdhanVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(20, 100)
        _adhanVolumePercent.value = clamped
        prefs.edit().putInt("adhan_volume_percent", clamped).apply()
    }

    fun setAdhanVibrationEnabled(enabled: Boolean) {
        _adhanVibrationEnabled.value = enabled
        prefs.edit().putBoolean("adhan_vibration_enabled", enabled).apply()
    }

    fun selectDhikr(phrase: DhikrPhrase) {
        _selectedDhikr.value = phrase
        _tasbihCount.value = 0
        _tasbihTarget.value = phrase.defaultTarget
        prefs.edit()
            .putString("tasbih_dhikr_id", phrase.id)
            .putInt("tasbih_current_count", 0)
            .putInt("tasbih_target", phrase.defaultTarget)
            .apply()
    }

    fun setTasbihTarget(target: Int) {
        val clean = target.coerceAtLeast(0)
        _tasbihTarget.value = clean
        prefs.edit().putInt("tasbih_target", clean).apply()
    }

    fun incrementTasbih(): Boolean {
        val target = _tasbihTarget.value
        val (next, reachedTarget) = AdhanLogStore.nextTasbihCount(_tasbihCount.value, target)
        val newTotal = _totalTasbihCount.value + 1
        _totalTasbihCount.value = newTotal

        return if (reachedTarget) {
            val newCycles = _tasbihCompletedCycles.value + 1
            _tasbihCompletedCycles.value = newCycles
            _tasbihCount.value = 0
            prefs.edit()
                .putInt("tasbih_current_count", 0)
                .putInt("tasbih_total_count", newTotal)
                .putInt("tasbih_completed_cycles", newCycles)
                .apply()
            true
        } else {
            _tasbihCount.value = next
            prefs.edit()
                .putInt("tasbih_current_count", next)
                .putInt("tasbih_total_count", newTotal)
                .apply()
            false
        }
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
        prefs.edit().putInt("tasbih_current_count", 0).apply()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (_: Exception) {
            true
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun testAdhan(delaySeconds: Int = 5) {
        val voice = _selectedAdhanVoice.value
        val city = _selectedCity.value
        val prayerLabel = "تجربة الأذان (${delaySeconds} ث)"
        scheduleOneShotAlarm(
            reqCode = REQ_CODE_TEST_ADHAN,
            delaySeconds = delaySeconds,
            prayerName = prayerLabel,
            cityName = city.nameAr,
            voiceId = voice.id,
            alarmType = "TEST",
            offsetMinutes = 0
        )
        _adhanLogs.value = AdhanLogStore.appendLog(
            context = context,
            prayerName = prayerLabel,
            cityName = city.nameAr,
            voiceName = voice.muazzinName,
            eventType = AdhanAlarmType.TEST,
            note = "تم جدولة اختبار الأذان بعد $delaySeconds ثوانٍ"
        )
    }

    fun testPreAdhan(delaySeconds: Int = 3) {
        val city = _selectedCity.value
        val offset = _preAdhanMinutesBefore.value.takeIf { it > 0 } ?: 10
        scheduleOneShotAlarm(
            reqCode = REQ_CODE_TEST_PRE,
            delaySeconds = delaySeconds,
            prayerName = _nextPrayerTitle.value,
            cityName = city.nameAr,
            voiceId = _selectedAdhanVoice.value.id,
            alarmType = "PRE_ADHAN",
            offsetMinutes = offset
        )
    }

    fun testPostAdhan(delaySeconds: Int = 3) {
        val city = _selectedCity.value
        val offset = _postAdhanMinutesAfter.value.takeIf { it > 0 } ?: 10
        scheduleOneShotAlarm(
            reqCode = REQ_CODE_TEST_POST,
            delaySeconds = delaySeconds,
            prayerName = _nextPrayerTitle.value,
            cityName = city.nameAr,
            voiceId = _selectedAdhanVoice.value.id,
            alarmType = "POST_ADHAN",
            offsetMinutes = offset
        )
    }

    fun stopActiveAdhan() {
        stopAdhanPreview()
        try {
            val stopIntent = Intent(context, AdhanAlarmService::class.java).apply {
                action = AdhanAlarmService.ACTION_STOP_ADHAN
            }
            context.startService(stopIntent)
        } catch (_: Exception) {}
    }

    private fun scheduleOneShotAlarm(
        reqCode: Int,
        delaySeconds: Int,
        prayerName: String,
        cityName: String,
        voiceId: String,
        alarmType: String,
        offsetMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetMs = System.currentTimeMillis() + (delaySeconds * 1000L)
        val formattedTarget = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(targetMs))

        val alarmIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ADHAN
            putExtra(EXTRA_PRAYER_NAME, prayerName)
            putExtra(EXTRA_CITY_NAME, cityName)
            putExtra(EXTRA_VOICE_ID, voiceId)
            putExtra(EXTRA_ALARM_TYPE, alarmType)
            putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactPendingIntent(alarmManager, targetMs, pendingIntent)
        if (alarmType == "TEST" || alarmType == "ADHAN") {
            prefs.edit().putString("last_test_alarm_scheduled", formattedTarget).apply()
        }
    }

    fun setAutoMuezzinEnabled(enabled: Boolean) {
        _isAutoMuezzinEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_muezzin", enabled).apply()
        if (enabled) {
            autoSelectVoiceForLocation()
        }
        scheduleExactAdhanAlarms()
    }

    private fun autoSelectVoiceForLocation() {
        val city = _selectedCity.value
        val voice = AdhanVoiceSelector.selectVoiceForLocation(
            _currentLat.value,
            _currentLon.value,
            city.country,
            city.nameAr
        )
        _selectedAdhanVoice.value = voice
        prefs.edit().putString("selected_adhan_voice", voice.id).apply()
    }

    fun setCity(city: CityLocation) {
        _isUsingGps.value = false
        _selectedCity.value = city
        _currentLat.value = city.latitude
        _currentLon.value = city.longitude
        prefs.edit().putString("selected_city_name", city.nameAr).apply()

        if (_isAutoMuezzinEnabled.value && !_hasUserChosenMuezzin.value) {
            autoSelectVoiceForLocation()
        } else if (_isAutoMuezzinEnabled.value) {
            autoSelectVoiceForLocation()
        }

        recalculatePrayerTimes()
    }

    fun updateCoordinatesFromGps(lat: Double, lon: Double, localityName: String = "الموقع الحالي (GPS)") {
        _isUsingGps.value = true
        _currentLat.value = lat
        _currentLon.value = lon
        _selectedCity.value = CityLocation(localityName, "Current GPS", "", lat, lon, 3.0)

        if (_isAutoMuezzinEnabled.value) {
            autoSelectVoiceForLocation()
        }

        recalculatePrayerTimes()
    }

    fun setSelectedAdhanVoice(voice: AdhanVoice) {
        _selectedAdhanVoice.value = voice
        _isAutoMuezzinEnabled.value = false
        _hasUserChosenMuezzin.value = true
        prefs.edit()
            .putString("selected_adhan_voice", voice.id)
            .putBoolean("is_auto_muezzin", false)
            .putBoolean("user_selected_muezzin", true)
            .apply()
        scheduleExactAdhanAlarms()
    }

    fun getPrayerAdhanVoice(prayerType: PrayerType): AdhanVoice {
        val savedId = prefs.getString("adhan_voice_${prayerType.id}", "")
        return if (!savedId.isNullOrBlank()) {
            AdhanVoicesCatalog.getVoiceById(savedId)
        } else {
            _selectedAdhanVoice.value
        }
    }

    fun hasCustomPrayerVoice(prayerType: PrayerType): Boolean {
        return _perPrayerVoices.value.containsKey(prayerType)
    }

    fun setPrayerAdhanVoice(prayerType: PrayerType, voice: AdhanVoice) {
        _hasUserChosenMuezzin.value = true
        prefs.edit()
            .putString("adhan_voice_${prayerType.id}", voice.id)
            .putBoolean("user_selected_muezzin", true)
            .apply()
        _perPrayerVoices.value = _perPrayerVoices.value + (prayerType to voice.id)
        scheduleExactAdhanAlarms()
    }

    fun clearPrayerAdhanVoice(prayerType: PrayerType) {
        prefs.edit().remove("adhan_voice_${prayerType.id}").apply()
        _perPrayerVoices.value = _perPrayerVoices.value - prayerType
        scheduleExactAdhanAlarms()
    }

    fun recalculatePrayerTimes() {
        val lat = _currentLat.value
        val lon = _currentLon.value
        val method = _calculationMethod.value
        val offlineList = PrayerTimesCalculator.calculatePrayerTimes(lat, lon, Calendar.getInstance(), method)
        val mappedOffline = offlineList.map { entry ->
            entry.copy(adhanEnabled = isPrayerAdhanEnabled(entry.type))
        }
        _prayerTimes.value = mappedOffline

        _qiblaBearing.value = PrayerTimesCalculator.calculateQiblaBearing(lat, lon)
        _distanceToKaabaKm.value = PrayerTimesCalculator.calculateDistanceToKaabaKm(lat, lon)

        val nextOffline = mappedOffline.find { it.isNext } ?: mappedOffline.firstOrNull()
        if (nextOffline != null) {
            _nextPrayerTitle.value = nextOffline.type.titleAr
            calculateSecondsUntil(nextOffline.hour24, nextOffline.minute)
        }

        scheduleExactAdhanAlarms()

        // Asynchronously fetch exact API prayer times from Aladhan API
        scope.launch {
            val apiList = AladhanApiClient.fetchPrayerTimes(lat, lon, method)
            if (!apiList.isNullOrEmpty()) {
                val mappedApi = apiList.map { entry ->
                    entry.copy(adhanEnabled = isPrayerAdhanEnabled(entry.type))
                }
                _prayerTimes.value = mappedApi
                val nextApi = mappedApi.find { it.isNext } ?: mappedApi.firstOrNull()
                if (nextApi != null) {
                    _nextPrayerTitle.value = nextApi.type.titleAr
                    calculateSecondsUntil(nextApi.hour24, nextApi.minute)
                }
                scheduleExactAdhanAlarms()
            }
        }
    }

    fun scheduleExactAdhanAlarms() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val list = _prayerTimes.value
            val city = _selectedCity.value
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val preMin = _preAdhanMinutesBefore.value
            val postMin = _postAdhanMinutesAfter.value
            var enabledCount = 0

            list.forEachIndexed { index, prayer ->
                if (prayer.type != PrayerType.SUNRISE) {
                    val now = Calendar.getInstance()
                    val target = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, prayer.hour24)
                        set(Calendar.MINUTE, prayer.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (before(now) || timeInMillis <= now.timeInMillis) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    val targetFormatted = sdf.format(target.time)
                    val isEnabled = isPrayerAdhanEnabled(prayer.type)
                    val mainReqCode = REQ_CODE_ADHAN_BASE + index
                    val preReqCode = REQ_CODE_PRE_ADHAN_BASE + index
                    val postReqCode = REQ_CODE_POST_ADHAN_BASE + index
                    val prayerVoice = getPrayerAdhanVoice(prayer.type)

                    if (isEnabled) {
                        enabledCount++
                        // 1. Main Adhan Alarm
                        val mainIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                            action = ACTION_TRIGGER_ADHAN
                            putExtra(EXTRA_PRAYER_NAME, prayer.type.titleAr)
                            putExtra(EXTRA_CITY_NAME, city.nameAr)
                            putExtra(EXTRA_VOICE_ID, prayerVoice.id)
                            putExtra(EXTRA_ALARM_TYPE, "ADHAN")
                        }
                        val mainPending = PendingIntent.getBroadcast(
                            context,
                            mainReqCode,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        scheduleExactPendingIntent(alarmManager, target.timeInMillis, mainPending)

                        // 2. Pre-Adhan Reminder Alarm
                        if (preMin > 0) {
                            var preTriggerMs = AdhanLogStore.computeOffsetTriggerMillis(target.timeInMillis, preMin, isBefore = true)
                            if (preTriggerMs <= System.currentTimeMillis()) {
                                preTriggerMs += 86_400_000L
                            }
                            val preIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                                action = ACTION_TRIGGER_ADHAN
                                putExtra(EXTRA_PRAYER_NAME, prayer.type.titleAr)
                                putExtra(EXTRA_CITY_NAME, city.nameAr)
                                putExtra(EXTRA_VOICE_ID, prayerVoice.id)
                                putExtra(EXTRA_ALARM_TYPE, "PRE_ADHAN")
                                putExtra(EXTRA_OFFSET_MINUTES, preMin)
                            }
                            val prePending = PendingIntent.getBroadcast(
                                context,
                                preReqCode,
                                preIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            scheduleExactPendingIntent(alarmManager, preTriggerMs, prePending)
                        } else {
                            cancelAlarm(alarmManager, preReqCode)
                        }

                        // 3. Post-Adhan Reminder Alarm (Iqama / Dhikr / Tasbih)
                        if (postMin > 0) {
                            val postTriggerMs = AdhanLogStore.computeOffsetTriggerMillis(target.timeInMillis, postMin, isBefore = false)
                            val postIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                                action = ACTION_TRIGGER_ADHAN
                                putExtra(EXTRA_PRAYER_NAME, prayer.type.titleAr)
                                putExtra(EXTRA_CITY_NAME, city.nameAr)
                                putExtra(EXTRA_VOICE_ID, prayerVoice.id)
                                putExtra(EXTRA_ALARM_TYPE, "POST_ADHAN")
                                putExtra(EXTRA_OFFSET_MINUTES, postMin)
                            }
                            val postPending = PendingIntent.getBroadcast(
                                context,
                                postReqCode,
                                postIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            scheduleExactPendingIntent(alarmManager, postTriggerMs, postPending)
                        } else {
                            cancelAlarm(alarmManager, postReqCode)
                        }

                        val extrasDesc = buildString {
                            if (preMin > 0) append(" | قبل ${preMin}د")
                            if (postMin > 0) append(" | بعد ${postMin}د")
                        }
                        prefs.edit().putString(
                            "diagnostic_alarm_${prayer.type.id}",
                            "مجدول: $targetFormatted • ${prayerVoice.muazzinName}$extrasDesc"
                        ).apply()
                    } else {
                        cancelAlarm(alarmManager, mainReqCode)
                        cancelAlarm(alarmManager, preReqCode)
                        cancelAlarm(alarmManager, postReqCode)
                        prefs.edit().putString("diagnostic_alarm_${prayer.type.id}", "معطّل من الإعدادات").apply()
                    }
                }
            }

            if (list.isNotEmpty()) {
                val note = "مفعّل: $enabledCount صلوات • قبل: ${if (preMin > 0) "${preMin}د" else "إيقاف"} • بعد: ${if (postMin > 0) "${postMin}د" else "إيقاف"}"
                _adhanLogs.value = AdhanLogStore.appendLog(
                    context = context,
                    prayerName = _nextPrayerTitle.value,
                    cityName = city.nameAr,
                    voiceName = _selectedAdhanVoice.value.muazzinName,
                    eventType = AdhanAlarmType.SCHEDULED,
                    note = note
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerTimesManager", "[SCHEDULE_ERROR] Failed to schedule exact alarms", e)
        }
    }

    private fun scheduleExactPendingIntent(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canExact = canScheduleExactAlarms()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, com.example.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (canExact) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(alarmManager: AlarmManager, reqCode: Int) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ADHAN
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun calculateSecondsUntil(hour24: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val diffMs = target.timeInMillis - now.timeInMillis
        _nextPrayerCountdownSeconds.value = (diffMs / 1000L).coerceAtLeast(0L)
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_nextPrayerCountdownSeconds.value > 0) {
                    _nextPrayerCountdownSeconds.value -= 1
                } else {
                    recalculatePrayerTimes()
                }
            }
        }
    }

    // Compass Sensor Management
    fun startCompass() {
        sensorManager?.let { sm ->
            val rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationVector != null) {
                sm.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
            } else {
                val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
                if (magnet != null) sm.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopCompass() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuthDeg < 0) azimuthDeg += 360f
            _compassHeading.value = azimuthDeg
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            hasAccelerometer = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            hasMagnetometer = true
        }

        if (hasAccelerometer && hasMagnetometer) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerReading, magnetometerReading)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f
                _compassHeading.value = azimuthDeg
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Location Listener
    override fun onLocationChanged(location: Location) {
        updateCoordinatesFromGps(location.latitude, location.longitude, "موقعي الحالي (GPS)")
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    // Adhan Audio Preview - Real Sheikh Audio Playback
    fun previewAdhanVoice(voice: AdhanVoice) {
        if (_isPlayingAdhanPreview.value && _previewingVoiceId.value == voice.id) {
            stopAdhanPreview()
            return
        }

        stopAdhanPreview()
        _previewingVoiceId.value = voice.id

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val mediaUri = if (voice.rawResId != 0) {
                androidx.media3.datasource.RawResourceDataSource.buildRawResourceUri(voice.rawResId)
            } else {
                Uri.parse(voice.streamUrl)
            }

            adhanPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .build().apply {
                    volume = (_adhanVolumePercent.value / 100f).coerceIn(0.2f, 1.0f)
                    playWhenReady = true
                    setMediaItem(MediaItem.fromUri(mediaUri))
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            _isPlayingAdhanPreview.value = playing
                        }
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                _isPlayingAdhanPreview.value = false
                                _previewingVoiceId.value = null
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            _isPlayingAdhanPreview.value = false
                            _previewingVoiceId.value = null
                        }
                    })
                    prepare()
                    play()
                }
        } catch (_: Exception) {
            _isPlayingAdhanPreview.value = false
            _previewingVoiceId.value = null
        }
    }

    fun stopAdhanPreview() {
        try {
            adhanPlayer?.stop()
            adhanPlayer?.release()
        } catch (_: Exception) {}
        adhanPlayer = null
        _isPlayingAdhanPreview.value = false
        _previewingVoiceId.value = null
    }

    fun release() {
        stopCompass()
        stopAdhanPreview()
        countdownJob?.cancel()
    }
}
