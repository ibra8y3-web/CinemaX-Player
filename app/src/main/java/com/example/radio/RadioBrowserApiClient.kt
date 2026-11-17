package com.example.radio

import android.util.Log
import com.example.data.model.RadioCategory
import com.example.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * عميل الوصول إلى مكتبة الراديو المفتوحة العالمية (Radio Browser API)
 * يتيح البحث وتصفح أكثر من 45,000 محطة إذاعية حقيقية تبث في جميع دول العالم
 * (فرنسا، بريطانيا، أمريكا، مصر، تركيا، ألمانيا، إسبانيا، المغرب، السعودية، إلخ).
 */
object RadioBrowserApiClient {

    private const val TAG = "RadioBrowserClient"
    
    // مرايا خوادم راديو براوزر لضمان العمل الدائم 100%
    private val SERVERS = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    private var currentServerIndex = 0

    private fun getBaseUrl(): String {
        return SERVERS[currentServerIndex % SERVERS.size]
    }

    private fun rotateServer() {
        currentServerIndex = (currentServerIndex + 1) % SERVERS.size
    }

    suspend fun searchStations(query: String, limit: Int = 150, offset: Int = 0): List<RadioStation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "${getBaseUrl()}/json/stations/search?name=$encoded&limit=$limit&offset=$offset&order=clickcount&reverse=true"
        fetchAndParse(endpoint, query)
    }

    suspend fun getStationsByCountry(country: String, limit: Int = 250, offset: Int = 0): List<RadioStation> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(country.trim(), "UTF-8")
        val endpoint = "${getBaseUrl()}/json/stations/bycountry/$encoded?limit=$limit&offset=$offset&order=clickcount&reverse=true"
        fetchAndParse(endpoint, country)
    }

    suspend fun getStationsByTag(tag: String, limit: Int = 250, offset: Int = 0): List<RadioStation> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(tag.trim(), "UTF-8")
        val endpoint = "${getBaseUrl()}/json/stations/bytag/$encoded?limit=$limit&offset=$offset&order=clickcount&reverse=true"
        fetchAndParse(endpoint, tag)
    }

    suspend fun getTopClickStations(limit: Int = 300, offset: Int = 0): List<RadioStation> = withContext(Dispatchers.IO) {
        val endpoint = "${getBaseUrl()}/json/stations/topclick/$limit?offset=$offset"
        fetchAndParse(endpoint, "Top Click")
    }

    suspend fun getTopVotedStations(limit: Int = 250, offset: Int = 0): List<RadioStation> = withContext(Dispatchers.IO) {
        val endpoint = "${getBaseUrl()}/json/stations/topvote/$limit?offset=$offset"
        fetchAndParse(endpoint, "Top Voted")
    }

    private fun fetchAndParse(endpoint: String, contextTag: String): List<RadioStation> {
        val results = mutableListOf<RadioStation>()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 7000
                setRequestProperty("User-Agent", "CinemaX/2.0")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val jsonArray = JSONArray(sb.toString())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val streamUrl = obj.optString("url_resolved").takeIf { it.isNotBlank() }
                        ?: obj.optString("url")
                    if (streamUrl.isBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
                        continue
                    }

                    val name = obj.optString("name", "إذاعة عالمية").trim()
                    val country = obj.optString("country", "دولي").trim()
                    val tags = obj.optString("tags", "").trim()
                    val favicon = obj.optString("favicon", "").takeIf { it.isNotBlank() }
                    val bitrate = obj.optInt("bitrate", 64).let { if (it <= 0) 64 else it }
                    val uuid = obj.optString("stationuuid", "rb_${System.currentTimeMillis()}_$i")

                    val category = when {
                        country.contains("France", ignoreCase = true) || tags.contains("french", ignoreCase = true) ->
                            RadioCategory.FRANCE_GLOBAL
                        country.contains("Turkey", ignoreCase = true) || tags.contains("turkish", ignoreCase = true) ->
                            RadioCategory.TURKISH_RADIO
                        country.contains("Egypt", ignoreCase = true) || country.contains("Saudi", ignoreCase = true) ||
                                country.contains("Kuwait", ignoreCase = true) || country.contains("Morocco", ignoreCase = true) ->
                            RadioCategory.ARABIC_RADIO
                        country.contains("United Kingdom", ignoreCase = true) || country.contains("United States", ignoreCase = true) ->
                            RadioCategory.UK_USA
                        tags.contains("quran", ignoreCase = true) || tags.contains("islam", ignoreCase = true) ->
                            RadioCategory.QURAN
                        tags.contains("classical", ignoreCase = true) || tags.contains("tarab", ignoreCase = true) ->
                            RadioCategory.TARAB
                        tags.contains("chill", ignoreCase = true) || tags.contains("ambient", ignoreCase = true) ->
                            RadioCategory.MUSIC_RELAX
                        else ->
                            RadioCategory.WORLD_GLOBAL
                    }

                    val desc = if (tags.isNotBlank()) {
                        "$country • $tags • $bitrate kbps"
                    } else {
                        "بث مباشر من $country بجودة $bitrate kbps"
                    }

                    results.add(
                        RadioStation(
                            id = uuid,
                            titleAr = name,
                            reciterOrDesc = desc,
                            streamUrl = streamUrl,
                            backupUrl = obj.optString("url"),
                            category = category,
                            bitrateKbps = bitrate,
                            isUltraLowData = bitrate <= 64,
                            iconName = "radio",
                            country = if (country.isNotBlank()) country else "العالم",
                            faviconUrl = favicon
                        )
                    )
                }
            } else {
                rotateServer()
            }
        } catch (e: Exception) {
            Log.w(TAG, "RadioBrowser fetch error ($contextTag): ${e.message}")
            rotateServer()
        } finally {
            conn?.disconnect()
        }
        return results
    }
}
