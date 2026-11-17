package com.example.live

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.dao.LiveChannelDao
import com.example.data.model.LiveStreamChannel
import com.example.data.model.LiveStreamEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * مستودع إدارة القنوات والبث المباشر مع قاعدة بيانات Room
 * وفحص دوري ذكي لصلاحية الروابط في الخلفية وحذف الروابط التالفة تلقائياً.
 */
object LiveStreamRepository {

    private const val TAG = "LiveStreamRepo"
    private const val PREFS_NAME = "cinemax_live_prefs"
    private const val KEY_CATALOG_VERSION = "catalog_version"
    private const val KEY_DELETED_IDS = "deleted_channel_ids"
    private const val CURRENT_CATALOG_VERSION = 2

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var channelDao: LiveChannelDao? = null
    private var prefs: android.content.SharedPreferences? = null
    private var isInitialized = false

    private val _channels = MutableStateFlow<List<LiveStreamChannel>>(emptyList())
    val channels: StateFlow<List<LiveStreamChannel>> = _channels.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // Health Inspector Background Service States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scannedChannelsCount = MutableStateFlow(0)
    val scannedChannelsCount: StateFlow<Int> = _scannedChannelsCount.asStateFlow()

    private val _totalToScan = MutableStateFlow(0)
    val totalToScan: StateFlow<Int> = _totalToScan.asStateFlow()

    private val _deletedBrokenChannelsCount = MutableStateFlow(0)
    val deletedBrokenChannelsCount: StateFlow<Int> = _deletedBrokenChannelsCount.asStateFlow()

    private val _lastScanReport = MutableStateFlow<String?>("خدمة الفحص الدوري نشطة ومستعدة لحماية ونظافة القنوات")
    val lastScanReport: StateFlow<String?> = _lastScanReport.asStateFlow()

    private val _lastScanTime = MutableStateFlow(0L)
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

    private fun getDeletedIds(): Set<String> {
        return prefs?.getStringSet(KEY_DELETED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    private fun addDeletedIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val updated = getDeletedIds().toMutableSet().apply { addAll(ids) }
        prefs?.edit()?.putStringSet(KEY_DELETED_IDS, updated)?.apply()
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appCtx = context.applicationContext
        val db = AppDatabase.getDatabase(appCtx)
        val dao = db.liveChannelDao()
        channelDao = dao
        prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        scope.launch {
            val savedVersion = prefs?.getInt(KEY_CATALOG_VERSION, 1) ?: 1
            val catalog = LiveChannelsCatalog.buildCatalog()

            if (savedVersion < CURRENT_CATALOG_VERSION) {
                // Migrate away from stale v1 catalog (removes ~190 broken links while keeping favorites & custom streams)
                val existing = dao.getAllChannelsSync()
                val favUrls = existing.filter { it.isFavorite }.map { it.url }.toSet()
                val favIds = existing.filter { it.isFavorite }.map { it.id }.toSet()
                dao.deleteCatalogChannels()
                prefs?.edit()
                    ?.remove(KEY_DELETED_IDS)
                    ?.putInt(KEY_CATALOG_VERSION, CURRENT_CATALOG_VERSION)
                    ?.apply()
                val entities = catalog.map { ch ->
                    val wasFav = favIds.contains(ch.id) || favUrls.contains(ch.url)
                    LiveStreamEntity.fromChannel(ch.copy(isFavorite = wasFav))
                }
                dao.insertAllIgnore(entities)
            } else {
                val deleted = getDeletedIds()
                val entities = catalog
                    .filterNot { deleted.contains(it.id) }
                    .map { LiveStreamEntity.fromChannel(it) }
                dao.insertAllIgnore(entities)
            }

            // Observe Room DB updates reactively
            dao.getAllChannelsFlow().collect { entities ->
                val channelList = entities.map { it.toLiveStreamChannel() }
                _channels.value = channelList
                _favoriteIds.value = channelList.filter { it.isFavorite }.map { it.id }.toSet()
            }
        }
    }

    fun refreshCatalog(restoreDeleted: Boolean = false) {
        val dao = channelDao ?: return
        scope.launch {
            if (restoreDeleted) {
                prefs?.edit()?.remove(KEY_DELETED_IDS)?.apply()
            }
            val deleted = if (restoreDeleted) emptySet() else getDeletedIds()
            val catalog = LiveChannelsCatalog.buildCatalog()
            val entities = catalog
                .filterNot { deleted.contains(it.id) }
                .map { LiveStreamEntity.fromChannel(it) }
            dao.insertAllIgnore(entities)
        }
    }

    fun importCountryChannels(
        country: WorldChannelsDownloader.CountryPreset,
        onComplete: (count: Int) -> Unit = {}
    ) {
        val dao = channelDao ?: return
        scope.launch {
            val count = WorldChannelsDownloader.downloadAndImportCountry(country, dao)
            withContext(Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    fun syncTopWorldCountries(
        onProgress: (countryName: String, count: Int) -> Unit = { _, _ -> },
        onComplete: (totalImported: Int) -> Unit = {}
    ) {
        val dao = channelDao ?: return
        scope.launch {
            var total = 0
            val topCountries = WorldChannelsDownloader.supportedCountries
            for (c in topCountries) {
                try {
                    val count = WorldChannelsDownloader.downloadAndImportCountry(c, dao)
                    total += count
                    withContext(Dispatchers.Main) {
                        onProgress(c.nameAr, count)
                    }
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                onComplete(total)
            }
        }
    }

    private var scanJob: kotlinx.coroutines.Job? = null

    fun startUserHealthScan(
        onProgressUpdate: (current: Int, total: Int, deleted: Int) -> Unit = { _, _, _ -> }
    ) {
        if (_isScanning.value) return
        scanJob?.cancel()
        scanJob = scope.launch {
            try {
                performHealthScan(onProgressUpdate)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun stopHealthScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        _lastScanReport.value = "تم إيقاف الفحص بطلب منك."
    }

    fun toggleFavorite(channelId: String) {
        val dao = channelDao ?: return
        scope.launch {
            val currentFavs = _favoriteIds.value.toMutableSet()
            val isNowFav = !currentFavs.contains(channelId)
            dao.updateFavorite(channelId, isNowFav)
        }
    }

    fun addCustomStream(title: String, url: String, category: String = "بث مخصص") {
        val dao = channelDao ?: return
        scope.launch {
            val newChannel = LiveStreamChannel(
                id = "custom_${System.currentTimeMillis()}",
                title = title,
                titleAr = title,
                url = url,
                category = category,
                iconName = "live_tv",
                description = "قناة بث مخصصة أضافها المستخدم",
                isCustom = true
            )
            dao.insert(LiveStreamEntity.fromChannel(newChannel))
        }
    }

    fun deleteChannel(id: String) {
        val dao = channelDao ?: return
        scope.launch {
            addDeletedIds(listOf(id))
            dao.deleteById(id)
        }
    }

    fun removeCustomStream(id: String) {
        deleteChannel(id)
    }

    fun importM3uPlaylist(m3uContent: String): Int {
        val dao = channelDao ?: return 0
        val lines = m3uContent.lines()
        var addedCount = 0
        var currentTitle = ""
        var currentCategory = "بث M3U المستورد"
        val newEntities = mutableListOf<LiveStreamEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                val groupMatch = Regex("group-title=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(trimmed)
                if (groupMatch != null) {
                    currentCategory = groupMatch.groupValues[1]
                }
                currentTitle = trimmed.substringAfterLast(",").trim()
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("rtsp://")) {
                if (currentTitle.isEmpty()) {
                    currentTitle = "قناة ${addedCount + 1}"
                }
                val entity = LiveStreamEntity(
                    id = "m3u_${System.currentTimeMillis()}_$addedCount",
                    title = currentTitle,
                    titleAr = currentTitle,
                    url = trimmed,
                    category = currentCategory,
                    iconName = "tv",
                    description = "قناة مستوردة من قائمة M3U",
                    isCustom = true,
                    isHealthy = true,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
                newEntities.add(entity)
                addedCount++
                currentTitle = ""
            }
        }

        if (newEntities.isNotEmpty()) {
            scope.launch {
                dao.insertAll(newEntities)
            }
        }
        return addedCount
    }

    /**
     * تنفيذ فحص دوري شامل لصلاحية كافة روابط البث (HLS/DASH/MP4/Audio/YouTube).
     * يتم حذف أي رابط تالف أو غير مستجيب مباشرة من قاعدة البيانات.
     */
    suspend fun performHealthScan(
        onProgressUpdate: (current: Int, total: Int, deleted: Int) -> Unit = { _, _, _ -> }
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val dao = channelDao ?: return@withContext Pair(0, 0)
        if (_isScanning.value) {
            return@withContext Pair(_scannedChannelsCount.value, _deletedBrokenChannelsCount.value)
        }

        _isScanning.value = true
        _scanProgress.value = 0f
        _scannedChannelsCount.value = 0
        _deletedBrokenChannelsCount.value = 0

        val channelsSnapshot = dao.getAllChannelsSync()
        val total = channelsSnapshot.size
        _totalToScan.value = total

        if (total == 0) {
            _isScanning.value = false
            return@withContext Pair(0, 0)
        }

        val deadIds = mutableListOf<String>()
        val semaphore = Semaphore(8) // Concurrent check of up to 8 streams
        var scanned = 0

        for (channel in channelsSnapshot) {
            if (!isActive || !_isScanning.value) break
            semaphore.withPermit {
                if (!isActive || !_isScanning.value) return@withPermit
                val isAlive = checkStreamUrlHealthy(channel.url)
                scanned++
                _scannedChannelsCount.value = scanned
                _scanProgress.value = scanned.toFloat() / total.toFloat()

                if (!isAlive) {
                    val failures = channel.failureCount + 1
                    // If permanently dead or failed check, mark for removal
                    if (failures >= 1) {
                        deadIds.add(channel.id)
                        addDeletedIds(listOf(channel.id))
                        dao.deleteById(channel.id)
                        _deletedBrokenChannelsCount.value = deadIds.size
                    } else {
                        dao.updateHealth(channel.id, isHealthy = false, failureCount = failures, timestamp = System.currentTimeMillis())
                    }
                } else {
                    dao.updateHealth(channel.id, isHealthy = true, failureCount = 0, timestamp = System.currentTimeMillis())
                }

                onProgressUpdate(scanned, total, deadIds.size)
            }
        }

        val deletedCount = deadIds.size
        _lastScanTime.value = System.currentTimeMillis()
        _lastScanReport.value = if (deletedCount > 0) {
            "اكتمل الفحص: تم فحص $total قناة وحذف $deletedCount رابط تالف نهائياً لتنظيف القائمة."
        } else {
            "اكتمل الفحص بنجاح: جميع قنوات البث الحالية ($total) صالحة وتعمل 100%."
        }
        _isScanning.value = false

        return@withContext Pair(scanned, deletedCount)
    }

    /**
     * فحص حقيقي لصلاحية واستجابة رابط البث (يرفض صفحات HTML أو الروابط التالفة 400/403/404)
     */
    private suspend fun checkStreamUrlHealthy(urlStr: String): Boolean = withContext(Dispatchers.IO) {
        if (urlStr.isBlank()) return@withContext false

        // Reject raw web pages like YouTube HTML watch links that ExoPlayer cannot directly stream
        if (urlStr.contains("youtube.com", ignoreCase = true) || urlStr.contains("youtu.be", ignoreCase = true)) {
            return@withContext false
        }

        if (urlStr.startsWith("rtsp://", ignoreCase = true)) {
            return@withContext true
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                instanceFollowRedirects = true
                setRequestProperty("Range", "bytes=0-1024")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "*/*")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext false
            }

            val contentType = (conn.contentType ?: "").lowercase()
            val preview = try {
                val buf = ByteArray(1024)
                val bytesRead = conn.inputStream.read(buf)
                if (bytesRead > 0) String(buf, 0, bytesRead, Charsets.UTF_8) else ""
            } catch (_: Exception) {
                ""
            }

            val looksLikeHtml = preview.contains("<!doctype html", ignoreCase = true) ||
                preview.contains("<html", ignoreCase = true)
            val isHlsOrPlaylist = preview.contains("#EXTM3U", ignoreCase = true) ||
                contentType.contains("mpegurl")
            val isMediaType = contentType.contains("audio/") ||
                contentType.contains("video/") ||
                contentType.contains("application/dash+xml") ||
                contentType.contains("application/octet-stream")

            if (looksLikeHtml && !isHlsOrPlaylist) {
                return@withContext false
            }
            return@withContext isHlsOrPlaylist || isMediaType || urlStr.contains(".m3u8", ignoreCase = true)
        } catch (_: Exception) {
            return@withContext false
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }
    }
}
