package com.example.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.util.UnstableApi
import com.example.MainActivity

@UnstableApi
class RadioPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "nova_radio_playback_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.radio.ACTION_START"
        const val ACTION_UPDATE = "com.example.radio.ACTION_UPDATE"
        const val ACTION_PLAY = "com.example.radio.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.radio.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.radio.ACTION_NEXT"
        const val ACTION_PREV = "com.example.radio.ACTION_PREV"
        const val ACTION_STOP = "com.example.radio.ACTION_STOP"

        const val EXTRA_STATION_TITLE = "extra_station_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
    }

    private var currentTitle: String = "إذاعة القرآن الكريم"
    private var currentDesc: String = "بث صوتي فائق التوفير (أقل استهلاك نت)"
    private var isPlaying: Boolean = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                intent.getStringExtra(EXTRA_STATION_TITLE)?.let { currentTitle = it }
                intent.getStringExtra(EXTRA_CATEGORY)?.let { currentDesc = it }
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                startForegroundWithNotification()
            }
            ACTION_PLAY -> {
                RadioPlayerManager.getInstance(this).play()
                isPlaying = true
                startForegroundWithNotification()
            }
            ACTION_PAUSE -> {
                RadioPlayerManager.getInstance(this).pause()
                isPlaying = false
                startForegroundWithNotification()
            }
            ACTION_NEXT -> {
                RadioPlayerManager.getInstance(this).playNext()
            }
            ACTION_PREV -> {
                RadioPlayerManager.getInstance(this).playPrevious()
            }
            ACTION_STOP -> {
                RadioPlayerManager.getInstance(this).stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseActionIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RadioPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(currentDesc)
            .setSubText("راديو وبث صوتي مباشر (توفير نت)")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_media_previous, "السابق", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                playPauseActionIntent
            )
            .addAction(android.R.drawable.ic_media_next, "التالي", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "راديو وإذاعات CinemaX Player"
            val desc = "إشعارات التحكم في البث الإذاعي الصوتي أثناء عمله في الخلفية"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
