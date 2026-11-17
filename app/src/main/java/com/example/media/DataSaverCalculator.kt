package com.example.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * حاسبة استهلاك البيانات وفاحص سرعة واستقرار البث المباشر (Smart Data Saver & Ping Tester)
 * تقدم للمستخدم حساباً دقيقاً لحجم البيانات المستهلكة حسب الدقة والوقت، وتختبر سرعة الاستجابة (Latency) لمخدمات البث.
 */
object DataSaverCalculator {

    data class QualityProfile(
        val nameAr: String,
        val resolution: String,
        val bitrateKbps: Int,
        val description: String
    ) {
        fun calculateUsageMB(minutes: Int): Float {
            // (bitrateKbps * 1000 bits / 8) * (minutes * 60) / (1024 * 1024)
            val bytes = (bitrateKbps.toLong() * 1000L / 8L) * (minutes.toLong() * 60L)
            return bytes / (1024f * 1024f)
        }
    }

    val profiles = listOf(
        QualityProfile("فائق الجودة 4K UHD", "3840×2160", 18000, "يستهلك سرعة إنترنت عالية جداً (~2.2 جيجابايت/ساعة)"),
        QualityProfile("عالي الدقة 1080p FHD", "1920×1080", 4500, "صورة سينمائية نقية (~580 ميجابايت/ساعة)"),
        QualityProfile("دقة قياسية 720p HD", "1280×720", 2200, "التوازن المثالي للبث المباشر والموبايل (~280 ميجابايت/ساعة)"),
        QualityProfile("موفر البيانات 480p SD", "854×480", 900, "مناسب لشبكات 3G/4G الضعيفة (~115 ميجابايت/ساعة)"),
        QualityProfile("أقصى توفير 360p", "640×360", 450, "مناسب للباقات المحدودة جداً (~58 ميجابايت/ساعة)"),
        QualityProfile("بث صوتي وراديو (Audio)", "نقي AAC", 128, "توفير فائق للأذكار والإذاعات (~16 ميجابايت/ساعة)")
    )

    data class PingResult(
        val host: String,
        val label: String,
        val pingMs: Long,
        val isSuccess: Boolean
    )

    suspend fun testServerPing(urlStr: String, label: String): PingResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var success = false
        var host = ""
        try {
            val url = URL(urlStr)
            host = url.host
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "HEAD"
                instanceFollowRedirects = true
            }
            connection.connect()
            val code = connection.responseCode
            success = code in 200..399
            connection.disconnect()
        } catch (_: Exception) {
            success = false
        }
        val duration = System.currentTimeMillis() - start
        PingResult(host, label, duration, success)
    }
}
