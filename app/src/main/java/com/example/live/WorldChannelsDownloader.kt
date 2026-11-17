package com.example.live

import android.util.Log
import com.example.data.dao.LiveChannelDao
import com.example.data.model.LiveStreamEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * محرك تنزيل واستيراد آلاف القنوات التلفزيونية العالمية الحقيقية والمباشرة
 * من قاعدة البيانات العالمية IPTV-Org حسب الدول والمناطق.
 */
object WorldChannelsDownloader {

    private const val TAG = "WorldChannelsDownloader"

    data class CountryPreset(
        val code: String,
        val nameAr: String,
        val flag: String,
        val categoryName: String
    )

    val supportedCountries = listOf(
        CountryPreset("eg", "مصر", "🇪🇬", "قنوات عربية وفضائيات"),
        CountryPreset("sa", "السعودية", "🇸🇦", "قنوات عربية وفضائيات"),
        CountryPreset("ae", "الإمارات", "🇦🇪", "قنوات عربية وفضائيات"),
        CountryPreset("qa", "قطر", "🇶🇦", "قنوات عربية وفضائيات"),
        CountryPreset("kw", "الكويت", "🇰🇼", "قنوات عربية وفضائيات"),
        CountryPreset("om", "عمان", "🇴🇲", "قنوات عربية وفضائيات"),
        CountryPreset("bh", "البحرين", "🇧🇭", "قنوات عربية وفضائيات"),
        CountryPreset("ma", "المغرب", "🇲🇦", "قنوات عربية وفضائيات"),
        CountryPreset("dz", "الجزائر", "🇩🇿", "قنوات عربية وفضائيات"),
        CountryPreset("tn", "تونس", "🇹🇳", "قنوات عربية وفضائيات"),
        CountryPreset("iq", "العراق", "🇮🇶", "قنوات عربية وفضائيات"),
        CountryPreset("jo", "الأردن", "🇯🇴", "قنوات عربية وفضائيات"),
        CountryPreset("lb", "لبنان", "🇱🇧", "قنوات عربية وفضائيات"),
        CountryPreset("ps", "فلسطين", "🇵🇸", "قنوات عربية وفضائيات"),
        CountryPreset("sy", "سوريا", "🇸🇾", "قنوات عربية وفضائيات"),
        CountryPreset("ye", "اليمن", "🇾🇪", "قنوات عربية وفضائيات"),
        CountryPreset("fr", "فرنسا", "🇫🇷", "قنوات فرنسية (France TV 🇫🇷)"),
        CountryPreset("tr", "تركيا", "🇹🇷", "قنوات تركية (Turkey TV)"),
        CountryPreset("uk", "بريطانيا", "🇬🇧", "قنوات أوروبية (Europe TV)"),
        CountryPreset("us", "أمريكا", "🇺🇸", "قنوات أمريكية ولاتينية"),
        CountryPreset("de", "ألمانيا", "🇩🇪", "قنوات أوروبية (Europe TV)"),
        CountryPreset("es", "إسبانيا", "🇪🇸", "قنوات أوروبية (Europe TV)"),
        CountryPreset("it", "إيطاليا", "🇮🇹", "قنوات أوروبية (Europe TV)")
    )

    suspend fun downloadAndImportCountry(
        countryPreset: CountryPreset,
        dao: LiveChannelDao,
        onProgress: (importedCount: Int) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val urlString = "https://iptv-org.github.io/iptv/countries/${countryPreset.code}.m3u"
        val channels = mutableListOf<LiveStreamEntity>()
        var conn: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("User-Agent", "CinemaXPlayer/2.0")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val content = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = M3uPlaylistParser.parse(
                    rawContent = content,
                    playlistUrl = urlString,
                    fallbackCategory = countryPreset.categoryName
                )
                val timestamp = System.currentTimeMillis()
                parsed.forEachIndexed { index, channel ->
                    val id = "imp_${countryPreset.code}_${index}_${channel.id.removePrefix("m3u_")}"
                    channels.add(
                        LiveStreamEntity(
                            id = id,
                            title = channel.title,
                            titleAr = "${countryPreset.flag} ${channel.title}",
                            url = channel.url,
                            category = countryPreset.categoryName,
                            iconName = "tv",
                            description = "قناة مستوردة من قائمة IPTV لدولة ${countryPreset.nameAr}",
                            isCustom = true,
                            isFavorite = false,
                            backupUrlsString = "",
                            isHealthy = true,
                            failureCount = 0,
                            lastCheckedTimestamp = timestamp
                        )
                    )
                }

                if (channels.isNotEmpty()) {
                    dao.insertAllIgnore(channels)
                    onProgress(channels.size)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download M3U for ${countryPreset.nameAr}: ${e.message}")
        } finally {
            conn?.disconnect()
        }

        return@withContext channels.size
    }
}
