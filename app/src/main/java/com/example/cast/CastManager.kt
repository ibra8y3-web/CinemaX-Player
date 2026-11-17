package com.example.cast

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.example.media.model.LocalVideo
import com.example.sharing.LocalHttpShareServer
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CastDeviceInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val isSelected: Boolean = false,
    val isDefault: Boolean = false
)

/**
 * Manages Google Cast integration, local network device discovery,
 * and remote playback coordination between Nova Player and Chromecast.
 */
class CastManager(
    private val context: Context,
    private val shareServer: LocalHttpShareServer
) {

    private val TAG = "NovaCastManager"
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var castContext: CastContext? = null
    private var mediaRouter: MediaRouter? = null
    private var routeSelector: MediaRouteSelector? = null

    private val _isCastAvailable = MutableStateFlow(false)
    val isCastAvailable: StateFlow<Boolean> = _isCastAvailable.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<CastDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<CastDeviceInfo>> = _availableDevices.asStateFlow()

    private val _remotePlaybackPositionMs = MutableStateFlow(0L)
    val remotePlaybackPositionMs: StateFlow<Long> = _remotePlaybackPositionMs.asStateFlow()

    private val _remoteDurationMs = MutableStateFlow(0L)
    val remoteDurationMs: StateFlow<Long> = _remoteDurationMs.asStateFlow()

    private val _isRemotePlaying = MutableStateFlow(false)
    val isRemotePlaying: StateFlow<Boolean> = _isRemotePlaying.asStateFlow()

    private val _currentMediaTitle = MutableStateFlow("")
    val currentMediaTitle: StateFlow<String> = _currentMediaTitle.asStateFlow()

    // Callbacks to sync with ExoPlayer
    var onCastStarted: ((positionMs: Long) -> Unit)? = null
    var onCastEnded: ((positionMs: Long) -> Unit)? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Log.d(TAG, "Cast session starting...")
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.d(TAG, "Cast session started: $sessionId")
            handleSessionConnected(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.w(TAG, "Cast session start failed: $error")
            _isCasting.value = false
            _connectedDeviceName.value = null
        }

        override fun onSessionEnding(session: CastSession) {
            Log.d(TAG, "Cast session ending...")
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.d(TAG, "Cast session ended")
            handleSessionDisconnected()
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            handleSessionConnected(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            handleSessionDisconnected()
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    private val mediaRouterCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshAvailableDevices()
        }

        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshAvailableDevices()
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshAvailableDevices()
        }

        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshAvailableDevices()
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            refreshAvailableDevices()
        }
    }

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updateRemotePlaybackState()
        }

        override fun onMetadataUpdated() {
            updateRemotePlaybackState()
        }
    }

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            val rmc = currentRemoteMediaClient()
            if (rmc != null && _isCasting.value) {
                _remotePlaybackPositionMs.value = rmc.approximateStreamPosition
                val status = rmc.mediaStatus
                _isRemotePlaying.value = status?.playerState == com.google.android.gms.cast.MediaStatus.PLAYER_STATE_PLAYING
                mainHandler.postDelayed(this, 1000)
            }
        }
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(context.applicationContext)
            castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)

            mediaRouter = MediaRouter.getInstance(context.applicationContext)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build()

            _isCastAvailable.value = true
            startDeviceScanning()

            // Check if already in a session
            castContext?.sessionManager?.currentCastSession?.let {
                if (it.isConnected) {
                    handleSessionConnected(it)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CastContext init failed or Google Play Services Cast unavailable: ${e.message}")
            _isCastAvailable.value = false
        }
    }

    fun startDeviceScanning() {
        try {
            val router = mediaRouter ?: return
            val selector = routeSelector ?: return
            router.addCallback(selector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
            refreshAvailableDevices()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start device scanning", e)
        }
    }

    fun stopDeviceScanning() {
        try {
            mediaRouter?.removeCallback(mediaRouterCallback)
        } catch (_: Exception) {}
    }

    fun refreshAvailableDevices() {
        try {
            val router = mediaRouter ?: return
            val selector = routeSelector ?: return
            val routes = router.routes
            val castRoutes = mutableListOf<CastDeviceInfo>()

            for (route in routes) {
                if (route.matchesSelector(selector) && !route.isDefault) {
                    castRoutes.add(
                        CastDeviceInfo(
                            id = route.id,
                            name = route.name,
                            description = route.description ?: "",
                            isSelected = route.isSelected,
                            isDefault = route.isDefault
                        )
                    )
                }
            }
            _availableDevices.value = castRoutes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh devices", e)
        }
    }

    fun selectDevice(device: CastDeviceInfo) {
        try {
            val router = mediaRouter ?: return
            val route = router.routes.find { it.id == device.id }
            if (route != null) {
                router.selectRoute(route)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select route: ${device.name}", e)
        }
    }

    fun disconnect() {
        try {
            val router = mediaRouter
            if (router != null) {
                router.selectRoute(router.defaultRoute)
            }
            castContext?.sessionManager?.endCurrentSession(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect Cast", e)
        }
    }

    private fun handleSessionConnected(session: CastSession) {
        _isCasting.value = true
        _connectedDeviceName.value = session.castDevice?.friendlyName ?: "جهاز Chromecast"
        val rmc = session.remoteMediaClient
        rmc?.registerCallback(remoteMediaClientCallback)
        mainHandler.removeCallbacks(positionUpdateRunnable)
        mainHandler.post(positionUpdateRunnable)

        val currentPos = _remotePlaybackPositionMs.value
        onCastStarted?.invoke(currentPos)
    }

    private fun handleSessionDisconnected() {
        _isCasting.value = false
        val deviceName = _connectedDeviceName.value
        _connectedDeviceName.value = null
        mainHandler.removeCallbacks(positionUpdateRunnable)

        val lastPos = _remotePlaybackPositionMs.value
        onCastEnded?.invoke(lastPos)
    }

    /**
     * Casts a video to the connected Chromecast device.
     * If the video is a local video, starts the local HTTP share server and streams it.
     * If the video is an online URL, streams it directly.
     */
    fun castMedia(
        uriString: String,
        title: String,
        localVideo: LocalVideo? = null,
        startPositionMs: Long = 0L,
        isLiveStream: Boolean = false
    ) {
        val session = castContext?.sessionManager?.currentCastSession ?: return
        if (!session.isConnected) return

        val rmc = session.remoteMediaClient ?: return
        _currentMediaTitle.value = title

        scope.launch(Dispatchers.IO) {
            val finalStreamUrl: String
            val mimeType: String

            val isLocal = uriString.startsWith("content://") ||
                    uriString.startsWith("file://") ||
                    uriString.startsWith("/") ||
                    localVideo != null

            if (isLocal) {
                // Ensure local HTTP streaming server is running on local Wi-Fi
                if (!shareServer.isRunning.value) {
                    shareServer.start(8080)
                }
                val serverBase = shareServer.serverUrl.value ?: "http://127.0.0.1:8080"
                val videoId = localVideo?.id ?: System.currentTimeMillis()
                finalStreamUrl = "$serverBase/stream/$videoId"
                mimeType = "video/mp4"
            } else {
                finalStreamUrl = uriString
                mimeType = when {
                    uriString.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
                    uriString.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
                    uriString.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                    else -> "video/mp4"
                }
            }

            val metadata = MediaMetadata(
                if (mimeType.startsWith("audio/")) MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
                else MediaMetadata.MEDIA_TYPE_MOVIE
            ).apply {
                putString(MediaMetadata.KEY_TITLE, title)
                putString(MediaMetadata.KEY_SUBTITLE, if (isLiveStream) "بث حي مباشر" else "CinemaX Player")
            }

            val mediaInfo = MediaInfo.Builder(finalStreamUrl)
                .setStreamType(if (isLiveStream) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType)
                .setMetadata(metadata)
                .build()

            val loadRequestData = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setCurrentTime(if (isLiveStream) 0L else startPositionMs)
                .setAutoplay(true)
                .build()

            mainHandler.post {
                try {
                    rmc.load(loadRequestData)
                    _isRemotePlaying.value = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load media into RemoteMediaClient", e)
                }
            }
        }
    }

    fun play() {
        currentRemoteMediaClient()?.play()
    }

    fun pause() {
        currentRemoteMediaClient()?.pause()
    }

    fun togglePlayPause() {
        val rmc = currentRemoteMediaClient() ?: return
        if (_isRemotePlaying.value) {
            rmc.pause()
        } else {
            rmc.play()
        }
    }

    fun seekTo(positionMs: Long) {
        currentRemoteMediaClient()?.seek(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        val current = _remotePlaybackPositionMs.value
        val duration = _remoteDurationMs.value
        val target = (current + offsetMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        seekTo(target)
    }

    fun setVolume(volume: Double) {
        try {
            val session = castContext?.sessionManager?.currentCastSession
            session?.setVolume(volume.coerceIn(0.0, 1.0))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set cast volume", e)
        }
    }

    private fun currentRemoteMediaClient(): RemoteMediaClient? {
        return castContext?.sessionManager?.currentCastSession?.remoteMediaClient
    }

    private fun updateRemotePlaybackState() {
        val rmc = currentRemoteMediaClient() ?: return
        val status = rmc.mediaStatus
        if (status != null) {
            _isRemotePlaying.value = status.playerState == com.google.android.gms.cast.MediaStatus.PLAYER_STATE_PLAYING
            _remotePlaybackPositionMs.value = rmc.approximateStreamPosition
            _remoteDurationMs.value = rmc.streamDuration
        }
    }

    fun release() {
        try {
            mainHandler.removeCallbacks(positionUpdateRunnable)
            stopDeviceScanning()
            castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        } catch (_: Exception) {}
    }
}
