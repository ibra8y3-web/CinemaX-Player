package com.example.media

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object RealMediaDownloader {

    const val USER_AGENT_CHROME = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    @Volatile
    private var isPauseRequested = false

    fun normalizeUrl(urlStr: String): String {
        var trimmed = urlStr.trim()
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            trimmed = trimmed.substring(1, trimmed.length - 1).trim()
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) {
            trimmed = trimmed.substring(1, trimmed.length - 1).trim()
        }
        if (trimmed.isNotBlank() && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            if (trimmed.startsWith("www.") || trimmed.contains(".") || trimmed.contains("/")) {
                trimmed = "https://$trimmed"
            }
        }
        return trimmed
    }

    sealed class VideoVerificationResult {
        data class Valid(
            val directUrl: String,
            val mimeType: String,
            val contentLength: Long,
            val format: String,
            val isResumable: Boolean,
            val server: String?
        ) : VideoVerificationResult() {
            val formattedSize: String
                get() = if (contentLength > 0) {
                    val mb = contentLength / (1024.0 * 1024.0)
                    if (mb >= 1024) String.format(Locale.US, "%.2f جيجابايت", mb / 1024.0)
                    else String.format(Locale.US, "%.1f ميجابايت", mb)
                } else "بث مباشر أو حجم تدفقي غير محدود"
        }

        data class SocialWebPage(
            val pageUrl: String,
            val platform: String,
            val advice: String = "هذا رابط صفحة ويب/سوشيال ميديا يتطلب استخراج ملف MP4 المباشر لتجنب خطأ 400. استخدم المستكشف المدمج (Web Video Sniffer) لالتقاطه فوراً بنقرة واحدة!"
        ) : VideoVerificationResult()

        data class Invalid(
            val reason: String,
            val detectedType: String? = null
        ) : VideoVerificationResult()
    }

    data class DownloadProgress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percent: Int,
        val speedBytesPerSec: Long = 0L,
        val etaSeconds: Long = -1L,
        val isPaused: Boolean = false,
        val isCompleted: Boolean = false,
        val errorMsg: String? = null,
        val savedFilePath: String? = null
    )

    fun requestPause() {
        isPauseRequested = true
    }

    fun resetPause() {
        isPauseRequested = false
    }

    /**
     * Extracts direct playable/downloadable video stream URLs from social media pages
     * (Facebook Reels/Watch, TikTok, YouTube, Instagram, X/Twitter).
     */
    private fun extractSocialMediaVideo(urlStr: String): Pair<String, String>? {
        val lower = urlStr.lowercase(Locale.ROOT)
        val isFacebook = lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com")
        val isTikTok = lower.contains("tiktok.com")
        val isYouTube = lower.contains("youtube.com") || lower.contains("youtu.be")
        val isInstagram = lower.contains("instagram.com")
        val isTwitter = lower.contains("twitter.com") || lower.contains("x.com")

        if (!isFacebook && !isTikTok && !isYouTube && !isInstagram && !isTwitter) {
            return null
        }

        if (isYouTube) {
            val videoId = when {
                urlStr.contains("youtu.be/") -> urlStr.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                urlStr.contains("watch?v=") -> urlStr.substringAfter("watch?v=").substringBefore("&")
                urlStr.contains("/shorts/") -> urlStr.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
                else -> null
            }
            if (!videoId.isNullOrBlank()) {
                return Pair(urlStr, "YouTube Video ($videoId)")
            }
        }

        try {
            var currentUrl = urlStr
            var redirects = 0
            var html = ""

            while (redirects < 5) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    setRequestProperty("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
                }

                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                        redirects++
                        continue
                    }
                }

                if (code == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                    val sb = StringBuilder()
                    var bytesRead = 0
                    while (true) {
                        val currentLine: String = reader.readLine() ?: break
                        sb.append(currentLine)
                        bytesRead += currentLine.length
                        if (bytesRead > 600_000) break // Read up to ~600KB of page
                    }
                    reader.close()
                    html = sb.toString()
                }
                conn.disconnect()
                break
            }

            if (html.isNotBlank()) {
                // 1. Check Facebook JSON properties
                val fbPatterns = listOf(
                    Regex("\"browser_native_hd_url\":\"(https:[^\"]+)\""),
                    Regex("\"browser_native_sd_url\":\"(https:[^\"]+)\""),
                    Regex("\"playable_url_quality_hd\":\"(https:[^\"]+)\""),
                    Regex("\"playable_url\":\"(https:[^\"]+)\""),
                    Regex("\"video_url\":\"(https:[^\"]+)\""),
                    Regex("<meta\\s+property=\"og:video\"\\s+content=\"(https:[^\"]+)\""),
                    Regex("<meta\\s+property=\"og:video:url\"\\s+content=\"(https:[^\"]+)\""),
                    Regex("<meta\\s+property=\"og:video:secure_url\"\\s+content=\"(https:[^\"]+)\"")
                )

                for (pattern in fbPatterns) {
                    val match = pattern.find(html)
                    if (match != null) {
                        val raw = match.groupValues[1]
                        val unescaped = raw.replace("\\u0026", "&")
                            .replace("\\/", "/")
                            .replace("&amp;", "&")
                        if (unescaped.startsWith("https://") || unescaped.startsWith("http://")) {
                            val platform = if (isFacebook) "فيديو فيسبوك (Facebook)" else if (isTikTok) "فيديو تيك توك (TikTok)" else "فيديو سوشيال ميديا"
                            return Pair(unescaped, platform)
                        }
                    }
                }

                // 2. TikTok specific regex
                val ttPatterns = listOf(
                    Regex("\"playAddr\":\"(https:[^\"]+)\""),
                    Regex("\"downloadAddr\":\"(https:[^\"]+)\""),
                    Regex("<video[^>]*src=\"(https:[^\"]+)\"")
                )
                for (pattern in ttPatterns) {
                    val match = pattern.find(html)
                    if (match != null) {
                        val raw = match.groupValues[1]
                        val unescaped = raw.replace("\\u0026", "&").replace("\\/", "/").replace("&amp;", "&")
                        return Pair(unescaped, "فيديو تيك توك (TikTok)")
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback for social media: Allow direct attempt without hard failure
        val platformLabel = when {
            isFacebook -> "فيديو فيسبوك (Facebook)"
            isTikTok -> "فيديو تيك توك (TikTok)"
            isInstagram -> "فيديو انستغرام (Instagram)"
            isTwitter -> "فيديو منصة X / Twitter"
            else -> "فيديو ويب"
        }
        return Pair(urlStr, platformLabel)
    }

    /**
     * Real Pre-Download Video Verifier:
     * Connects to the URL, verifies MIME type, checks social media pages (Facebook, TikTok, YouTube),
     * extracts real video format, file size, and resumability before allowing the download.
     */
    suspend fun verifyVideoBeforeDownload(urlStr: String): VideoVerificationResult = withContext(Dispatchers.IO) {
        val trimmed = normalizeUrl(urlStr)
        if (trimmed.isBlank() || (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true))) {
            return@withContext VideoVerificationResult.Invalid("الرابط غير صالح. يرجى كتابة أو لصق رابط صالح يبدأ بـ http:// أو https://")
        }

        // Check if social media URL (Facebook, TikTok, YouTube, Instagram, X)
        val socialExtracted = extractSocialMediaVideo(trimmed)
        if (socialExtracted != null) {
            val (directOrPageUrl, formatLabel) = socialExtracted
            if (directOrPageUrl != trimmed) {
                // Successfully extracted direct CDN video stream!
                return@withContext VideoVerificationResult.Valid(
                    directUrl = directOrPageUrl,
                    mimeType = "video/mp4",
                    contentLength = -1L,
                    format = "$formatLabel (رابط مباشر مستخرج 100%)",
                    isResumable = true,
                    server = "CDN"
                )
            } else {
                // Extraction failed: the URL is still a web page, downloading it would only save HTML.
                val isYt = trimmed.contains("youtube.com", true) || trimmed.contains("youtu.be", true)
                return@withContext VideoVerificationResult.SocialWebPage(
                    pageUrl = trimmed,
                    platform = formatLabel,
                    advice = if (isYt)
                        "تحميل فيديوهات يوتيوب غير مدعوم داخل التطبيق. يمكنك مشاهدته عبر يوتيوب أو استخدام خدمة YouTube Premium للتنزيل الرسمي."
                    else
                        "تعذر استخراج رابط الفيديو المباشر من هذه الصفحة (قد يكون الفيديو خاصاً أو محمياً). افتح الرابط في المستكشف المدمج (Web Video Sniffer) وشغّل الفيديو ليلتقطه التطبيق."
                )
            }
        }

        var currentUrl = trimmed
        var redirects = 0
        var conn: HttpURLConnection? = null

        try {
            while (redirects < 10) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    // Do NOT send Range on probe to avoid 400 Bad Request on CDNs!
                    setRequestProperty("User-Agent", USER_AGENT_CHROME)
                    setRequestProperty("Accept", "*/*")
                    instanceFollowRedirects = false
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 303 || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                        redirects++
                        continue
                    }
                }
                break
            }

            if (conn == null) {
                return@withContext VideoVerificationResult.Invalid("تعذر الاتصال بالخادم للتحقق من الرابط")
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                conn.disconnect()
                return@withContext VideoVerificationResult.Invalid("الخادم رد برمز خطأ ($responseCode). الرابط غير متاح أو منتهي الصلاحية.")
            }

            val contentType = conn.contentType?.lowercase(Locale.ROOT)?.split(";")?.first()?.trim() ?: "unknown"
            val contentLengthHeader = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            val contentRangeHeader = conn.getHeaderField("Content-Range")
            val totalLength = if (contentRangeHeader != null && contentRangeHeader.contains("/")) {
                contentRangeHeader.substringAfterLast("/").trim().toLongOrNull() ?: contentLengthHeader
            } else {
                contentLengthHeader
            }
            val acceptRanges = conn.getHeaderField("Accept-Ranges")
            val isResumable = (acceptRanges != null && acceptRanges.contains("bytes", ignoreCase = true)) || (responseCode == 206)
            val server = conn.getHeaderField("Server")
            conn.disconnect()

            // If HTML, check if page contains meta og:video before rejecting
            if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                val extracted = extractSocialMediaVideo(currentUrl)
                if (extracted != null && extracted.first != currentUrl) {
                    return@withContext VideoVerificationResult.Valid(
                        directUrl = extracted.first,
                        mimeType = "video/mp4",
                        contentLength = -1L,
                        format = "${extracted.second} (مستخرج)",
                        isResumable = true,
                        server = server
                    )
                }

                return@withContext VideoVerificationResult.Invalid(
                    reason = "الرابط يشير إلى صفحة ويب (HTML). إذا كان الفيديو داخل موقع تواصل (فيسبوك، تيك توك، يوتيوب)، تم تمكين التنزيل المباشر بالأسفل.",
                    detectedType = contentType
                )
            }

            if (contentType.contains("application/json") || contentType.contains("application/xml") && !contentType.contains("dash+xml")) {
                return@withContext VideoVerificationResult.Invalid(
                    reason = "الرابط يشير إلى واجهة بيانات برمجية (API/JSON) وليس ملف فيديو حقيقي.",
                    detectedType = contentType
                )
            }

            // Check if recognized video or media format
            val isVideoMime = contentType.startsWith("video/") ||
                    contentType.contains("mpegurl") ||
                    contentType.contains("dash+xml") ||
                    contentType.contains("mp4") ||
                    contentType.contains("ogg")

            val urlLower = currentUrl.lowercase(Locale.ROOT)
            val hasVideoExt = urlLower.contains(".mp4") || urlLower.contains(".mkv") || urlLower.contains(".webm") ||
                    urlLower.contains(".m3u8") || urlLower.contains(".mov") || urlLower.contains(".ts") ||
                    urlLower.contains(".avi") || urlLower.contains(".flv") || urlLower.contains(".3gp")

            if (!isVideoMime && !hasVideoExt && !contentType.contains("application/octet-stream")) {
                return@withContext VideoVerificationResult.Invalid(
                    reason = "نوع الملف المكتشف ($contentType) ليس فيديو قابل للتشغيل. يرجى توفير رابط فيديو حقيقي.",
                    detectedType = contentType
                )
            }

            val detectedFormat = when {
                contentType.contains("mp4") || urlLower.contains(".mp4") -> "MP4 (H.264 / AAC)"
                contentType.contains("webm") || urlLower.contains(".webm") -> "WebM (VP9 / Opus)"
                contentType.contains("matroska") || urlLower.contains(".mkv") -> "MKV (Matroska)"
                contentType.contains("mpegurl") || urlLower.contains(".m3u8") -> "HLS Live Stream (M3U8)"
                contentType.contains("dash") || urlLower.contains(".mpd") -> "MPEG-DASH Stream"
                contentType.contains("quicktime") || urlLower.contains(".mov") -> "MOV (QuickTime)"
                contentType.contains("mp2t") || urlLower.contains(".ts") -> "MPEG-2 Transport Stream"
                else -> "فيديو متوافق ($contentType)"
            }

            VideoVerificationResult.Valid(
                directUrl = currentUrl,
                mimeType = if (contentType != "unknown" && contentType != "application/octet-stream") contentType else "video/mp4",
                contentLength = totalLength,
                format = detectedFormat,
                isResumable = isResumable,
                server = server
            )
        } catch (e: Exception) {
            // If connection failed, but it looks like a social media or direct video URL, permit direct download attempt
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("tiktok.com") || lower.contains("youtube.com") || lower.contains(".mp4")) {
                VideoVerificationResult.Valid(
                    directUrl = trimmed,
                    mimeType = "video/mp4",
                    contentLength = -1L,
                    format = "فيديو مباشر (تنزيل ذكي)",
                    isResumable = true,
                    server = "Direct Stream"
                )
            } else {
                VideoVerificationResult.Invalid("تعذر الاتصال بالرابط وفحصه: ${e.message ?: "خطأ في الشبكة"}")
            }
        }
    }

    /**
     * Resolves direct media URL, follows redirects, detects real content type and filename.
     */
    suspend fun resolveMediaDetails(urlStr: String): Pair<String, String> = withContext(Dispatchers.IO) {
        var currentUrl = normalizeUrl(urlStr)
        var redirects = 0
        var finalUrl = currentUrl
        var mimeType = "video/mp4"

        while (redirects < 10) {
            try {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", USER_AGENT_CHROME)
                    setRequestProperty("Accept", "*/*")
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 303 || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                        redirects++
                        continue
                    }
                }

                finalUrl = currentUrl
                val contentType = conn.contentType
                if (!contentType.isNullOrBlank()) {
                    mimeType = contentType.split(";").first().trim()
                }
                conn.disconnect()
                break
            } catch (_: Exception) {
                break
            }
        }
        Pair(finalUrl, mimeType)
    }

    /**
     * Enqueues download using Android System DownloadManager.
     */
    fun downloadWithSystemManager(context: Context, urlStr: String, customFileName: String? = null): Long {
        val cleanUrl = normalizeUrl(urlStr)
        val uri = Uri.parse(cleanUrl)

        val ext = getExtensionFromUrl(cleanUrl)
        val fileName = customFileName ?: "Nova_Video_${System.currentTimeMillis()}.$ext"

        val request = DownloadManager.Request(uri).apply {
            setTitle("تحميل فيديو: $fileName")
            setDescription("جاري تنزيل الفيديوهات والوسائط إلى المجلد العام Downloads...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            addRequestHeader("User-Agent", USER_AGENT_CHROME)
            addRequestHeader("Accept", "*/*")
            setAllowedOverRoaming(true)
            setAllowedOverMetered(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    /**
     * Resumable HTTP Engine with Range Requests (206 Partial Content), Speed (KB/s, MB/s) & ETA calculations!
     */
    suspend fun downloadWithHttpEngine(
        context: Context,
        urlStr: String,
        onProgress: (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        resetPause()
        try {
            val normalizedUrl = normalizeUrl(urlStr)
            val (resolvedUrl, mimeType) = resolveMediaDetails(normalizedUrl)
            val url = URL(resolvedUrl)

            // Temp file for Range Request Resume support
            val safeKey = Math.abs(normalizedUrl.hashCode())
            val tempFile = File(context.cacheDir, "nova_dl_$safeKey.tmp")
            var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            var conn: HttpURLConnection? = null
            var responseCode = -1

            // 1. Try with Range header if existingBytes > 0
            if (existingBytes > 0) {
                try {
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 15000
                        setRequestProperty("User-Agent", USER_AGENT_CHROME)
                        setRequestProperty("Accept", "*/*")
                        setRequestProperty("Range", "bytes=$existingBytes-")
                    }
                    responseCode = conn.responseCode
                } catch (_: Exception) {}
            }

            // 2. If no existing bytes OR if server rejected Range (e.g. 400, 403, 405, 416, 500)
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                conn?.disconnect()
                try { tempFile.delete() } catch (_: Exception) {}
                existingBytes = 0L

                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", USER_AGENT_CHROME)
                    setRequestProperty("Accept", "*/*")
                }
                responseCode = conn.responseCode
            }

            val isPartial = (responseCode == HttpURLConnection.HTTP_PARTIAL)
            val isSuccess = (responseCode == HttpURLConnection.HTTP_OK)

            if (!isPartial && !isSuccess) {
                conn?.disconnect()
                val errorMsg = if (responseCode == 400) {
                    "خطأ من السيرفر (كود 400): هذا الرابط يحتاج استخراج مباشر. استخدم المستكشف المدمج (Web Video Sniffer) لالتقاطه فوراً!"
                } else if (responseCode == 403) {
                    "خطأ 403 (وصول مقيد): الرابط محمي بجلسة. استخدم المستكشف المدمج لالتقاط الفيديو أثناء تشغيله."
                } else {
                    "خطأ من السيرفر (كود $responseCode)"
                }
                onProgress(DownloadProgress(0, 0, 0, errorMsg = errorMsg))
                return@withContext
            }

            val activeConn = conn ?: return@withContext

            // Guard against downloading HTML pages when user wanted a direct video
            val detectedContentType = activeConn.contentType?.lowercase(Locale.ROOT) ?: ""
            if (detectedContentType.contains("text/html") && !resolvedUrl.contains(".m3u8")) {
                activeConn.disconnect()
                try { tempFile.delete() } catch (_: Exception) {}
                onProgress(DownloadProgress(0, 0, 0, errorMsg = "الرابط يقود إلى صفحة ويب وليس ملف فيديو مباشر. استخدم المستكشف المدمج لالتقاط الفيديو فوراً!"))
                return@withContext
            }

            if (detectedContentType.contains("mpegurl") || detectedContentType.contains("dash+xml") ||
                resolvedUrl.substringBefore("?").lowercase(Locale.ROOT).let { it.endsWith(".m3u8") || it.endsWith(".mpd") }) {
                activeConn.disconnect()
                try { tempFile.delete() } catch (_: Exception) {}
                onProgress(DownloadProgress(0, 0, 0, errorMsg = "هذا الرابط بث مباشر أو قائمة تشغيل (HLS/DASH) وليس ملف فيديو واحد، لذلك لا يمكن تنزيله كملف. شغّله من تبويب التلفاز."))
                return@withContext
            }

            val currentStartBytes = if (isPartial) existingBytes else 0L
            val contentLength = activeConn.contentLengthLong
            val totalBytes = if (isPartial && contentLength > 0) currentStartBytes + contentLength else contentLength.coerceAtLeast(currentStartBytes)

            val append = isPartial
            val fileOutputStream = FileOutputStream(tempFile, append)
            val inputStream: InputStream = activeConn.inputStream

            val buffer = ByteArray(64 * 1024)
            var bytesDownloaded = currentStartBytes
            var bytesThisSession = 0L

            val startTimeMs = System.currentTimeMillis()
            var lastUpdateMs = startTimeMs

            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                if (isPauseRequested) {
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    inputStream.close()
                    activeConn.disconnect()

                    val percent = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, percent, isPaused = true))
                    return@withContext
                }

                fileOutputStream.write(buffer, 0, read)
                bytesDownloaded += read
                bytesThisSession += read

                val nowMs = System.currentTimeMillis()
                if (nowMs - lastUpdateMs >= 350) {
                    lastUpdateMs = nowMs
                    val elapsedSec = (nowMs - startTimeMs) / 1000.0
                    val speedBytesPerSec = if (elapsedSec > 0) (bytesThisSession / elapsedSec).toLong() else 0L

                    val remainingBytes = if (totalBytes > bytesDownloaded) totalBytes - bytesDownloaded else 0L
                    val etaSec = if (speedBytesPerSec > 0) remainingBytes / speedBytesPerSec else -1L

                    val percent = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, percent, speedBytesPerSec, etaSec))
                }
            }

            fileOutputStream.flush()
            fileOutputStream.close()
            inputStream.close()
            activeConn.disconnect()

            if (totalBytes > 0 && bytesDownloaded < totalBytes) {
                // Connection closed early: keep the partial file so "resume" continues from here.
                onProgress(DownloadProgress(bytesDownloaded, totalBytes, ((bytesDownloaded * 100) / totalBytes).toInt(),
                    errorMsg = "انقطع الاتصال قبل اكتمال التحميل. اضغط تحميل مرة أخرى للمتابعة من حيث توقف."))
                return@withContext
            }

            // Download Complete! Transfer tempFile to Public MediaStore / Downloads
            // Servers often answer application/octet-stream (-> ".bin" files). Fall back to the URL / mp4.
            val finalMime = if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) mimeType
                else when (getExtensionFromUrl(resolvedUrl)) {
                    "mp3" -> "audio/mpeg"; "m4a", "aac" -> "audio/mp4"
                    "mkv" -> "video/x-matroska"; "webm" -> "video/webm"
                    "avi" -> "video/x-msvideo"; "mov" -> "video/quicktime"
                    else -> "video/mp4"
                }
            val ext = getExtensionFromMimeOrUrl(finalMime, resolvedUrl)
            val finalFileName = "CinemaX_${System.currentTimeMillis()}.$ext"
            var savedPath: String? = null
            val isVideo = finalMime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov")

            // 1. If Video on Android 10+ (Q+), insert directly into MediaStore.Video so it appears instantly in VideosScreen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isVideo) {
                try {
                    val videoValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
                        put(MediaStore.Video.Media.MIME_TYPE, if (finalMime.startsWith("video/")) finalMime else "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CinemaXPlayer")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val videoUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)
                    if (videoUri != null) {
                        context.contentResolver.openOutputStream(videoUri)?.use { out ->
                            FileInputStream(tempFile).use { inStream -> inStream.copyTo(out) }
                        }
                        videoValues.clear()
                        videoValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                        context.contentResolver.update(videoUri, videoValues, null, null)
                        savedPath = "مجلد الفيديوهات والمعرض (Movies/CinemaXPlayer/$finalFileName)"
                    }
                } catch (_: Exception) {}
            }

            // 2. Also save to Public Downloads if not saved as video or on older Android versions
            if (savedPath == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                            put(MediaStore.Downloads.MIME_TYPE, finalMime)
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                FileInputStream(tempFile).use { inStream -> inStream.copyTo(out) }
                            }
                            savedPath = "مجلد التحميلات العام والمعرض (Downloads/$finalFileName)"
                        }
                    } catch (_: Exception) {}
                }
            }

            // 3. Fallback to direct public storage write and MediaScanner indexing
            if (savedPath == null) {
                val targetDir = Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_DOWNLOADS
                )
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, finalFileName)
                FileInputStream(tempFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                savedPath = targetFile.absolutePath
                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(finalMime),
                        null
                    )
                } catch (_: Exception) {}
            }

            // Cleanup temp file
            try { tempFile.delete() } catch (_: Exception) {}

            onProgress(
                DownloadProgress(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes,
                    percent = 100,
                    isCompleted = true,
                    savedFilePath = savedPath
                )
            )

        } catch (e: Exception) {
            onProgress(DownloadProgress(0, 0, 0, errorMsg = "حدث خطأ أثناء التنزيل: ${e.localizedMessage ?: "يرجى الفحص وتأكيد الاتصال"}"))
        }
    }

    private fun getExtensionFromUrl(url: String): String {
        val clean = url.lowercase(Locale.ROOT)
        return when {
            clean.contains(".mp4") -> "mp4"
            clean.contains(".mkv") -> "mkv"
            clean.contains(".webm") -> "webm"
            clean.contains(".avi") -> "avi"
            clean.contains(".mov") -> "mov"
            clean.contains(".mp3") -> "mp3"
            clean.contains(".m4a") -> "m4a"
            clean.contains(".aac") -> "aac"
            else -> "mp4"
        }
    }

    private fun getExtensionFromMimeOrUrl(mime: String, url: String): String {
        val map = MimeTypeMap.getSingleton()
        val extFromMime = map.getExtensionFromMimeType(mime)
        if (!extFromMime.isNullOrBlank()) return extFromMime
        return getExtensionFromUrl(url)
    }

    fun formatSpeed(speedBytesPerSec: Long): String {
        if (speedBytesPerSec <= 0) return "0 كيلوبايت/ث"
        val kb = speedBytesPerSec / 1024.0
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB/s", kb)
        }
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB/s", mb)
    }

    fun formatEta(etaSeconds: Long): String {
        if (etaSeconds <= 0) return "--:--"
        val mins = etaSeconds / 60
        val secs = etaSeconds % 60
        return if (mins > 0) {
            String.format(Locale.US, "%d دقيقة و %d ثانية", mins, secs)
        } else {
            String.format(Locale.US, "%d ثانية", secs)
        }
    }
}
