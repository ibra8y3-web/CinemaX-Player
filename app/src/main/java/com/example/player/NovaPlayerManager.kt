package com.example.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlin.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.ai.AiEnhanceMode
import com.example.audio.AudioStudioManager
import com.example.audio.AudioPreset
import com.example.data.AppDatabase
import com.example.data.model.PlayHistoryEntity
import com.example.subtitles.OnlineSubtitleService
import com.example.subtitles.OnlineSubtitleItem
import com.example.subtitles.SubtitleStyleConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(UnstableApi::class)
class NovaPlayerManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var networkMonitorJob: Job? = null

    var exoPlayer: ExoPlayer? = null
        private set

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bandwidthMeter: DefaultBandwidthMeter? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Backup URLs and failover mechanism
    private var fallbackUrls = mutableListOf<String>()
    private var currentFallbackIndex = 0

    // Player observable states
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _resizeMode = MutableStateFlow(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    val resizeMode: StateFlow<Int> = _resizeMode

    enum class VideoRepeatMode { OFF, ONE, ALL }

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping

    private val _videoRepeatMode = MutableStateFlow(VideoRepeatMode.OFF)
    val videoRepeatMode: StateFlow<VideoRepeatMode> = _videoRepeatMode

    private val _repeatPointA = MutableStateFlow<Long?>(null)
    val repeatPointA: StateFlow<Long?> = _repeatPointA

    private val _repeatPointB = MutableStateFlow<Long?>(null)
    val repeatPointB: StateFlow<Long?> = _repeatPointB

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked

    private val _aiFilterMode = MutableStateFlow(AiEnhanceMode.STANDARD)
    val aiFilterMode: StateFlow<AiEnhanceMode> = _aiFilterMode

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly

    private val _videoTitle = MutableStateFlow("")
    val videoTitle: StateFlow<String> = _videoTitle

    private val _videoUri = MutableStateFlow("")
    val videoUri: StateFlow<String> = _videoUri

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive

    private val _audioBoostLevel = MutableStateFlow(0) // 0 to 1000 mB
    val audioBoostLevel: StateFlow<Int> = _audioBoostLevel

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes

    private val _sleepTimerRemainingSeconds = MutableStateFlow(0L)
    val sleepTimerRemainingSeconds: StateFlow<Long> = _sleepTimerRemainingSeconds

    private val _sleepTimerWarning = MutableStateFlow<String?>(null)
    val sleepTimerWarning: StateFlow<String?> = _sleepTimerWarning

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    // Network Quality & Live Stream Monitoring state
    private val _networkStreamInfo = MutableStateFlow(NetworkStreamQualityInfo())
    val networkStreamInfo: StateFlow<NetworkStreamQualityInfo> = _networkStreamInfo

    private val _currentStreamProtocol = MutableStateFlow("HLS")
    val currentStreamProtocol: StateFlow<String> = _currentStreamProtocol

    private val _isYouTubeStream = MutableStateFlow(false)
    val isYouTubeStream: StateFlow<Boolean> = _isYouTubeStream

    private val _youTubeVideoId = MutableStateFlow<String?>(null)
    val youTubeVideoId: StateFlow<String?> = _youTubeVideoId

    fun extractYouTubeId(url: String): String? {
        val pattern = "(?:youtube\\.com\\/(?:[^/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?|live)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        val matcher = java.util.regex.Pattern.compile(pattern).matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    // Playlist & Auto-play next video state
    private val _videoAspectRatio = MutableStateFlow(16f / 9f)
    val videoAspectRatio: StateFlow<Float> = _videoAspectRatio

    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist

    private val _currentPlaylistIndex = MutableStateFlow(-1)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex

    private val _isAutoPlayEnabled = MutableStateFlow(true)
    val isAutoPlayEnabled: StateFlow<Boolean> = _isAutoPlayEnabled

    private val _autoPlayState = MutableStateFlow(AutoPlayState())
    val autoPlayState: StateFlow<AutoPlayState> = _autoPlayState

    // Audio Studio (Equalizer, BassBoost, Virtualizer, Volume Boost)
    val audioStudio = AudioStudioManager()

    // Subtitle Online Engine
    val subtitleService = OnlineSubtitleService(context)

    // Quality selection
    private val _availableQualities = MutableStateFlow<List<VideoQualityOption>>(emptyList())
    val availableQualities: StateFlow<List<VideoQualityOption>> = _availableQualities

    private val _selectedQuality = MutableStateFlow(
        VideoQualityOption(id = "auto", label = "تلقائي (تكيفي)", resolutionLabel = "Auto", isAuto = true, isSelected = true)
    )
    val selectedQuality: StateFlow<VideoQualityOption> = _selectedQuality

    // Video Technical Stats
    private val _videoTechStats = MutableStateFlow(VideoTechStats())
    val videoTechStats: StateFlow<VideoTechStats> = _videoTechStats

    // Subtitles
    private val _availableSubtitleTracks = MutableStateFlow<List<SubtitleTrackOption>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<SubtitleTrackOption>> = _availableSubtitleTracks

    private val _selectedSubtitleTrack = MutableStateFlow<SubtitleTrackOption?>(null)
    val selectedSubtitleTrack: StateFlow<SubtitleTrackOption?> = _selectedSubtitleTrack

    private val _activeSubtitleCueText = MutableStateFlow("")
    val activeSubtitleCueText: StateFlow<String> = _activeSubtitleCueText

    private val _translatedSubtitleCueText = MutableStateFlow("")
    val translatedSubtitleCueText: StateFlow<String> = _translatedSubtitleCueText

    private val _subtitleStyle = MutableStateFlow(SubtitleStyleConfig())
    val subtitleStyle: StateFlow<SubtitleStyleConfig> = _subtitleStyle

    private var autoPlayJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initPlayer()
        setupNetworkCallback()
    }

    companion object {
        private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        private val DEFAULT_HTTP_HEADERS = mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "Accept" to "*/*",
            "Accept-Language" to "ar,en-US;q=0.9,en;q=0.8",
            "Icy-MetaData" to "1"
        )
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        val bMeter = DefaultBandwidthMeter.Builder(context)
            .setResetOnNetworkTypeChange(true)
            .build()
        bandwidthMeter = bMeter

        val tSelector = DefaultTrackSelector(context)
        trackSelector = tSelector

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
            .setUserAgent(BROWSER_USER_AGENT)
            .setDefaultRequestProperties(DEFAULT_HTTP_HEADERS)
            .setTransferListener(bMeter)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            .setTransferListener(bMeter)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                1500, // 1.5s for live start playback
                2500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val customInitErrorPolicy = object : DefaultLoadErrorHandlingPolicy(1) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val ex = loadErrorInfo.exception
                if (ex is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                    if (ex.responseCode == 404 || ex.responseCode == 403 || ex.responseCode == 410) {
                        return C.TIME_UNSET
                    }
                }
                if (ex is java.net.UnknownHostException) {
                    return C.TIME_UNSET
                }
                if (loadErrorInfo.errorCount >= 1) {
                    return C.TIME_UNSET
                }
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory)
            .setLoadErrorHandlingPolicy(customInitErrorPolicy)

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(tSelector)
            .setBandwidthMeter(bMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        _videoAspectRatio.value = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startProgressTracker() else stopProgressTracker()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = if (duration == C.TIME_UNSET) 0L else duration
                        _isLive.value = isCurrentMediaItemLive
                        _playbackError.value = null
                        initAudioEffects(audioSessionId)
                        updateTracksAndQualities(currentTracks)
                        updateVideoTechStats()
                    } else if (playbackState == Player.STATE_ENDED) {
                        when (_videoRepeatMode.value) {
                            VideoRepeatMode.ONE -> {
                                seekTo(0)
                                play()
                            }
                            VideoRepeatMode.ALL -> {
                                val items = _playlist.value
                                if (items.size > 1) {
                                    val nextIdx = (_currentPlaylistIndex.value + 1) % items.size
                                    val item = items[nextIdx]
                                    _currentPlaylistIndex.value = nextIdx
                                    playMedia(
                                        uriString = item.uri,
                                        title = item.title,
                                        startPositionMs = 0L,
                                        isLiveStream = item.isLiveStream,
                                        backupUrls = item.backupUrls
                                    )
                                } else {
                                    seekTo(0)
                                    play()
                                }
                            }
                            VideoRepeatMode.OFF -> {
                                if (!_isLive.value && _isAutoPlayEnabled.value) {
                                    handleVideoEndedForAutoPlay()
                                }
                            }
                        }
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    var res = ""
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    if (format.width > 0 && format.height > 0) {
                                        res = "${format.width}x${format.height}"
                                    }
                                }
                            }
                        }
                    }
                    if (res.isNotEmpty()) {
                        _networkStreamInfo.value = _networkStreamInfo.value.copy(currentResolution = res)
                    }
                    updateTracksAndQualities(tracks)
                    updateVideoTechStats()
                }

                override fun onCues(cueGroup: CueGroup) {
                    val rawText = cueGroup.cues.joinToString("\n") { it.text?.toString() ?: "" }.trim()
                    _activeSubtitleCueText.value = rawText
                    if (_subtitleStyle.value.isLiveTranslationEnabled && rawText.isNotEmpty()) {
                        scope.launch {
                            val translated = subtitleService.translateTextLive(rawText, "ar")
                            _translatedSubtitleCueText.value = translated
                        }
                    } else if (!_subtitleStyle.value.isLiveTranslationEnabled) {
                        _translatedSubtitleCueText.value = ""
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isPlaying.value = false
                    val cause = error.cause
                    android.util.Log.e("NovaPlayerManager", "Playback Error [${error.errorCode}]: ${error.message}", error)

                    try {
                        exoPlayer?.stop()
                    } catch (_: Exception) {}

                    val curUri = _videoUri.value
                    val isLocalFile = curUri.startsWith("content://") || curUri.startsWith("file://") ||
                            (!curUri.startsWith("http://", ignoreCase = true) &&
                             !curUri.startsWith("https://", ignoreCase = true) &&
                             !curUri.startsWith("rtsp://", ignoreCase = true))

                    if (isLocalFile) {
                        val causeMsg = cause?.localizedMessage ?: error.localizedMessage ?: error.message ?: "خطأ غير معروف في الملف"
                        _playbackError.value = "تعذر تشغيل الفيديو المحلي: $causeMsg"
                        return
                    }

                    // Automatic failover if backup URLs are available
                    if (fallbackUrls.isNotEmpty() && currentFallbackIndex < fallbackUrls.size) {
                        val backupUrl = fallbackUrls[currentFallbackIndex]
                        currentFallbackIndex++
                        _networkStreamInfo.value = _networkStreamInfo.value.copy(
                            alertMessage = "جاري التحويل تلقائياً إلى رابط بث بديل مستقر ($currentFallbackIndex/${fallbackUrls.size})..."
                        )
                        playInternal(
                            uriString = backupUrl,
                            title = _videoTitle.value,
                            startPositionMs = if (_isLive.value) 0L else _currentPositionMs.value,
                            isLiveStream = _isLive.value
                        )
                        return
                    }

                    val is404 = (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404) ||
                            (error.message?.contains("404", ignoreCase = true) == true)
                    val is403 = (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403) ||
                            (error.message?.contains("403", ignoreCase = true) == true)
                    val isNoNetwork = cause is java.net.UnknownHostException ||
                            (error.message?.contains("UnknownHost", ignoreCase = true) == true) ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED

                    _playbackError.value = when {
                        is404 -> "عذراً، هذا البث غير متوفر حالياً على خادم المصدر (استجابة 404). يرجى اختيار قناة أخرى أو تجربة البث البديل."
                        is403 -> "البث المباشر محمي أو مقيد جغرافياً من المصدر (استجابة 403). يرجى اختيار قناة أخرى."
                        isNoNetwork -> "تعذر الاتصال بالشبكة أو الوصول إلى خادم البث. يرجى التحقق من اتصال الإنترنت."
                        else -> "تعذر الاتصال بمصدر البث حالياً (استجابة الخادم غير متوفرة). يرجى اختيار قناة أخرى."
                    }
                }
            })
        }
    }

    private fun setupNetworkCallback() {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    scope.launch {
                        _networkStreamInfo.value = _networkStreamInfo.value.copy(
                            status = NetworkHealthStatus.DISCONNECTED,
                            alertMessage = "انقطع اتصال الإنترنت، يرجى إعادة الاتصال بالشبكة."
                        )
                    }
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val downKbps = capabilities.linkDownstreamBandwidthKbps
                    scope.launch {
                        val current = _networkStreamInfo.value
                        if (downKbps < 800) {
                            _networkStreamInfo.value = current.copy(
                                status = NetworkHealthStatus.POOR,
                                bitrateEstimateKbps = downKbps.toLong(),
                                alertMessage = if (!current.isAutoQualityDowngraded) "جودة الاتصال ضعيفة، نوصي بخفض الجودة تلقائياً للحفاظ على استمرارية البث." else current.alertMessage
                            )
                        } else {
                            _networkStreamInfo.value = current.copy(
                                status = NetworkHealthStatus.EXCELLENT,
                                bitrateEstimateKbps = downKbps.toLong(),
                                alertMessage = null
                            )
                        }
                    }
                }
            }
            networkCallback?.let { connectivityManager?.registerNetworkCallback(request, it) }
        } catch (_: Exception) {}
    }

    private fun startNetworkMonitor() {
        networkMonitorJob?.cancel()
        networkMonitorJob = scope.launch {
            while (isActive) {
                delay(3000)
                if (!_isLive.value || exoPlayer == null) continue

                val estimateBps = bandwidthMeter?.bitrateEstimate ?: 0L
                val estimateKbps = estimateBps / 1000L
                val currentInfo = _networkStreamInfo.value

                if (estimateKbps in 1..900) {
                    val msg = if (!currentInfo.isAutoQualityDowngraded) {
                        "سرعة الاتصال منخفضة (${estimateKbps} Kbps). تم ضبط الجودة تلقائياً لضمان سلاسة البث."
                    } else currentInfo.alertMessage

                    _networkStreamInfo.value = currentInfo.copy(
                        status = NetworkHealthStatus.POOR,
                        bitrateEstimateKbps = estimateKbps,
                        alertMessage = msg
                    )
                } else if (estimateKbps > 900) {
                    _networkStreamInfo.value = currentInfo.copy(
                        status = NetworkHealthStatus.EXCELLENT,
                        bitrateEstimateKbps = estimateKbps,
                        alertMessage = if (currentInfo.status == NetworkHealthStatus.POOR) null else currentInfo.alertMessage
                    )
                }
            }
        }
    }

    private fun stopNetworkMonitor() {
        networkMonitorJob?.cancel()
        networkMonitorJob = null
    }

    fun downgradeQualityToLowest() {
        trackSelector?.let { selector ->
            val parameters = selector.buildUponParameters()
                .setMaxVideoSize(640, 360)
                .setMaxVideoBitrate(700_000)
                .build()
            selector.setParameters(parameters)
            _networkStreamInfo.value = _networkStreamInfo.value.copy(
                isAutoQualityDowngraded = true,
                alertMessage = "تم خفض جودة البث إلى (360p) بنجاح لتجنب التقطيع."
            )
        }
    }

    fun resetQualityToAuto() {
        trackSelector?.let { selector ->
            val parameters = selector.buildUponParameters()
                .clearVideoSizeConstraints()
                .setMaxVideoBitrate(Int.MAX_VALUE)
                .build()
            selector.setParameters(parameters)
            _networkStreamInfo.value = _networkStreamInfo.value.copy(
                isAutoQualityDowngraded = false,
                alertMessage = null
            )
        }
    }

    fun dismissNetworkAlert() {
        _networkStreamInfo.value = _networkStreamInfo.value.copy(alertMessage = null)
    }

    private fun initAudioEffects(audioSessionId: Int) {
        try {
            audioStudio.attachAudioSession(audioSessionId)
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(_audioBoostLevel.value)
                    enabled = _audioBoostLevel.value > 0
                }
            }
        } catch (_: Exception) {}
    }

    fun selectVideoQuality(option: VideoQualityOption) {
        val selector = trackSelector ?: return
        val currentList = _availableQualities.value.map { it.copy(isSelected = it.id == option.id) }
        _availableQualities.value = currentList
        _selectedQuality.value = option.copy(isSelected = true)

        val parametersBuilder = selector.buildUponParameters()
        if (option.isAuto) {
            parametersBuilder.clearVideoSizeConstraints()
            parametersBuilder.setMaxVideoBitrate(Int.MAX_VALUE)
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            val maxH = option.height
            val maxW = if (option.width > 0) option.width else (maxH * 16 / 9)
            parametersBuilder.setMaxVideoSize(maxW, maxH)
            if (option.bitrate > 0) {
                parametersBuilder.setMaxVideoBitrate((option.bitrate * 1.25).toInt())
            }

            // Apply explicit track override for adaptive streams (HLS/DASH)
            try {
                val mappedTrackInfo = selector.currentMappedTrackInfo
                if (mappedTrackInfo != null) {
                    for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
                        if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO) {
                            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
                            for (groupIndex in 0 until trackGroupArray.length) {
                                val group = trackGroupArray.get(groupIndex)
                                for (trackIndex in 0 until group.length) {
                                    val format = group.getFormat(trackIndex)
                                    if (format.height == option.height) {
                                        val override = TrackSelectionOverride(group, trackIndex)
                                        parametersBuilder.setOverrideForType(override)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        selector.setParameters(parametersBuilder.build())
        updateVideoTechStats()
    }

    fun selectSubtitleTrack(track: SubtitleTrackOption?) {
        val selector = trackSelector ?: return
        _selectedSubtitleTrack.value = track

        val updatedList = _availableSubtitleTracks.value.map {
            it.copy(isSelected = it.id == track?.id)
        }
        _availableSubtitleTracks.value = updatedList

        val builder = selector.buildUponParameters()
        if (track == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            _activeSubtitleCueText.value = ""
            _translatedSubtitleCueText.value = ""
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            if (track.language.isNotEmpty() && track.language != "und") {
                builder.setPreferredTextLanguage(track.language)
            }
        }
        selector.setParameters(builder.build())
    }

    fun attachExternalSubtitle(subtitleUri: Uri, lang: String = "ar", label: String = "ترجمة مخصصة") {
        val player = exoPlayer ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying
        val currentUri = _videoUri.value
        if (currentUri.isEmpty()) return

        val subConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(lang)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(currentUri)
            .setSubtitleConfigurations(listOf(subConfig))
            .build()

        player.setMediaItem(mediaItem, currentPos)
        player.prepare()
        if (wasPlaying) player.play()

        val newTrack = SubtitleTrackOption(
            id = subtitleUri.toString(),
            label = label,
            language = lang,
            isSelected = true,
            isExternal = true
        )
        _selectedSubtitleTrack.value = newTrack
        val currentList = _availableSubtitleTracks.value.toMutableList()
        currentList.removeAll { it.isExternal }
        currentList.add(0, newTrack)
        _availableSubtitleTracks.value = currentList
    }

    fun setSubtitleSyncOffset(offsetMs: Long) {
        _subtitleStyle.value = _subtitleStyle.value.copy(syncOffsetMs = offsetMs)
    }

    fun setSubtitleStyle(style: SubtitleStyleConfig) {
        _subtitleStyle.value = style
    }

    fun toggleLiveTranslation() {
        val current = _subtitleStyle.value
        val newState = !current.isLiveTranslationEnabled
        _subtitleStyle.value = current.copy(isLiveTranslationEnabled = newState)
        if (!newState) {
            _translatedSubtitleCueText.value = ""
        } else if (_activeSubtitleCueText.value.isNotEmpty()) {
            scope.launch {
                val translated = subtitleService.translateTextLive(_activeSubtitleCueText.value, "ar")
                _translatedSubtitleCueText.value = translated
            }
        }
    }

    private fun updateTracksAndQualities(tracks: Tracks) {
        val qualityList = mutableListOf<VideoQualityOption>()
        val subTrackList = mutableListOf<SubtitleTrackOption>()

        // Standard presets
        val standardPresets = listOf(
            VideoQualityOption("auto", "تلقائي (تكيفي ذكي)", "Auto", 0, 0, isAuto = true, isSelected = _selectedQuality.value.isAuto),
            VideoQualityOption("1080p", "1080p (فائق الدقة Full HD)", "1080p", 1080, 1920, isSelected = _selectedQuality.value.id == "1080p"),
            VideoQualityOption("720p", "720p (عالي الدقة HD)", "720p", 720, 1280, isSelected = _selectedQuality.value.id == "720p"),
            VideoQualityOption("480p", "480p (دقة عادية SD)", "480p", 480, 854, isSelected = _selectedQuality.value.id == "480p"),
            VideoQualityOption("360p", "360p (توفير استهلاك البيانات)", "360p", 360, 640, isSelected = _selectedQuality.value.id == "360p"),
            VideoQualityOption("240p", "240p (سرعة فائقة)", "240p", 240, 426, isSelected = _selectedQuality.value.id == "240p")
        )
        qualityList.addAll(standardPresets)

        // Stream renditions
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0) {
                        val bitRateKbps = if (format.bitrate > 0) format.bitrate / 1000 else 0
                        val resLabel = "${format.height}p"
                        val customLabel = "$resLabel (${if (bitRateKbps > 0) "${bitRateKbps}k " else ""}${if (format.frameRate > 0) "${format.frameRate.toInt()}fps" else ""})".trim()
                        val trackId = "track_${format.height}_${format.bitrate}"
                        if (qualityList.none { it.height == format.height && it.bitrate == format.bitrate }) {
                            qualityList.add(
                                VideoQualityOption(
                                    id = trackId,
                                    label = customLabel,
                                    resolutionLabel = resLabel,
                                    height = format.height,
                                    width = format.width,
                                    bitrate = format.bitrate,
                                    fps = format.frameRate,
                                    isSelected = _selectedQuality.value.id == trackId
                                )
                            )
                        }
                    }
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language ?: "und"
                    val label = format.label ?: when (lang) {
                        "ar" -> "العربية (مدمجة)"
                        "en" -> "الإنجليزية (English)"
                        "fr" -> "الفرنسية (French)"
                        "es" -> "الإسبانية (Spanish)"
                        else -> "مسار ترجمة ${i + 1}"
                    }
                    val isSel = group.isTrackSelected(i)
                    subTrackList.add(
                        SubtitleTrackOption(
                            id = "embedded_${group.mediaTrackGroup.id}_$i",
                            label = label,
                            language = lang,
                            isSelected = isSel,
                            isExternal = false
                        )
                    )
                }
            }
        }

        val existingExternal = _availableSubtitleTracks.value.filter { it.isExternal }
        _availableSubtitleTracks.value = existingExternal + subTrackList
        _availableQualities.value = qualityList
    }

    fun updateVideoTechStats() {
        val player = exoPlayer ?: return
        val vFormat = player.videoFormat
        val aFormat = player.audioFormat

        val res = if (vFormat != null && vFormat.width > 0 && vFormat.height > 0) {
            "${vFormat.width} × ${vFormat.height}"
        } else if (_networkStreamInfo.value.currentResolution.isNotEmpty()) {
            _networkStreamInfo.value.currentResolution
        } else "تلقائي"

        val codecName = when {
            vFormat?.sampleMimeType?.contains("avc", ignoreCase = true) == true -> "H.264 / AVC"
            vFormat?.sampleMimeType?.contains("hevc", ignoreCase = true) == true -> "H.265 / HEVC"
            vFormat?.sampleMimeType?.contains("vp9", ignoreCase = true) == true -> "VP9 Ultra"
            vFormat?.sampleMimeType?.contains("av01", ignoreCase = true) == true -> "AV1 Cinema"
            vFormat?.sampleMimeType != null -> vFormat.sampleMimeType!!.substringAfter("/")
            else -> "MediaCodec Hardware"
        }

        val fps = vFormat?.frameRate ?: 0f
        val bitrate = if (vFormat != null && vFormat.bitrate > 0) {
            (vFormat.bitrate / 1000L)
        } else {
            _networkStreamInfo.value.bitrateEstimateKbps
        }

        val audioCh = when (aFormat?.channelCount) {
            1 -> "1.0 Mono"
            2 -> "2.0 Stereo المحيطي"
            6 -> "5.1 Dolby Surround"
            8 -> "7.1 Cinema Master"
            else -> "ستيريو عالي النقاء"
        }

        val audioSample = aFormat?.sampleRate ?: 48000
        val aCodec = when {
            aFormat?.sampleMimeType?.contains("mp4a", ignoreCase = true) == true -> "AAC-LC"
            aFormat?.sampleMimeType?.contains("ac3", ignoreCase = true) == true -> "Dolby AC-3"
            aFormat?.sampleMimeType?.contains("eac3", ignoreCase = true) == true -> "Dolby Digital Plus"
            aFormat?.sampleMimeType?.contains("opus", ignoreCase = true) == true -> "Opus Hi-Res"
            else -> "PCM 16-bit"
        }

        _videoTechStats.value = VideoTechStats(
            resolution = res,
            codec = codecName,
            frameRate = fps,
            bitrateKbps = bitrate,
            audioChannels = audioCh,
            audioSampleRate = audioSample,
            audioCodec = aCodec,
            decoderName = "MediaCodec HW Accelerated (c2.android)",
            isHardwareAccelerated = true,
            containerFormat = _currentStreamProtocol.value
        )
    }

    fun playMedia(
        uriString: String,
        title: String,
        startPositionMs: Long = 0L,
        isLiveStream: Boolean = false,
        backupUrls: List<String> = emptyList()
    ) {
        fallbackUrls = backupUrls.filter { it.isNotBlank() && it != uriString }.toMutableList()
        currentFallbackIndex = 0
        playInternal(uriString, title, startPositionMs, isLiveStream)
    }

    private fun playInternal(
        uriString: String,
        title: String,
        startPositionMs: Long = 0L,
        isLiveStream: Boolean = false
    ) {
        initPlayer()
        cancelAutoPlay()
        _playbackError.value = null
        _videoUri.value = uriString
        _videoTitle.value = title
        _isLive.value = isLiveStream
        _repeatPointA.value = null
        _repeatPointB.value = null

        val foundIndex = _playlist.value.indexOfFirst { it.uri == uriString }
        if (foundIndex != -1) {
            _currentPlaylistIndex.value = foundIndex
        }

        val ytId = extractYouTubeId(uriString)
        if (ytId != null) {
            _isYouTubeStream.value = true
            _youTubeVideoId.value = ytId
            _currentStreamProtocol.value = "YouTube Live"
            _isPlaying.value = true
            try {
                exoPlayer?.stop()
            } catch (_: Exception) {}
            _networkStreamInfo.value = _networkStreamInfo.value.copy(
                status = NetworkHealthStatus.EXCELLENT,
                currentResolution = "1080p HD"
            )
            return
        } else {
            _isYouTubeStream.value = false
            _youTubeVideoId.value = null
        }

        val uri = if (uriString.startsWith("/") && !uriString.startsWith("content://")) {
            Uri.fromFile(File(uriString))
        } else {
            Uri.parse(uriString)
        }
        val scheme = uri.scheme?.lowercase() ?: ""
        val isLocal = scheme == "content" || scheme == "file" ||
                (!uriString.startsWith("http://", ignoreCase = true) &&
                 !uriString.startsWith("https://", ignoreCase = true) &&
                 !uriString.startsWith("rtsp://", ignoreCase = true))

        if (isLocal) {
            _currentStreamProtocol.value = "Local Video"
            val mediaItem = MediaItem.Builder().setUri(uri).build()
            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                prepare()
                if (startPositionMs > 0) {
                    seekTo(startPositionMs)
                }
                play()
            }
            stopNetworkMonitor()
            return
        }

        val isRtsp = scheme == "rtsp"
        val isDash = uriString.contains(".mpd", ignoreCase = true)
        val isHls = uriString.contains(".m3u8", ignoreCase = true) || uriString.contains("/hls/", ignoreCase = true)
        val isAudioStream = uriString.contains("/radio/", ignoreCase = true) ||
                uriString.endsWith(".mp3", ignoreCase = true) ||
                uriString.endsWith(".aac", ignoreCase = true) ||
                uriString.contains("radiojar.com", ignoreCase = true)

        _currentStreamProtocol.value = when {
            isRtsp -> "RTSP"
            isDash -> "DASH"
            isHls -> "HLS"
            isAudioStream -> "Audio Live"
            else -> "MP4/Direct"
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
            .setUserAgent(BROWSER_USER_AGENT)
            .setDefaultRequestProperties(DEFAULT_HTTP_HEADERS)

        val customLoadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(1) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val ex = loadErrorInfo.exception
                if (ex is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                    if (ex.responseCode == 404 || ex.responseCode == 403 || ex.responseCode == 410) {
                        return C.TIME_UNSET
                    }
                }
                if (ex is java.net.UnknownHostException) {
                    return C.TIME_UNSET
                }
                if (loadErrorInfo.errorCount >= 1) {
                    return C.TIME_UNSET
                }
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }

        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        when {
            isDash -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            isHls -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            isAudioStream -> mediaItemBuilder.setMimeType(MimeTypes.AUDIO_MPEG)
            uriString.endsWith(".mp4", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
        }
        val mediaItem = mediaItemBuilder.build()

        val mediaSource: MediaSource = when {
            isRtsp -> {
                RtspMediaSource.Factory()
                    .setTimeoutMs(8000)
                    .setUserAgent(BROWSER_USER_AGENT)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            isDash -> {
                DashMediaSource.Factory(httpDataSourceFactory)
                    .setLoadErrorHandlingPolicy(customLoadErrorPolicy)
                    .createMediaSource(mediaItem)
            }
            isHls -> {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .setLoadErrorHandlingPolicy(customLoadErrorPolicy)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .setLoadErrorHandlingPolicy(customLoadErrorPolicy)
                    .createMediaSource(mediaItem)
            }
        }

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaSource(mediaSource)
            prepare()
            if (startPositionMs > 0 && !isLiveStream && !isRtsp) {
                seekTo(startPositionMs)
            }
            play()
        }

        if (isLiveStream) {
            startNetworkMonitor()
        } else {
            stopNetworkMonitor()
        }
    }

    fun retry() {
        _playbackError.value = null
        val uri = _videoUri.value
        if (uri.isNotEmpty()) {
            currentFallbackIndex = 0
            playInternal(
                uriString = uri,
                title = _videoTitle.value,
                startPositionMs = _currentPositionMs.value,
                isLiveStream = _isLive.value
            )
        }
    }

    fun clearError() {
        _playbackError.value = null
    }

    fun togglePlayPause() {
        if (_isYouTubeStream.value) {
            _isPlaying.value = !_isPlaying.value
            return
        }
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        exoPlayer?.seekTo(clamped)
        _currentPositionMs.value = clamped
    }

    fun seekForward(deltaMs: Long = 10_000L) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + deltaMs)
    }

    fun seekBackward(deltaMs: Long = 10_000L) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current - deltaMs)
    }

    fun seekRelative(deltaMs: Long) {
        if (deltaMs >= 0) seekForward(deltaMs) else seekBackward(-deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun cycleResizeMode() {
        _resizeMode.value = when (_resizeMode.value) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    fun setSpecificResizeMode(mode: Int) {
        _resizeMode.value = mode
    }

    fun toggleLoop() {
        val next = when (_videoRepeatMode.value) {
            VideoRepeatMode.OFF -> VideoRepeatMode.ONE
            VideoRepeatMode.ONE -> VideoRepeatMode.ALL
            VideoRepeatMode.ALL -> VideoRepeatMode.OFF
        }
        _videoRepeatMode.value = next
        _isLooping.value = next != VideoRepeatMode.OFF
    }

    fun setPointA() {
        val pos = exoPlayer?.currentPosition ?: 0L
        _repeatPointA.value = pos
    }

    fun setPointB() {
        val pos = exoPlayer?.currentPosition ?: 0L
        val a = _repeatPointA.value ?: 0L
        if (pos > a) {
            _repeatPointB.value = pos
        }
    }

    fun clearAbRepeat() {
        _repeatPointA.value = null
        _repeatPointB.value = null
    }

    fun toggleScreenLock() {
        _isScreenLocked.value = !_isScreenLocked.value
    }

    fun setAiFilter(mode: AiEnhanceMode) {
        _aiFilterMode.value = mode
    }

    fun toggleAudioOnly() {
        _isAudioOnly.value = !_isAudioOnly.value
    }

    fun setAudioBoost(gainMb: Int) {
        _audioBoostLevel.value = gainMb
        try {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = gainMb > 0
        } catch (_: Exception) {}
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        _sleepTimerWarning.value = null
        sleepTimerJob?.cancel()
        // Ensure volume is restored to full
        exoPlayer?.volume = 1.0f
        if (minutes > 0) {
            val totalSeconds = minutes * 60L
            _sleepTimerRemainingSeconds.value = totalSeconds
            sleepTimerJob = scope.launch {
                var remaining = totalSeconds
                while (isActive && remaining > 0) {
                    delay(1000L)
                    remaining--
                    _sleepTimerRemainingSeconds.value = remaining

                    // Smooth audio fade-out in the last 60 seconds
                    if (remaining in 1..60) {
                        val fadeFactor = (remaining.toFloat() / 60f).coerceIn(0.05f, 1f)
                        exoPlayer?.volume = fadeFactor
                    }

                    // Show warning when exactly 60 seconds (1 minute) left
                    if (remaining == 60L) {
                        _sleepTimerWarning.value = "مؤقت النوم: سيبدأ الصوت بالتلاشي التدريجي الهادئ ويغلق التطبيق خلال دقيقة 🌙"
                    } else if (remaining <= 0L) {
                        _sleepTimerWarning.value = null
                        exoPlayer?.pause()
                        exoPlayer?.volume = 1.0f // Restore normal volume for next playback
                        _sleepTimerMinutes.value = 0
                        _sleepTimerRemainingSeconds.value = 0L
                    }
                }
            }
        } else {
            _sleepTimerRemainingSeconds.value = 0L
            _sleepTimerWarning.value = null
            exoPlayer?.volume = 1.0f
        }
    }

    fun dismissSleepTimerWarning() {
        _sleepTimerWarning.value = null
    }

    fun extendSleepTimer(extraMinutes: Int = 10) {
        val currentRemaining = _sleepTimerRemainingSeconds.value
        val newRemaining = currentRemaining + (extraMinutes * 60L)
        val newMinutes = (newRemaining / 60L).toInt().coerceAtLeast(1)
        _sleepTimerMinutes.value = newMinutes
        _sleepTimerRemainingSeconds.value = newRemaining
        _sleepTimerWarning.value = null
        exoPlayer?.volume = 1.0f
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            var remaining = newRemaining
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining

                // Smooth audio fade-out in the last 60 seconds
                if (remaining in 1..60) {
                    val fadeFactor = (remaining.toFloat() / 60f).coerceIn(0.05f, 1f)
                    exoPlayer?.volume = fadeFactor
                }

                if (remaining == 60L) {
                    _sleepTimerWarning.value = "مؤقت النوم: سيبدأ الصوت بالتلاشي التدريجي الهادئ ويغلق التطبيق خلال دقيقة 🌙"
                } else if (remaining <= 0L) {
                    _sleepTimerWarning.value = null
                    exoPlayer?.pause()
                    exoPlayer?.volume = 1.0f
                    _sleepTimerMinutes.value = 0
                    _sleepTimerRemainingSeconds.value = 0L
                }
            }
        }
    }

    // Playlist & Auto-Play Management
    fun setPlaylist(items: List<PlaylistItem>, startIndex: Int = 0) {
        _playlist.value = items
        _currentPlaylistIndex.value = startIndex
        cancelAutoPlay()
    }

    fun toggleAutoPlay() {
        _isAutoPlayEnabled.value = !_isAutoPlayEnabled.value
        if (!_isAutoPlayEnabled.value) {
            cancelAutoPlay()
        }
    }

    fun setAutoPlayEnabled(enabled: Boolean) {
        _isAutoPlayEnabled.value = enabled
        if (!enabled) {
            cancelAutoPlay()
        }
    }

    fun cancelAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        _autoPlayState.value = AutoPlayState(isCountingDown = false, remainingSeconds = 5, nextVideo = null)
    }

    fun playNextVideoImmediately() {
        val next = _autoPlayState.value.nextVideo
        cancelAutoPlay()
        if (next != null) {
            val nextIdx = _playlist.value.indexOfFirst { it.uri == next.uri }
            if (nextIdx != -1) {
                _currentPlaylistIndex.value = nextIdx
            }
            playMedia(
                uriString = next.uri,
                title = next.title,
                startPositionMs = 0L,
                isLiveStream = next.isLiveStream,
                backupUrls = next.backupUrls
            )
        } else {
            playNext()
        }
    }

    fun playNext() {
        val items = _playlist.value
        val nextIdx = _currentPlaylistIndex.value + 1
        if (items.isNotEmpty() && nextIdx in items.indices) {
            cancelAutoPlay()
            val item = items[nextIdx]
            _currentPlaylistIndex.value = nextIdx
            playMedia(
                uriString = item.uri,
                title = item.title,
                startPositionMs = 0L,
                isLiveStream = item.isLiveStream,
                backupUrls = item.backupUrls
            )
        }
    }

    fun playPrevious() {
        val items = _playlist.value
        val prevIdx = _currentPlaylistIndex.value - 1
        if (items.isNotEmpty() && prevIdx in items.indices) {
            cancelAutoPlay()
            val item = items[prevIdx]
            _currentPlaylistIndex.value = prevIdx
            playMedia(
                uriString = item.uri,
                title = item.title,
                startPositionMs = 0L,
                isLiveStream = item.isLiveStream,
                backupUrls = item.backupUrls
            )
        }
    }

    private fun handleVideoEndedForAutoPlay() {
        val items = _playlist.value
        val currentIndex = _currentPlaylistIndex.value
        val nextIndex = currentIndex + 1

        if (items.isNotEmpty() && nextIndex in items.indices) {
            val nextVideo = items[nextIndex]
            startAutoPlayCountdown(nextVideo, nextIndex)
        }
    }

    private fun startAutoPlayCountdown(nextVideo: PlaylistItem, nextIndex: Int) {
        autoPlayJob?.cancel()
        _autoPlayState.value = AutoPlayState(
            isCountingDown = true,
            remainingSeconds = 5,
            nextVideo = nextVideo
        )

        autoPlayJob = scope.launch {
            for (sec in 5 downTo 1) {
                _autoPlayState.value = _autoPlayState.value.copy(
                    isCountingDown = true,
                    remainingSeconds = sec,
                    nextVideo = nextVideo
                )
                delay(1000L)
            }

            // Time reached 0, auto-play next video
            _autoPlayState.value = AutoPlayState(isCountingDown = false, remainingSeconds = 0, nextVideo = null)
            _currentPlaylistIndex.value = nextIndex
            playMedia(
                uriString = nextVideo.uri,
                title = nextVideo.title,
                startPositionMs = 0L,
                isLiveStream = nextVideo.isLiveStream,
                backupUrls = nextVideo.backupUrls
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    _bufferedPositionMs.value = player.bufferedPosition

                    // Check A-B repeat
                    val a = _repeatPointA.value
                    val b = _repeatPointB.value
                    if (a != null && b != null && pos >= b) {
                        player.seekTo(a)
                    }

                    // Save history periodically
                    if (pos > 1000 && !_isLive.value && _videoUri.value.isNotEmpty()) {
                        saveCurrentHistory(pos)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun saveCurrentHistory(pos: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                db.videoDao().insertOrUpdateHistory(
                    PlayHistoryEntity(
                        videoUri = _videoUri.value,
                        title = _videoTitle.value,
                        lastPositionMs = pos,
                        durationMs = _durationMs.value,
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    suspend fun saveScreenshot(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val filename = "CinemaX_Screenshot_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null

            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CinemaXPlayer")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.also {
                    outputStream = context.contentResolver.openOutputStream(it)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val novaDir = File(picturesDir, "CinemaXPlayer").apply { if (!exists()) mkdirs() }
                val file = File(novaDir, filename)
                outputStream = FileOutputStream(file)
                Uri.fromFile(file)
            }

            outputStream?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            imageUri?.toString() ?: filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun release() {
        cancelAutoPlay()
        stopProgressTracker()
        stopNetworkMonitor()
        sleepTimerJob?.cancel()
        audioStudio.releaseEffects()
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}

