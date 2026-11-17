package com.example.prayer

import com.example.data.model.CalculationMethod
import com.example.data.model.PrayerTimeEntry
import com.example.data.model.PrayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object AladhanApiClient {

    /**
     * Fetches exact prayer times from Aladhan API for given coordinates or city.
     * Returns null if network fails or timeout occurs, so callers can seamlessly fallback
     * to PrayerTimesCalculator offline astronomical calculation!
     */
    suspend fun fetchPrayerTimes(
        lat: Double,
        lon: Double,
        method: CalculationMethod = CalculationMethod.EGYPT
    ): List<PrayerTimeEntry>? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis() / 1000
            val methodId = when (method) {
                CalculationMethod.EGYPT -> 5
                CalculationMethod.MAKKAH -> 4
                CalculationMethod.MWL -> 3
                CalculationMethod.ISNA -> 2
                CalculationMethod.KARACHI -> 1
            }
            val urlString = "https://api.aladhan.com/v1/timings/$timestamp?latitude=$lat&longitude=$lon&method=$methodId"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "CinemaX/1.0 (Android)")
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.optString("status") == "OK") {
                    val timings = root.getJSONObject("data").getJSONObject("timings")
                    return@withContext parseTimings(timings)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimings(timings: JSONObject): List<PrayerTimeEntry> {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val nowMinutes = currentHour * 60 + currentMinute

        val rawTimes = listOf(
            PrayerType.FAJR to timings.optString("Fajr", "04:30"),
            PrayerType.SUNRISE to timings.optString("Sunrise", "06:00"),
            PrayerType.DHUHR to timings.optString("Dhuhr", "12:15"),
            PrayerType.ASR to timings.optString("Asr", "15:30"),
            PrayerType.MAGHRIB to timings.optString("Maghrib", "18:00"),
            PrayerType.ISHA to timings.optString("Isha", "19:30")
        )

        var foundNext = false
        val entries = mutableListOf<PrayerTimeEntry>()

        rawTimes.forEach { (type, timeStr) ->
            val cleanTime = timeStr.split(" ").firstOrNull() ?: timeStr
            val parts = cleanTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val timeMinutes = h * 60 + m
            val isPassed = timeMinutes < nowMinutes
            val isNext = !isPassed && !foundNext

            if (isNext) {
                foundNext = true
            }

            val amPm = if (h < 12) "ص" else "م"
            val displayHour = if (h % 12 == 0) 12 else h % 12
            val formatted = String.format("%02d:%02d %s", displayHour, m, amPm)

            entries.add(
                PrayerTimeEntry(
                    type = type,
                    timeFormatted = formatted,
                    hour24 = h,
                    minute = m,
                    isNext = isNext,
                    isPassed = isPassed,
                    adhanEnabled = true
                )
            )
        }

        if (!foundNext && entries.isNotEmpty()) {
            val first = entries.first()
            entries[0] = first.copy(isNext = true)
        }

        return entries
    }
}
