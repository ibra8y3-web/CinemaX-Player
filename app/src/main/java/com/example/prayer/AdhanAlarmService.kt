package com.example.prayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AdhanAlarmType

class AdhanAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPrayerName: String = "الصلاة"
    private var currentCityName: String = ""
    private var currentMuazzinName: String = ""

    companion object {
        const val CHANNEL_ID = "PRAYER_ADHAN_CHANNEL"
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP_ADHAN = "com.example.cinemax.ACTION_STOP_ADHAN"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ADHAN) {
            AdhanLogStore.appendLog(
                context = this,
                prayerName = currentPrayerName,
                cityName = currentCityName,
                voiceName = currentMuazzinName,
                eventType = AdhanAlarmType.STOPPED,
                note = "تم إيقاف صوت الأذان يدوياً"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences("nova_prayer_prefs", Context.MODE_PRIVATE)
        val prayerName = intent?.getStringExtra(PrayerTimesManager.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val cityName = intent?.getStringExtra(PrayerTimesManager.EXTRA_CITY_NAME) ?: ""
        val rawVoiceId = intent?.getStringExtra(PrayerTimesManager.EXTRA_VOICE_ID) ?: ""
        val resolvedId = rawVoiceId.ifBlank {
            prefs.getString("selected_adhan_voice", AdhanVoicesCatalog.voices.first().id)
                ?: AdhanVoicesCatalog.voices.first().id
        }
        val voice = AdhanVoicesCatalog.getVoiceById(resolvedId)

        currentPrayerName = prayerName
        currentCityName = cityName
        currentMuazzinName = voice.muazzinName

        val notification = buildNotification(prayerName, cityName, voice.muazzinName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("AdhanAlarmService", "Error calling startForeground", e)
        }

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CinemaX:AdhanWakeLock").apply {
                acquire(voice.durationSec * 1000L + 30_000L)
            }
        } catch (e: Exception) {
            Log.e("AdhanAlarmService", "Error acquiring WakeLock", e)
        }

        val volumePercent = prefs.getInt("adhan_volume_percent", 90).coerceIn(20, 100)
        val vibrateEnabled = prefs.getBoolean("adhan_vibration_enabled", true)

        if (vibrateEnabled) {
            triggerGentleVibration()
        }

        playAdhan(voice.rawResId, volumePercent)

        val overlayIntent = Intent(this, AdhanOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(PrayerTimesManager.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerTimesManager.EXTRA_CITY_NAME, cityName)
            putExtra("EXTRA_MUAZZIN_NAME", voice.muazzinName)
        }
        try {
            startActivity(overlayIntent)
        } catch (e: Exception) {
            Log.e("AdhanAlarmService", "Error starting AdhanOverlayActivity", e)
        }

        return START_NOT_STICKY
    }

    private fun triggerGentleVibration() {
        try {
            val pattern = longArrayOf(0L, 400L, 250L, 400L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playAdhan(rawResId: Int, volumePercent: Int) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val resId = if (rawResId != 0) rawResId else R.raw.adhan_makkah
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ratio = (volumePercent / 100f).coerceIn(0.2f, 1.0f)

            try {
                audioManager?.let { am ->
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    if (maxVol > 0) {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, (maxVol * ratio).toInt().coerceAtLeast(1), 0)
                    }
                }
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }

            val player = try {
                MediaPlayer.create(
                    this,
                    resId,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                    audioManager?.generateAudioSessionId() ?: 0
                )
            } catch (_: Exception) {
                null
            } ?: MediaPlayer.create(this, resId)

            mediaPlayer = player
            player?.apply {
                setVolume(ratio, ratio)
                setOnCompletionListener {
                    stopSelf()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AdhanAlarmService", "MediaPlayer error: what=$what extra=$extra")
                    stopSelf()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AdhanAlarmService", "Error playing Adhan audio", e)
        }
    }

    private fun buildNotification(prayerName: String, cityName: String, muazzinName: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_SCREEN", "prayer_times")
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdhanAlarmService::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overlayIntent = Intent(this, AdhanOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(PrayerTimesManager.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerTimesManager.EXTRA_CITY_NAME, cityName)
            putExtra("EXTRA_MUAZZIN_NAME", muazzinName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 2, overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "حان الآن موعد أذان $prayerName" + if (cityName.isNotBlank()) " في $cityName" else ""
        val content = "بصوت: $muazzinName"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, "إيقاف الأذان", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات أذان الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تشغيل الأذان بصوت المؤذن المختار في الوقت الفعلي"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val preChannel = NotificationChannel(
                AdhanAlarmReceiver.PRE_ADHAN_CHANNEL_ID,
                "تنبيهات قبل الأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير بالاستعداد قبل دخول وقت الصلاة"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val postChannel = NotificationChannel(
                AdhanAlarmReceiver.POST_ADHAN_CHANNEL_ID,
                "أذكار ما بعد الأذان والتسبيح",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير بدعاء ما بعد الأذان والسبحة"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(preChannel)
            manager.createNotificationChannel(postChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}
