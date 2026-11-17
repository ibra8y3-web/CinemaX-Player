package com.example.music

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.media.model.LocalAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class MusicPlayerManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "MusicPlayerManager"

        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // Separate ExoPlayer instance dedicated to music playback ONLY (never shared with video)
    private var internalPlayer: ExoPlayer? = null

    enum class AudioRepeatMode { OFF, ALL, ONE }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<LocalAudio?>(null)
    val currentTrack: StateFlow<LocalAudio?> = _currentTrack.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<LocalAudio>>(emptyList())
    val playlist: StateFlow<List<LocalAudio>> = _playlist.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.ALL)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            Log.d(TAG, "MusicPlayer isPlaying changed: $playing, volume=${getPlayer().volume}")
            if (playing) startProgressTracker() else stopProgressTracker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "STATE_IDLE"
                Player.STATE_BUFFERING -> "STATE_BUFFERING"
                Player.STATE_READY -> "STATE_READY"
                Player.STATE_ENDED -> "STATE_ENDED"
                else -> "UNKNOWN"
            }
            Log.d(TAG, "MusicPlayer playbackState: $stateName, isPlaying=${getPlayer().isPlaying}, volume=${getPlayer().volume}")

            if (playbackState == Player.STATE_READY) {
                val dur = getPlayer().duration
                _durationMs.value = if (dur == C.TIME_UNSET) 0L else dur
                _playbackError.value = null
            } else if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
            Log.e(TAG, "MusicPlayer ERROR [code=${error.errorCode}]: ${error.message}", error)
            _playbackError.value = "خطأ في تشغيل المقطع الصوتي: ${error.localizedMessage ?: error.errorCodeName}"
        }
    }

    init {
        ensurePlayer()
    }

    private fun ensurePlayer(): ExoPlayer {
        if (internalPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            internalPlayer = ExoPlayer.Builder(appContext)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .build().apply {
                    volume = 1.0f // Explicitly ensure 1f volume and unmuted
                    addListener(playerListener)
                }
            Log.d(TAG, "Created dedicated music ExoPlayer instance with volume=1.0f and handleAudioFocus=true")
        }
        return internalPlayer!!
    }

    fun getPlayer(): ExoPlayer = ensurePlayer()

    fun attachPlayer(servicePlayer: ExoPlayer) {
        if (internalPlayer != servicePlayer) {
            internalPlayer?.removeListener(playerListener)
            internalPlayer?.release()
            internalPlayer = servicePlayer
            internalPlayer?.volume = 1.0f
            internalPlayer?.addListener(playerListener)
            Log.d(TAG, "Attached servicePlayer to MusicPlayerManager")
        }
    }

    fun detachPlayer() {
        internalPlayer?.removeListener(playerListener)
        internalPlayer = null
    }

    fun setPlaylist(list: List<LocalAudio>, startIndex: Int = 0) {
        _playlist.value = list
        if (startIndex in list.indices) {
            playTrack(list[startIndex])
        }
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun removeTrackFromPlaylist(audioId: Long) {
        val updated = _playlist.value.filterNot { it.id == audioId }
        _playlist.value = updated
        if (_currentTrack.value?.id == audioId) {
            stop()
        }
    }

    fun playTrack(audio: LocalAudio) {
        _currentTrack.value = audio
        _playbackError.value = null

        val player = ensurePlayer()
        player.volume = 1.0f

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(audio.title)
            .setArtist(audio.artist)
            .setAlbumTitle(audio.album)
            .build()

        // Use content:// URI as requested
        val mediaItem = MediaItem.Builder()
            .setUri(audio.contentUri)
            .setMediaMetadata(mediaMetadata)
            .build()

        Log.d(TAG, "Playing audio with content URI: ${audio.contentUri}, title=${audio.title}")
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Start MediaSessionService in foreground so playback plays with screen off
        startMusicService()
    }

    fun togglePlayPause() {
        val player = ensurePlayer()
        if (player.isPlaying) {
            player.pause()
        } else {
            player.volume = 1.0f
            player.play()
            startMusicService()
        }
    }

    fun play() {
        val player = ensurePlayer()
        player.volume = 1.0f
        player.play()
        startMusicService()
    }

    fun pause() {
        ensurePlayer().pause()
    }

    fun stop() {
        try {
            internalPlayer?.stop()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _currentTrack.value = null
        _currentPositionMs.value = 0L
        stopProgressTracker()
    }

    fun seekTo(positionMs: Long) {
        val player = ensurePlayer()
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    private fun handleTrackEnded() {
        val list = _playlist.value
        val cur = _currentTrack.value
        when (_repeatMode.value) {
            AudioRepeatMode.ONE -> {
                if (cur != null) {
                    seekTo(0L)
                    play()
                }
            }
            AudioRepeatMode.ALL -> {
                if (list.isNotEmpty()) playNext()
            }
            AudioRepeatMode.OFF -> {
                if (list.isEmpty()) return
                if (_isShuffleEnabled.value && list.size > 1) {
                    playNext()
                } else {
                    val curIdx = list.indexOfFirst { it.id == cur?.id }
                    if (curIdx != -1 && curIdx < list.size - 1) {
                        playTrack(list[curIdx + 1])
                    } else {
                        internalPlayer?.pause()
                        seekTo(0L)
                        _isPlaying.value = false
                    }
                }
            }
        }
    }

    fun playNext() {
        val list = _playlist.value
        val cur = _currentTrack.value
        if (list.isEmpty()) return

        if (_isShuffleEnabled.value && list.size > 1) {
            val candidates = list.filter { it.id != cur?.id }
            val randomTrack = candidates.randomOrNull() ?: list.first()
            playTrack(randomTrack)
            return
        }

        val curIdx = list.indexOfFirst { it.id == cur?.id }
        val nextIdx = if (curIdx != -1 && curIdx < list.size - 1) curIdx + 1 else 0
        playTrack(list[nextIdx])
    }

    fun playPrevious() {
        val list = _playlist.value
        val cur = _currentTrack.value
        if (list.isEmpty()) return

        val player = ensurePlayer()
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val curIdx = list.indexOfFirst { it.id == cur?.id }
        val prevIdx = if (curIdx > 0) curIdx - 1 else list.size - 1
        playTrack(list[prevIdx])
    }

    private fun startMusicService() {
        try {
            val serviceIntent = Intent(appContext, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(appContext, serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MusicPlaybackService: ${e.message}")
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                val player = internalPlayer
                if (player != null && player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition
                    val dur = player.duration
                    if (dur > 0 && dur != C.TIME_UNSET) {
                        _durationMs.value = dur
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
}
