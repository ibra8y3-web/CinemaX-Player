package com.example.prayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AdhanAlarmType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AdhanAlarmReceiver"
        const val PRE_ADHAN_CHANNEL_ID = "PRAYER_PRE_ADHAN_CHANNEL"
        const val POST_ADHAN_CHANNEL_ID = "PRAYER_POST_ADHAN_CHANNEL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "[RECEIVER] Triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            try {
                PrayerTimesManager(context).scheduleExactAdhanAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "[RECEIVER: REBOOT ERROR] Failed to reschedule alarms", e)
            }
            return
        }

        val prefs = context.getSharedPreferences("nova_prayer_prefs", Context.MODE_PRIVATE)
        val prayerName = intent.getStringExtra(PrayerTimesManager.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val cityName = intent.getStringExtra(PrayerTimesManager.EXTRA_CITY_NAME) ?: ""
        val rawVoiceId = intent.getStringExtra(PrayerTimesManager.EXTRA_VOICE_ID) ?: ""
        val effectiveVoiceId = rawVoiceId.ifBlank {
            prefs.getString("selected_adhan_voice", AdhanVoicesCatalog.voices.first().id)
                ?: AdhanVoicesCatalog.voices.first().id
        }
        val voice = AdhanVoicesCatalog.getVoiceById(effectiveVoiceId)
        val alarmTypeStr = intent.getStringExtra(PrayerTimesManager.EXTRA_ALARM_TYPE) ?: "ADHAN"
        val offsetMinutes = intent.getIntExtra(PrayerTimesManager.EXTRA_OFFSET_MINUTES, 10)

        when (alarmTypeStr) {
            "PRE_ADHAN" -> {
                postPreAdhanNotification(context, prayerName, cityName, offsetMinutes)
                AdhanLogStore.appendLog(
                    context = context,
                    prayerName = prayerName,
                    cityName = cityName,
                    voiceName = voice.muazzinName,
                    eventType = AdhanAlarmType.PRE_ADHAN,
                    note = "تذكير قبل أذان $prayerName بـ $offsetMinutes دقائق"
                )
            }
            "POST_ADHAN" -> {
                postPostAdhanNotification(context, prayerName, cityName, offsetMinutes)
                AdhanLogStore.appendLog(
                    context = context,
                    prayerName = prayerName,
                    cityName = cityName,
                    voiceName = voice.muazzinName,
                    eventType = AdhanAlarmType.POST_ADHAN,
                    note = "تنبيه الإقامة والأذكار والتسبيح بعد $prayerName بـ $offsetMinutes دقائق"
                )
            }
            else -> {
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                prefs.edit().putString("last_adhan_triggered_time", "$prayerName (${voice.muazzinName}) - $nowStr").apply()

                val logType = if (alarmTypeStr == "TEST") AdhanAlarmType.TEST else AdhanAlarmType.ADHAN
                AdhanLogStore.appendLog(
                    context = context,
                    prayerName = prayerName,
                    cityName = cityName,
                    voiceName = voice.muazzinName,
                    eventType = logType,
                    note = "تم إطلاق الأذان بنجاح (${voice.nameAr})"
                )

                val serviceIntent = Intent(context, AdhanAlarmService::class.java).apply {
                    putExtra(PrayerTimesManager.EXTRA_PRAYER_NAME, prayerName)
                    putExtra(PrayerTimesManager.EXTRA_CITY_NAME, cityName)
                    putExtra(PrayerTimesManager.EXTRA_VOICE_ID, voice.id)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[RECEIVER: ERROR] Failed to start AdhanAlarmService", e)
                }

                if (alarmTypeStr != "TEST") {
                    try {
                        PrayerTimesManager(context).scheduleExactAdhanAlarms()
                    } catch (e: Exception) {
                        Log.e(TAG, "[RECEIVER: ERROR] Failed to reschedule next prayer alarms", e)
                    }
                }
            }
        }
    }

    private fun postPreAdhanNotification(
        context: Context,
        prayerName: String,
        cityName: String,
        offsetMinutes: Int
    ) {
        ensureReminderChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_SCREEN", "prayer_times")
        }
        val pi = PendingIntent.getActivity(
            context,
            8101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val citySuffix = if (cityName.isNotBlank()) " في $cityName" else ""
        val notification = NotificationCompat.Builder(context, PRE_ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("اقترب أذان $prayerName بعد $offsetMinutes دقائق")
            .setContentText("حيّ على الوضوء والاستعداد لصلاة $prayerName$citySuffix")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "متبقي $offsetMinutes دقائق على دخول وقت صلاة $prayerName$citySuffix • استحضر النية واستعد للصلاة."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        manager.notify(9011, notification)
    }

    private fun postPostAdhanNotification(
        context: Context,
        prayerName: String,
        cityName: String,
        offsetMinutes: Int
    ) {
        ensureReminderChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val tasbihIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_SCREEN", "prayer_tasbih")
        }
        val pi = PendingIntent.getActivity(
            context,
            8102,
            tasbihIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val citySuffix = if (cityName.isNotBlank()) " ($cityName)" else ""
        val notification = NotificationCompat.Builder(context, POST_ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("أذكار ما بعد الأذان والإقامة • $prayerName$citySuffix")
            .setContentText("مضت $offsetMinutes دقائق على الأذان • اضغط لفتح السبحة وأذكار ما بعد الصلاة")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${AdhanLogStore.postAdhanDuaAr}\nاضغط لفتح السبحة الإلكترونية (سبحان الله • الحمد لله • الله أكبر 33×) وسجل الأذان."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .addAction(R.mipmap.ic_launcher, "فتح التسبيح والأذكار", pi)
            .build()

        manager.notify(9012, notification)
    }

    private fun ensureReminderChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val preChannel = NotificationChannel(
                PRE_ADHAN_CHANNEL_ID,
                "تنبيهات قبل الأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه بالاستعداد والوضوء قبل دخول وقت الصلاة"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val postChannel = NotificationChannel(
                POST_ADHAN_CHANNEL_ID,
                "أذكار ما بعد الأذان والتسبيح",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير بدعاء ما بعد الأذان والإقامة والسبحة"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(preChannel)
            manager.createNotificationChannel(postChannel)
        }
    }
}
