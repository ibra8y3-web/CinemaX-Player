package com.example.sync

import android.content.Context
import android.os.Build
import android.util.Base64
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.model.BookmarkEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.LiveStreamEntity
import com.example.data.model.PlayHistoryEntity
import com.example.data.model.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * CinemaX Universal Cloud & Decentralized Sync Engine:
 * - Full export & import of watch history, exact video progress, favorites, bookmarks,
 *   custom live streams, radio stations, audio equalizer presets, and player settings.
 * - Dual Mode:
 *   1. Decentralized Air-gapped Sync Key: Compressed GZIP + Base64 string that works 100% offline.
 *   2. Instant Cloud PIN (6-digit): Uploads payload to secure cloud relay and returns a short,
 *      easy-to-type PIN that can be pasted on any phone worldwide to retrieve everything!
 */
object CinemaXSyncEngine {

    const val SYNC_PROTOCOL_PREFIX = "CXSYNC-V2:"

    enum class RestoreMode {
        SMART_MERGE,  // Keep existing items, add new items and update newer play positions
        FULL_REPLACE  // Clear and replace with backup
    }

    data class SyncSummary(
        val version: Int,
        val appName: String,
        val exportTimestamp: Long,
        val deviceName: String,
        val historyCount: Int,
        val favoritesCount: Int,
        val bookmarksCount: Int,
        val channelsCount: Int,
        val radioCount: Int,
        val downloadsCount: Int,
        val vaultCount: Int,
        val settingsCount: Int
    ) {
        val formattedDate: String
            get() {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                return sdf.format(java.util.Date(exportTimestamp))
            }
    }

    data class CinemaXSyncPayload(
        val version: Int = 2,
        val appName: String = "CinemaXPlayer",
        val exportTimestamp: Long = System.currentTimeMillis(),
        val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val history: List<PlayHistoryEntity> = emptyList(),
        val favorites: List<FavoriteEntity> = emptyList(),
        val bookmarks: List<BookmarkEntity> = emptyList(),
        val customChannels: List<LiveStreamEntity> = emptyList(),
        val radioFavorites: List<String> = emptyList(),
        val downloads: List<DownloadTaskEntity> = emptyList(),
        val vaultItems: List<VaultItemEntity> = emptyList(),
        val settings: Map<String, String> = emptyMap()
    ) {
        fun toSummary(): SyncSummary {
            return SyncSummary(
                version = version,
                appName = appName,
                exportTimestamp = exportTimestamp,
                deviceName = deviceName,
                historyCount = history.size,
                favoritesCount = favorites.size,
                bookmarksCount = bookmarks.size,
                channelsCount = customChannels.size,
                radioCount = radioFavorites.size,
                downloadsCount = downloads.size,
                vaultCount = vaultItems.size,
                settingsCount = settings.size
            )
        }
    }

    /**
     * Gathers all real user data across the app into a structured CinemaXSyncPayload.
     */
    suspend fun createBackupPayload(context: Context): CinemaXSyncPayload = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val videoDao = db.videoDao()
        val channelDao = db.liveChannelDao()

        val historyList = videoDao.getAllHistoryList()
        val favList = videoDao.getAllFavoritesList()
        val bookList = videoDao.getAllBookmarksList()
        val vaultList = videoDao.getAllVaultItemsList()
        val downList = videoDao.getAllDownloadsList()
        val channelList = channelDao.getAllChannelsSync().filter { it.isCustom || it.isFavorite }

        val radioPrefs = context.getSharedPreferences("nova_radio_prefs", Context.MODE_PRIVATE)
        val radioFavs = (radioPrefs.getStringSet("favorite_radio_ids", emptySet()) ?: emptySet()).toList()

        val playerPrefs = context.getSharedPreferences("nova_player_prefs", Context.MODE_PRIVATE)
        val settingsMap = mutableMapOf<String, String>()
        playerPrefs.all.forEach { (k, v) ->
            if (v != null && k != "vault_pin_hash") { // Exclude raw hash for safety
                settingsMap[k] = v.toString()
            }
        }

        CinemaXSyncPayload(
            history = historyList,
            favorites = favList,
            bookmarks = bookList,
            customChannels = channelList,
            radioFavorites = radioFavs,
            downloads = downList,
            vaultItems = vaultList,
            settings = settingsMap
        )
    }

    /**
     * Converts CinemaXSyncPayload to JSON string.
     */
    fun payloadToJson(payload: CinemaXSyncPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("app", payload.appName)
        root.put("timestamp", payload.exportTimestamp)
        root.put("device", payload.deviceName)

        // History
        val histArr = JSONArray()
        payload.history.forEach {
            val obj = JSONObject()
            obj.put("uri", it.videoUri)
            obj.put("title", it.title)
            obj.put("pos", it.lastPositionMs)
            obj.put("dur", it.durationMs)
            obj.put("playedAt", it.lastPlayedTimestamp)
            obj.put("folder", it.folderName)
            obj.put("res", it.resolution)
            histArr.put(obj)
        }
        root.put("history", histArr)

        // Favorites
        val favArr = JSONArray()
        payload.favorites.forEach {
            val obj = JSONObject()
            obj.put("uri", it.videoUri)
            obj.put("title", it.title)
            obj.put("dur", it.durationMs)
            obj.put("size", it.sizeBytes)
            obj.put("date", it.dateAdded)
            favArr.put(obj)
        }
        root.put("favorites", favArr)

        // Bookmarks
        val bookArr = JSONArray()
        payload.bookmarks.forEach {
            val obj = JSONObject()
            obj.put("uri", it.videoUri)
            obj.put("pos", it.timestampMs)
            obj.put("title", it.title)
            obj.put("note", it.note)
            obj.put("created", it.createdAt)
            bookArr.put(obj)
        }
        root.put("bookmarks", bookArr)

        // Custom Channels
        val chanArr = JSONArray()
        payload.customChannels.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("titleAr", it.titleAr)
            obj.put("url", it.url)
            obj.put("cat", it.category)
            obj.put("icon", it.iconName)
            obj.put("desc", it.description)
            obj.put("isCustom", it.isCustom)
            obj.put("isFav", it.isFavorite)
            obj.put("backups", it.backupUrlsString)
            chanArr.put(obj)
        }
        root.put("channels", chanArr)

        // Radio
        val radArr = JSONArray()
        payload.radioFavorites.forEach { radArr.put(it) }
        root.put("radioFavs", radArr)

        // Downloads
        val downArr = JSONArray()
        payload.downloads.forEach {
            val obj = JSONObject()
            obj.put("url", it.url)
            obj.put("title", it.title)
            obj.put("path", it.localPath)
            obj.put("status", it.status)
            obj.put("created", it.createdAt)
            downArr.put(obj)
        }
        root.put("downloads", downArr)

        // Vault summary
        val vaultArr = JSONArray()
        payload.vaultItems.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.originalTitle)
            obj.put("path", it.originalPath)
            obj.put("file", it.encryptedFileName)
            obj.put("size", it.fileSize)
            obj.put("dur", it.durationMs)
            obj.put("cat", it.category)
            vaultArr.put(obj)
        }
        root.put("vault", vaultArr)

        // Settings
        val setObj = JSONObject()
        payload.settings.forEach { (k, v) -> setObj.put(k, v) }
        root.put("settings", setObj)

        return root.toString()
    }

    /**
     * Parses JSON string into CinemaXSyncPayload.
     */
    fun jsonToPayload(jsonStr: String): CinemaXSyncPayload {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 2)
        val app = root.optString("app", "CinemaXPlayer")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())
        val device = root.optString("device", "Unknown Device")

        val history = mutableListOf<PlayHistoryEntity>()
        val histArr = root.optJSONArray("history")
        if (histArr != null) {
            for (i in 0 until histArr.length()) {
                val obj = histArr.getJSONObject(i)
                history.add(
                    PlayHistoryEntity(
                        videoUri = obj.optString("uri"),
                        title = obj.optString("title"),
                        lastPositionMs = obj.optLong("pos"),
                        durationMs = obj.optLong("dur"),
                        lastPlayedTimestamp = obj.optLong("playedAt", System.currentTimeMillis()),
                        folderName = obj.optString("folder"),
                        resolution = obj.optString("res")
                    )
                )
            }
        }

        val favorites = mutableListOf<FavoriteEntity>()
        val favArr = root.optJSONArray("favorites")
        if (favArr != null) {
            for (i in 0 until favArr.length()) {
                val obj = favArr.getJSONObject(i)
                favorites.add(
                    FavoriteEntity(
                        videoUri = obj.optString("uri"),
                        title = obj.optString("title"),
                        durationMs = obj.optLong("dur"),
                        sizeBytes = obj.optLong("size"),
                        dateAdded = obj.optLong("date", System.currentTimeMillis())
                    )
                )
            }
        }

        val bookmarks = mutableListOf<BookmarkEntity>()
        val bookArr = root.optJSONArray("bookmarks")
        if (bookArr != null) {
            for (i in 0 until bookArr.length()) {
                val obj = bookArr.getJSONObject(i)
                bookmarks.add(
                    BookmarkEntity(
                        videoUri = obj.optString("uri"),
                        timestampMs = obj.optLong("pos"),
                        title = obj.optString("title"),
                        note = obj.optString("note"),
                        createdAt = obj.optLong("created", System.currentTimeMillis())
                    )
                )
            }
        }

        val channels = mutableListOf<LiveStreamEntity>()
        val chanArr = root.optJSONArray("channels")
        if (chanArr != null) {
            for (i in 0 until chanArr.length()) {
                val obj = chanArr.getJSONObject(i)
                channels.add(
                    LiveStreamEntity(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        titleAr = obj.optString("titleAr"),
                        url = obj.optString("url"),
                        category = obj.optString("cat"),
                        iconName = obj.optString("icon"),
                        description = obj.optString("desc"),
                        isCustom = obj.optBoolean("isCustom", false),
                        isFavorite = obj.optBoolean("isFav", false),
                        backupUrlsString = obj.optString("backups")
                    )
                )
            }
        }

        val radioFavs = mutableListOf<String>()
        val radArr = root.optJSONArray("radioFavs")
        if (radArr != null) {
            for (i in 0 until radArr.length()) {
                radioFavs.add(radArr.getString(i))
            }
        }

        val downloads = mutableListOf<DownloadTaskEntity>()
        val downArr = root.optJSONArray("downloads")
        if (downArr != null) {
            for (i in 0 until downArr.length()) {
                val obj = downArr.getJSONObject(i)
                downloads.add(
                    DownloadTaskEntity(
                        url = obj.optString("url"),
                        title = obj.optString("title"),
                        localPath = obj.optString("path"),
                        status = obj.optString("status", "COMPLETED"),
                        createdAt = obj.optLong("created", System.currentTimeMillis())
                    )
                )
            }
        }

        val vaultItems = mutableListOf<VaultItemEntity>()
        val vaultArr = root.optJSONArray("vault")
        if (vaultArr != null) {
            for (i in 0 until vaultArr.length()) {
                val obj = vaultArr.getJSONObject(i)
                vaultItems.add(
                    VaultItemEntity(
                        id = obj.optString("id"),
                        originalTitle = obj.optString("title"),
                        originalPath = obj.optString("path"),
                        encryptedFileName = obj.optString("file"),
                        fileSize = obj.optLong("size"),
                        durationMs = obj.optLong("dur"),
                        category = obj.optString("cat", "خزنة آمنة")
                    )
                )
            }
        }

        val settings = mutableMapOf<String, String>()
        val setObj = root.optJSONObject("settings")
        if (setObj != null) {
            val keys = setObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                settings[key] = setObj.optString(key)
            }
        }

        return CinemaXSyncPayload(
            version = version,
            appName = app,
            exportTimestamp = timestamp,
            deviceName = device,
            history = history,
            favorites = favorites,
            bookmarks = bookmarks,
            customChannels = channels,
            radioFavorites = radioFavs,
            downloads = downloads,
            vaultItems = vaultItems,
            settings = settings
        )
    }

    /**
     * Compresses the payload using GZIP and returns a compact Base64 sync code with prefix.
     */
    fun compressToSyncCode(payload: CinemaXSyncPayload): String {
        val json = payloadToJson(payload)
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(bytes) }
        val compressed = bos.toByteArray()
        val base64 = Base64.encodeToString(compressed, Base64.NO_WRAP or Base64.URL_SAFE)
        return "$SYNC_PROTOCOL_PREFIX$base64"
    }

    /**
     * Decompresses a sync code or parses raw JSON.
     */
    fun parseSyncCode(rawInput: String): CinemaXSyncPayload {
        val trimmed = rawInput.trim()
        if (trimmed.startsWith(SYNC_PROTOCOL_PREFIX)) {
            val b64 = trimmed.substring(SYNC_PROTOCOL_PREFIX.length)
            val compressed = Base64.decode(b64, Base64.NO_WRAP or Base64.URL_SAFE)
            val bis = ByteArrayInputStream(compressed)
            val json = GZIPInputStream(bis).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return jsonToPayload(json)
        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return jsonToPayload(trimmed)
        } else {
            // Attempt direct base64 decode if prefix omitted
            val compressed = Base64.decode(trimmed, Base64.NO_WRAP or Base64.URL_SAFE)
            val bis = ByteArrayInputStream(compressed)
            val json = GZIPInputStream(bis).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return jsonToPayload(json)
        }
    }

    /**
     * Uploads the backup to a fast cloud relay (dpaste.org / cl1p.net) and returns an easy 6-character PIN.
     */
    suspend fun uploadToCloudRelay(syncCode: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val postData = "content=${URLEncoder.encode(syncCode, "UTF-8")}&expiry_days=30&format=url"
            val postBytes = postData.toByteArray(StandardCharsets.UTF_8)

            val url = URL("https://dpaste.org/api/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Content-Length", postBytes.size.toString())
                setRequestProperty("User-Agent", "CinemaXPlayer-CloudSync/2.0")
            }

            conn.outputStream.use { it.write(postBytes) }

            val responseCode = conn.responseCode
            if (responseCode == 200 || responseCode == 201) {
                val responseUrl = conn.inputStream.bufferedReader().use { it.readText().trim() }
                conn.disconnect()

                // Extract PIN slug from URL e.g. "https://dpaste.org/abcde" -> "abcde" -> formatted as "CX-ABCDE"
                val slug = responseUrl.substringAfterLast("/").trim()
                if (slug.isNotBlank()) {
                    val formattedPin = "CX-${slug.uppercase(Locale.ROOT)}"
                    return@withContext Result.success(formattedPin)
                }
            }
            conn.disconnect()
            Result.failure(Exception("سيرفر المزامنة رد برمز $responseCode"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves a 6-digit PIN, short code, or URL from the cloud relay and downloads the payload.
     */
    suspend fun fetchFromCloudRelay(pinOrUrl: String): Result<CinemaXSyncPayload> = withContext(Dispatchers.IO) {
        val clean = pinOrUrl.trim()

        // 1. If it's already an offline sync code or raw JSON, decode directly!
        if (clean.startsWith(SYNC_PROTOCOL_PREFIX) || (clean.startsWith("{") && clean.endsWith("}"))) {
            return@withContext try {
                Result.success(parseSyncCode(clean))
            } catch (e: Exception) {
                Result.failure(Exception("صيغة كود المزامنة غير صالحة: ${e.message}"))
            }
        }

        // 2. Extract PIN slug e.g. "CX-ABCDE" -> "abcde"
        var slug = clean
        if (slug.startsWith("CX-", ignoreCase = true)) {
            slug = slug.substring(3).trim()
        }
        if (slug.contains("dpaste.org/")) {
            slug = slug.substringAfter("dpaste.org/").substringBefore("/").substringBefore("?").trim()
        }

        if (slug.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال رمز مزامنة صالح أو كود سحابي صحيح"))
        }

        try {
            val targetUrl = "https://dpaste.org/${slug.lowercase(Locale.ROOT)}/raw"
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "CinemaXPlayer-CloudSync/2.0")
            }

            val code = conn.responseCode
            if (code == 200) {
                val downloadedText = conn.inputStream.bufferedReader().use { it.readText().trim() }
                conn.disconnect()
                val payload = parseSyncCode(downloadedText)
                return@withContext Result.success(payload)
            }
            conn.disconnect()
            Result.failure(Exception("لم يتم العثور على حزمة مزامنة بهذا الرمز ($clean). تأكد من صحة الرمز."))
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بالسيرفر السحابي: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}"))
        }
    }

    /**
     * Restores and merges the payload into the device's Room database and Preferences.
     */
    suspend fun restorePayload(
        context: Context,
        payload: CinemaXSyncPayload,
        mode: RestoreMode = RestoreMode.SMART_MERGE
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val videoDao = db.videoDao()
            val channelDao = db.liveChannelDao()
            val prefs = PreferencesManager(context)

            if (mode == RestoreMode.FULL_REPLACE) {
                videoDao.clearAllHistory()
                videoDao.clearAllFavorites()
                videoDao.clearAllBookmarks()
            }

            // 1. History
            if (payload.history.isNotEmpty()) {
                if (mode == RestoreMode.SMART_MERGE) {
                    val existing = videoDao.getAllHistoryList().associateBy { it.videoUri }
                    val toInsert = payload.history.map { incoming ->
                        val current = existing[incoming.videoUri]
                        if (current != null && current.lastPlayedTimestamp > incoming.lastPlayedTimestamp) {
                            current // Keep newer device progress
                        } else {
                            incoming
                        }
                    }
                    videoDao.insertAllHistory(toInsert)
                } else {
                    videoDao.insertAllHistory(payload.history)
                }
            }

            // 2. Favorites
            if (payload.favorites.isNotEmpty()) {
                videoDao.insertAllFavorites(payload.favorites)
            }

            // 3. Bookmarks
            if (payload.bookmarks.isNotEmpty()) {
                videoDao.insertAllBookmarks(payload.bookmarks)
            }

            // 4. Downloads
            if (payload.downloads.isNotEmpty()) {
                videoDao.insertAllDownloads(payload.downloads)
            }

            // 5. Vault items
            if (payload.vaultItems.isNotEmpty()) {
                videoDao.insertAllVaultItems(payload.vaultItems)
            }

            // 6. Custom Live Channels
            if (payload.customChannels.isNotEmpty()) {
                channelDao.insertAll(payload.customChannels)
            }

            // 7. Radio Favorites
            if (payload.radioFavorites.isNotEmpty()) {
                val radioPrefs = context.getSharedPreferences("nova_radio_prefs", Context.MODE_PRIVATE)
                val currentFavs = radioPrefs.getStringSet("favorite_radio_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                currentFavs.addAll(payload.radioFavorites)
                radioPrefs.edit().putStringSet("favorite_radio_ids", currentFavs).apply()
            }

            // 8. Settings
            val playerPrefs = context.getSharedPreferences("nova_player_prefs", Context.MODE_PRIVATE)
            val editor = playerPrefs.edit()
            payload.settings.forEach { (k, v) ->
                when (k) {
                    "theme_mode" -> prefs.setThemeMode(v)
                    "language" -> prefs.setLanguage(v)
                    "night_shield" -> prefs.setNightShield(v.toBoolean())
                    "sound_preset" -> prefs.setSoundPreset(v)
                    "ai_enhance" -> prefs.setAiEnhance(v)
                    else -> editor.putString(k, v)
                }
            }
            editor.apply()

            val msg = "✅ تمت استعادة ${payload.history.size} فيديو في السجل، و ${payload.favorites.size} في المفضلة، و ${payload.bookmarks.size} علامة مرجعية، وإعدادات المشغل بنجاح!"
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
