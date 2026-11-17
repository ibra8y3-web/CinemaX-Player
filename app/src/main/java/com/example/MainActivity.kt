package com.example

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var incomingShareRequest by mutableStateOf<IncomingShareRequest?>(null)

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.example.cinemax.ACTION_PIP_PLAY_PAUSE"
        const val ACTION_PIP_REWIND = "com.example.cinemax.ACTION_PIP_REWIND"
        const val ACTION_PIP_FORWARD = "com.example.cinemax.ACTION_PIP_FORWARD"
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    viewModel.playerManager.togglePlayPause()
                }
                ACTION_PIP_REWIND -> {
                    viewModel.playerManager.seekRelative(-10000L)
                }
                ACTION_PIP_FORWARD -> {
                    viewModel.playerManager.seekRelative(10000L)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_REWIND)
            addAction(ACTION_PIP_FORWARD)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }

        // Dynamically update PiP parameters whenever playback or aspect ratio changes
        lifecycleScope.launch {
            combine(
                viewModel.playerManager.isPlaying,
                viewModel.isPlayerActive,
                viewModel.playerManager.videoAspectRatio
            ) { isPlaying, isPlayerActive, ratio ->
                Triple(isPlaying, isPlayerActive, ratio)
            }.collect { (isPlaying, isPlayerActive, ratio) ->
                updatePipParams(isPlaying, isPlayerActive, ratio)
            }
        }

        handleDeepLinkIntent(intent)
        handleExternalShareIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        incomingShareRequest = incomingShareRequest,
                        onConsumeIncomingShare = { incomingShareRequest = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
        handleExternalShareIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleExternalShareIntent(intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
        val uris = linkedSetOf<Uri>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (action == Intent.ACTION_SEND_MULTIPLE) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { uris.addAll(it) }
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris.add(it) }
            }
        } else {
            if (action == Intent.ACTION_SEND_MULTIPLE) {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            } else {
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { uris.add(it) }
            }
        }

        intent.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                clip.getItemAt(index).uri?.let { uris.add(it) }
            }
        }
        intent.data?.let { dataUri ->
            if (dataUri.scheme.equals("content", ignoreCase = true)) uris.add(dataUri)
        }

        if (sharedText.isNullOrBlank() && uris.isEmpty()) return
        incomingShareRequest = IncomingShareRequest(
            id = System.currentTimeMillis(),
            text = sharedText,
            uris = uris.toList(),
            mimeType = intent.type
        )
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        when (intent?.getStringExtra("EXTRA_OPEN_SCREEN")) {
            "prayer_tasbih" -> viewModel.navigateToPrayerTimes(3)
            "prayer_settings" -> viewModel.navigateToPrayerTimes(2)
            "prayer_times" -> viewModel.navigateToPrayerTimes(0)
        }
    }

    private fun buildPipParams(isPlaying: Boolean, isPlayerActive: Boolean, aspectRatio: Float): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val clampedRatio = aspectRatio.coerceIn(0.42f, 2.38f)
        val rational = Rational((clampedRatio * 1000).toInt(), 1000)

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)

        // Actions
        val rewindIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_PIP_REWIND).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardIntent = PendingIntent.getBroadcast(
            this,
            3,
            Intent(ACTION_PIP_FORWARD).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewindAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_rew),
            "10 ثواني للوراء",
            "10 ثواني للوراء",
            rewindIntent
        )
        val playPauseAction = RemoteAction(
            Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "إيقاف مؤقت" else "تشغيل",
            if (isPlaying) "إيقاف مؤقت" else "تشغيل",
            playPauseIntent
        )
        val forwardAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_ff),
            "10 ثواني للأمام",
            "10 ثواني للأمام",
            forwardIntent
        )

        builder.setActions(listOf(rewindAction, playPauseAction, forwardAction))

        // Android 12+ (API 31+) seamless auto-enter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlayerActive && isPlaying)
        }

        return builder.build()
    }

    private fun updatePipParams(isPlaying: Boolean, isPlayerActive: Boolean, aspectRatio: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = buildPipParams(isPlaying, isPlayerActive, aspectRatio) ?: return
            try {
                setPictureInPictureParams(params)
            } catch (_: Exception) {}
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPlaying = viewModel.playerManager.isPlaying.value
            val isPlayerActive = viewModel.isPlayerActive.value
            val ratio = viewModel.playerManager.videoAspectRatio.value
            val params = buildPipParams(isPlaying, isPlayerActive, ratio)
            try {
                if (params != null) {
                    enterPictureInPictureMode(params)
                } else {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If watching a video, enter PiP when leaving the app
        if (viewModel.isPlayerActive.value && viewModel.playerManager.isPlaying.value) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setInPipMode(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
    }
}
