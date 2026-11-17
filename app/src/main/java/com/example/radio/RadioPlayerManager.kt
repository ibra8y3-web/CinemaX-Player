package com.example.radio

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.example.data.model.RadioCategory
import com.example.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@UnstableApi
class RadioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var sleepTimerJob: Job? = null
    private var dataTrackerJob: Job? = null
    private var autoRetryJob: Job? = null

    private var exoPlayer: ExoPlayer? = null
    private var isUsingBackupUrl = false
    private var retryCount = 0
    private var wasPlayingBeforeNetworkLost = false

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _dataConsumedBytes = MutableStateFlow(0L)
    val dataConsumedBytes: StateFlow<Long> = _dataConsumedBytes.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _networkBundleMode = MutableStateFlow(NetworkBundleMode.NORMAL)
    val networkBundleMode: StateFlow<NetworkBundleMode> = _networkBundleMode.asStateFlow()

    private val prefs = context.getSharedPreferences("nova_radio_prefs", Context.MODE_PRIVATE)

    var onVideoPlaybackStarted: (() -> Unit)? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        val savedFavs = prefs.getStringSet("favorite_radio_ids", emptySet()) ?: emptySet()
        _favorites.value = savedFavs
        val savedBundle = prefs.getString("network_bundle_mode", "normal")
        _networkBundleMode.value = NetworkBundleMode.fromId(savedBundle)
        setupNetworkMonitoring()
        initPlayer()
    }

    private fun setupNetworkMonitoring() {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        _isNetworkAvailable.value = true
                        _playbackError.value = null
                        if (wasPlayingBeforeNetworkLost && !_isPlaying.value) {
                            val current = _currentStation.value
                            if (current != null) {
                                playStation(current)
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    scope.launch {
                        _isNetworkAvailable.value = false
                        if (_isPlaying.value) {
                            wasPlayingBeforeNetworkLost = true
                            _playbackError.value = "انقطع الاتصال بالإنترنت. جاري الانتظار لإعادة الاتصال تلقائياً..."
                        }
                    }
                }
            }
            networkCallback?.let { connectivityManager?.registerNetworkCallback(request, it) }
        } catch (_: Exception) {}
    }

    private fun isNetworkConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            // Allow general internet OR cellular transport (crucial for zero-rated carrier packages)
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            true
        }
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Ultra-low network buffer tuning for instant start and minimal data consumption
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,   // Min buffer: 2 seconds
                15000,  // Max buffer: 15 seconds (keeps data usage tiny!)
                1000,   // Buffer for playback start: 1 second
                1500    // Buffer for rebuffer: 1.5 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mode = _networkBundleMode.value
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(mode.userAgent)
            .setDefaultRequestProperties(mode.customHeaders)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val loadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy(3)
        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            _isLoading.value = false
                            _playbackError.value = null
                            retryCount = 0
                            wasPlayingBeforeNetworkLost = false
                            startDataTracker()
                            startForegroundService()
                        } else {
                            stopDataTracker()
                            updateForegroundService()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                _isLoading.value = true
                                _playbackError.value = null
                            }
                            Player.STATE_READY -> {
                                _isLoading.value = false
                                _playbackError.value = null
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                                _isLoading.value = false
                            }
                            Player.STATE_IDLE -> {
                                _isLoading.value = false
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isLoading.value = false
                        _isPlaying.value = false
                        val current = _currentStation.value ?: return

                        val isNetworkIssue = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                                error.cause is UnknownHostException ||
                                error.cause is SocketTimeoutException ||
                                error.cause is IOException ||
                                !isNetworkConnected()

                        val is404 = (error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && (error.cause as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException).responseCode == 404)

                        if (!isUsingBackupUrl && current.backupUrl != null) {
                            isUsingBackupUrl = true
                            _playbackError.value = "جاري الاتصال بالخادم الاحتياطي..."
                            _isLoading.value = true
                            playUrl(current.backupUrl, current)
                            return
                        }

                        if (!is404 && retryCount < 2 && isNetworkConnected()) {
                            retryCount++
                            val delayTime = retryCount * 2000L
                            _playbackError.value = "جاري إعادة الاتصال بالبث المباشر (محاولة $retryCount/2)..."
                            _isLoading.value = true
                            autoRetryJob?.cancel()
                            autoRetryJob = scope.launch {
                                delay(delayTime)
                                playUrl(if (isUsingBackupUrl && current.backupUrl != null) current.backupUrl else current.streamUrl, current)
                            }
                        } else {
                            _playbackError.value = if (!isNetworkConnected()) {
                                "لا يوجد اتصال بالإنترنت. يرجى تفعيل Wi-Fi أو بيانات الهاتف."
                            } else if (is404) {
                                "البث الإذاعي غير متوفر حالياً على خادم المصدر (استجابة 404). يرجى اختيار محطة أخرى."
                            } else {
                                "تعذر الاتصال بمصدر البث حالياً. يرجى تجربة محطة أخرى."
                            }
                        }
                    }
                })
            }
    }

    fun playStation(station: RadioStation) {
        _currentStation.value = station
        _playbackError.value = null
        _isLoading.value = true
        isUsingBackupUrl = false
        retryCount = 0

        onVideoPlaybackStarted?.invoke() // Pause video if playing
        playUrl(station.streamUrl, station)
    }

    private fun playUrl(url: String, station: RadioStation) {
        initPlayer()
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaId(station.id)
            .build()

        exoPlayer?.apply {
            volume = 1.0f
            playWhenReady = true
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_currentStation.value == null) {
                // Default to Cairo Quran if nothing selected
                RadioCatalog.stations.firstOrNull()?.let { playStation(it) }
            } else {
                player.play()
            }
        }
    }

    fun playNext() {
        val current = _currentStation.value ?: return
        val list = RadioCatalog.stations
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playStation(list[currentIndex + 1])
        } else if (list.isNotEmpty()) {
            playStation(list.first())
        }
    }

    fun playPrevious() {
        val current = _currentStation.value ?: return
        val list = RadioCatalog.stations
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playStation(list[currentIndex - 1])
        } else if (list.isNotEmpty()) {
            playStation(list.last())
        }
    }

    fun stop() {
        autoRetryJob?.cancel()
        exoPlayer?.stop()
        _isPlaying.value = false
        _isLoading.value = false
        _currentStation.value = null
        stopDataTracker()
        stopForegroundService()
    }

    fun toggleFavorite(stationId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(stationId)) {
            current.remove(stationId)
        } else {
            current.add(stationId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorite_radio_ids", current).apply()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes

        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = 0
            return
        }

        _sleepTimerRemainingSeconds.value = minutes * 60

        sleepTimerJob = scope.launch {
            while (isActive && _sleepTimerRemainingSeconds.value > 0) {
                delay(1000)
                _sleepTimerRemainingSeconds.value -= 1
            }
            if (_sleepTimerRemainingSeconds.value <= 0) {
                stop()
                _sleepTimerMinutes.value = 0
            }
        }
    }

    private fun startDataTracker() {
        dataTrackerJob?.cancel()
        dataTrackerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay(1000)
                val bitrate = _currentStation.value?.bitrateKbps ?: 64
                // bytes per sec = (bitrate * 1000) / 8
                val bytesPerSec = (bitrate * 125L)
                _dataConsumedBytes.value += bytesPerSec
            }
        }
    }

    private fun stopDataTracker() {
        dataTrackerJob?.cancel()
    }

    private fun startForegroundService() {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_START
            _currentStation.value?.let {
                putExtra(RadioPlaybackService.EXTRA_STATION_TITLE, it.titleAr)
                putExtra(RadioPlaybackService.EXTRA_CATEGORY, it.reciterOrDesc)
                putExtra(RadioPlaybackService.EXTRA_IS_PLAYING, true)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun updateForegroundService() {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_UPDATE
            _currentStation.value?.let {
                putExtra(RadioPlaybackService.EXTRA_STATION_TITLE, it.titleAr)
                putExtra(RadioPlaybackService.EXTRA_CATEGORY, it.reciterOrDesc)
                putExtra(RadioPlaybackService.EXTRA_IS_PLAYING, _isPlaying.value)
            }
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    private fun stopForegroundService() {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }

    fun setNetworkBundleMode(mode: NetworkBundleMode) {
        if (_networkBundleMode.value == mode) return
        _networkBundleMode.value = mode
        prefs.edit().putString("network_bundle_mode", mode.id).apply()

        val current = _currentStation.value
        val wasPlaying = _isPlaying.value

        try {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        } catch (_: Exception) {}

        initPlayer()

        if (current != null && wasPlaying) {
            playStation(current)
        }
    }

    fun release() {
        stop()
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        @Volatile
        private var instance: RadioPlayerManager? = null

        fun getInstance(context: Context): RadioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: RadioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
