package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.model.FavoriteEntity
import com.example.data.model.PlayHistoryEntity
import com.example.data.model.VaultItemEntity
import com.example.media.MediaScanner
import com.example.media.model.LocalVideo
import com.example.media.model.VideoFolder
import com.example.player.NovaPlayerManager
import com.example.security.VaultCryptoManager
import com.example.sharing.LocalHttpShareServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val videoDao = db.videoDao()
    private val mediaScanner = MediaScanner(application)

    val prefsManager = PreferencesManager(application)
    val cryptoManager = VaultCryptoManager(application)
    val playerManager = NovaPlayerManager(application)
    val musicPlayerManager = com.example.music.MusicPlayerManager.getInstance(application)
    val shareServer = LocalHttpShareServer(application)
    val castManager = com.example.cast.CastManager(application, shareServer)
    val radioManager = com.example.radio.RadioPlayerManager.getInstance(application)
    val prayerManager = com.example.prayer.PrayerTimesManager(application)
    val duaManager = com.example.duas.DuaManager(application)

    private val _isPlayerActive = MutableStateFlow(false)
    val isPlayerActive: StateFlow<Boolean> = _isPlayerActive

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode

    private val _requestedNavTab = MutableStateFlow<NavTab?>(null)
    val requestedNavTab: StateFlow<NavTab?> = _requestedNavTab

    private val _requestedPrayerTab = MutableStateFlow<Int?>(null)
    val requestedPrayerTab: StateFlow<Int?> = _requestedPrayerTab

    fun navigateToPrayerTimes(initialSubTab: Int = 0) {
        _requestedPrayerTab.value = initialSubTab
        _requestedNavTab.value = NavTab.ISLAMIC
    }

    fun consumeRequestedNavTab() {
        _requestedNavTab.value = null
    }

    fun consumeRequestedPrayerTab() {
        _requestedPrayerTab.value = null
    }

    fun setPlayerActive(active: Boolean) {
        _isPlayerActive.value = active
    }

    fun setInPipMode(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val videos: StateFlow<List<LocalVideo>> = _videos

    private val _audioFiles = MutableStateFlow<List<com.example.media.model.LocalAudio>>(emptyList())
    val audioFiles: StateFlow<List<com.example.media.model.LocalAudio>> = _audioFiles

    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    val folders: StateFlow<List<VideoFolder>> = _folders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val favorites: StateFlow<Set<String>> = videoDao.getAllFavorites()
        .map { list -> list.map { it.videoUri }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val vaultItems: StateFlow<List<VaultItemEntity>> = videoDao.getAllVaultItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playHistory: StateFlow<List<PlayHistoryEntity>> = videoDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        com.example.quran.QuranRepository.init(application)
        shareServer.setVideosProvider { _videos.value }
        com.example.live.LiveStreamRepository.init(application)
        castManager.onCastStarted = { _ ->
            playerManager.exoPlayer?.pause()
        }
        castManager.onCastEnded = { pos ->
            if (pos > 0) {
                playerManager.seekTo(pos)
            }
        }
        radioManager.onVideoPlaybackStarted = {
            playerManager.exoPlayer?.pause()
        }
    }

    fun loadLocalVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val list = mediaScanner.scanVideos()
            val folderList = mediaScanner.getFolders(list)
            _videos.value = list
            _folders.value = folderList
            _isLoading.value = false
        }
    }

    fun loadLocalAudioFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val audioList = mediaScanner.scanAudioFiles()
            _audioFiles.value = audioList
        }
    }

    fun toggleFavorite(video: LocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (favorites.value.contains(video.uri)) {
                videoDao.removeFavorite(video.uri)
            } else {
                videoDao.addFavorite(
                    FavoriteEntity(
                        videoUri = video.uri,
                        title = video.title,
                        durationMs = video.durationMs,
                        sizeBytes = video.sizeBytes
                    )
                )
            }
        }
    }

    fun encryptVideoToVault(video: LocalVideo, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputName = "vault_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.enc"
                val encryptedFile = cryptoManager.encryptVideo(Uri.parse(video.uri), pin, outputName)
                val item = VaultItemEntity(
                    id = UUID.randomUUID().toString(),
                    originalTitle = video.title,
                    originalPath = video.path,
                    encryptedFileName = encryptedFile.name,
                    fileSize = video.sizeBytes,
                    durationMs = video.durationMs
                )
                videoDao.insertVaultItem(item)

                // Refresh video list
                loadLocalVideos()

                viewModelScope.launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    onError(e.message ?: "فشل التشفير")
                }
            }
        }
    }

    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cryptoManager.deleteEncryptedFile(item.encryptedFileName)
            videoDao.deleteVaultItem(item.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            videoDao.clearAllHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        castManager.release()
        shareServer.stop()
        playerManager.release()
        cryptoManager.cleanTempFiles()
        duaManager.release()
    }
}

