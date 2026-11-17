package com.example.prayer

import android.content.Context
import com.example.data.model.AdhanAlarmType
import com.example.data.model.AdhanLogEntry
import com.example.data.model.DhikrPhrase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Persistent thread-safe event store for Adhan, Pre-Adhan, Post-Adhan, Scheduled, Stopped, and Test events,
 * plus pure scheduling & Tasbih helpers shared across receivers, services, UI, and tests.
 */
object AdhanLogStore {

    private const val PREFS_NAME = "nova_prayer_prefs"
    private const val KEY_LOGS = "adhan_event_logs"
    private const val MAX_LOGS = 120
    private val lock = Any()

    val postAdhanDuaAr: String =
        "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ."

    val defaultDhikrCatalog: List<DhikrPhrase> = listOf(
        DhikrPhrase(
            id = "subhan_allah",
            textAr = "سُبْحَانَ اللَّهِ",
            virtueAr = "تُكتب له ألف حسنة أو يُحط عنه ألف خطيئة (33 مرة بعد الصلاة)",
            defaultTarget = 33
        ),
        DhikrPhrase(
            id = "alhamdulillah",
            textAr = "الْحَمْدُ لِلَّهِ",
            virtueAr = "تملأ الميزان من أذكار ما بعد الصلاة (33 مرة)",
            defaultTarget = 33
        ),
        DhikrPhrase(
            id = "allahu_akbar",
            textAr = "اللَّهُ أَكْبَرُ",
            virtueAr = "تمام تسبيح دبر كل صلاة مكتوبة (33 مرة)",
            defaultTarget = 33
        ),
        DhikrPhrase(
            id = "istighfar",
            textAr = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
            virtueAr = "يُفتح به باب الرزق والفرج ومغفرة الذنوب",
            defaultTarget = 100
        ),
        DhikrPhrase(
            id = "salawat",
            textAr = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            virtueAr = "من صلى عليَّ واحدة صلى الله عليه عشراً",
            defaultTarget = 100
        ),
        DhikrPhrase(
            id = "tasbih_kamel",
            textAr = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            virtueAr = "كلمتان خفيفتان على اللسان ثقيلتان في الميزان",
            defaultTarget = 100
        ),
        DhikrPhrase(
            id = "hawqalah",
            textAr = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            virtueAr = "كنز من كنوز الجنة وفرج لكل هم",
            defaultTarget = 100
        )
    )

    fun appendLog(
        context: Context,
        prayerName: String,
        cityName: String,
        voiceName: String,
        eventType: AdhanAlarmType,
        note: String = ""
    ): List<AdhanLogEntry> {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nowMs = System.currentTimeMillis()
            val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(nowMs))
            val existing = decodeLogs(prefs.getString(KEY_LOGS, "[]") ?: "[]")

            // Deduplicate rapid repeated SCHEDULED logs within 20 seconds to keep log clean
            val filtered = if (eventType == AdhanAlarmType.SCHEDULED &&
                existing.firstOrNull()?.eventType == AdhanAlarmType.SCHEDULED &&
                (nowMs - existing.first().timestampMs) < 20_000L
            ) {
                existing.drop(1)
            } else {
                existing
            }

            val entry = AdhanLogEntry(
                id = UUID.randomUUID().toString(),
                timestampMs = nowMs,
                formattedTime = formatted,
                prayerName = prayerName,
                cityName = cityName,
                voiceName = voiceName,
                eventType = eventType,
                note = note
            )
            val updated = (listOf(entry) + filtered).take(MAX_LOGS)
            prefs.edit().putString(KEY_LOGS, encodeLogs(updated)).apply()
            return updated
        }
    }

    fun getLogs(context: Context): List<AdhanLogEntry> {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return decodeLogs(prefs.getString(KEY_LOGS, "[]") ?: "[]")
        }
    }

    fun clearLogs(context: Context): List<AdhanLogEntry> {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LOGS, "[]").apply()
            return emptyList()
        }
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun encodeLogs(logs: List<AdhanLogEntry>): String {
        return logs.take(MAX_LOGS).joinToString(prefix = "[", postfix = "]", separator = ",") { e ->
            buildString {
                append('{')
                append("\"id\":\"").append(escapeJson(e.id)).append("\",")
                append("\"ts\":").append(e.timestampMs).append(',')
                append("\"time\":\"").append(escapeJson(e.formattedTime)).append("\",")
                append("\"prayer\":\"").append(escapeJson(e.prayerName)).append("\",")
                append("\"city\":\"").append(escapeJson(e.cityName)).append("\",")
                append("\"voice\":\"").append(escapeJson(e.voiceName)).append("\",")
                append("\"type\":\"").append(escapeJson(e.eventType.code)).append("\",")
                append("\"note\":\"").append(escapeJson(e.note)).append('"')
                append('}')
            }
        }
    }

    fun decodeLogs(jsonStr: String): List<AdhanLogEntry> {
        if (jsonStr.isBlank()) return emptyList()
        val result = mutableListOf<AdhanLogEntry>()
        var i = 0
        val n = jsonStr.length

        fun skipWs() {
            while (i < n && (jsonStr[i].isWhitespace() || jsonStr[i] == ',')) i++
        }

        fun readString(): String {
            if (i >= n || jsonStr[i] != '"') return ""
            i++ // skip opening quote
            val sb = StringBuilder()
            while (i < n) {
                val c = jsonStr[i++]
                if (c == '\\' && i < n) {
                    when (val esc = jsonStr[i++]) {
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(esc)
                    }
                } else if (c == '"') {
                    break
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        fun readLong(): Long {
            val start = i
            while (i < n && (jsonStr[i].isDigit() || jsonStr[i] == '-')) i++
            return jsonStr.substring(start, i).toLongOrNull() ?: 0L
        }

        while (i < n) {
            if (jsonStr[i] == '{') {
                i++
                val fields = mutableMapOf<String, String>()
                var ts = 0L
                while (i < n && jsonStr[i] != '}') {
                    skipWs()
                    if (i < n && jsonStr[i] == '}') break
                    val key = readString()
                    while (i < n && jsonStr[i] != ':') i++
                    if (i < n && jsonStr[i] == ':') i++
                    while (i < n && jsonStr[i].isWhitespace()) i++
                    if (i < n && jsonStr[i] == '"') {
                        fields[key] = readString()
                    } else if (key == "ts") {
                        ts = readLong()
                    } else {
                        while (i < n && jsonStr[i] != ',' && jsonStr[i] != '}') i++
                    }
                    skipWs()
                }
                if (i < n && jsonStr[i] == '}') i++
                val code = fields["type"] ?: AdhanAlarmType.ADHAN.code
                val type = AdhanAlarmType.entries.find { it.code == code } ?: AdhanAlarmType.ADHAN
                result.add(
                    AdhanLogEntry(
                        id = fields["id"] ?: UUID.randomUUID().toString(),
                        timestampMs = ts,
                        formattedTime = fields["time"] ?: "",
                        prayerName = fields["prayer"] ?: "الصلاة",
                        cityName = fields["city"] ?: "",
                        voiceName = fields["voice"] ?: "",
                        eventType = type,
                        note = fields["note"] ?: ""
                    )
                )
            } else {
                i++
            }
        }
        return result
    }

    /**
     * Computes trigger epoch millis for Pre-Adhan (before) or Post-Adhan (after) reminders.
     */
    fun computeOffsetTriggerMillis(prayerMillis: Long, offsetMinutes: Int, isBefore: Boolean): Long {
        val delta = offsetMinutes.coerceAtLeast(0) * 60_000L
        return if (isBefore) prayerMillis - delta else prayerMillis + delta
    }

    /**
     * Advances Tasbih counter and reports whether a cycle target was just completed.
     * Passing target <= 0 runs in open-ended mode (no target milestone reset).
     */
    fun nextTasbihCount(current: Int, target: Int): Pair<Int, Boolean> {
        val incremented = current + 1
        return if (target > 0 && incremented >= target) {
            incremented to true
        } else {
            incremented to false
        }
    }
}
