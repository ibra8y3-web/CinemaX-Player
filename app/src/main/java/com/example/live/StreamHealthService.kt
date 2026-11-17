package com.example.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * خدمة خلفية (Background / Foreground Service) لفحص صلاحية روابط البث (HLS/DASH/MP4/Audio)
 * في قاعدة البيانات وحذف الروابط التالفة تلقائياً لتنظيف القائمة دورياً.
 */
class StreamHealthService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var scanJob: Job? = null

    companion object {
        const val ACTION_START_SCAN = "com.example.live.ACTION_START_SCAN"
        const val ACTION_STOP_SCAN = "com.example.live.ACTION_STOP_SCAN"
        const val NOTIFICATION_CHANNEL_ID = "stream_health_channel"
        const val NOTIFICATION_ID = 2001

        fun startScan(context: Context) {
            val intent = Intent(context, StreamHealthService::class.java).apply {
                action = ACTION_START_SCAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopScan(context: Context) {
            val intent = Intent(context, StreamHealthService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                val notification = buildNotification("جاري تشغيل خدمة فحص الروابط وتنظيف القنوات التالفة...")
                startForeground(NOTIFICATION_ID, notification)
                startInspecting()
            }
            ACTION_STOP_SCAN -> {
                scanJob?.cancel()
                LiveStreamRepository.stopHealthScan()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val notification = buildNotification("خدمة فحص الروابط الدورية نشطة...")
                startForeground(NOTIFICATION_ID, notification)
                startInspecting()
            }
        }
        return START_NOT_STICKY
    }

    private fun startInspecting() {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                LiveStreamRepository.performHealthScan(
                    onProgressUpdate = { current, total, deleted ->
                        val text = "فحص $current من $total قناة • تم حذف $deleted رابط تالف"
                        val updatedNotif = buildNotification(text)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, updatedNotif)
                    }
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "فحص روابط البث المباشر",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات فحص وتنظيف روابط البث المباشر التالفة في الخلفية"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("تنظيف وفحص قنوات البث المباشر")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
