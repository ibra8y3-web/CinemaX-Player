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
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()

        if (trimmed.startsWith("//")) return "https:$trimmed"

        // Only add HTTPS to a host-like value. Never rewrite intent://, fb://,
        // javascript:, file:, or arbitrary paths into malformed HTTPS URLs.
        val hasExplicitScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(trimmed)
        if (!hasExplicitScheme) {
            val hostCandidate = trimmed.substringBefore('/').substringBefore('?').substringBefore('#')
            if (hostCandidate.startsWith("www.", ignoreCase = true) ||
                (hostCandidate.contains('.') && !hostCandidate.startsWith('.'))) {
                trimmed = "https://$trimmed"
            }
        }
        return trimmed
    }

    private fun socialPlatformLabel(url: String): String? {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT) ?: return null
        fun matches(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            matches("facebook.com") || matches("fb.watch") || matches("fb.com") -> "Facebook"
            matches("instagram.com") -> "Instagram"
            matches("tiktok.com") -> "TikTok"
            matches("youtube.com") || matches("youtu.be") -> "YouTube"
            matches("x.com") || matches("twitter.com") -> "X / Twitter"
            else -> null
        }
    }

    private fun hasVideoFileExtension(url: String): Boolean {
        val path = Uri.parse(url).path?.lowercase(Locale.ROOT).orEmpty()
        val extension = path.substringAfterLast('.', "")
        return extension in setOf("mp4", "m4v", "webm", "mov", "mkv", "3gp", "avi", "flv", "m3u8", "mpd")
    }

    private fun applyRequestHeaders(
        connection: HttpURLConnection,
        headers: Map<String, String>,
        originalUrl: String
    ) {
        val originalHost = runCatching { URL(originalUrl).host }.getOrNull()
        val targetHost = runCatching { connection.url.host }.getOrNull()
        val allowed = setOf("user-agent", "referer", "origin", "cookie", "accept", "accept-language")
        val userAgent = headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value
        connection.setRequestProperty("User-Agent", userAgent?.takeIf { it.isNotBlank() } ?: USER_AGENT_CHROME)
        headers.forEach { (name, value) ->
            val key = name.lowercase(Locale.ROOT)
            if (name.isNotBlank() && value.isNotBlank() && key in allowed) {
                // A cookie captured for one CDN host must never be forwarded to another host.
                if (key != "cookie" || (!originalHost.isNullOrBlank() && originalHost.equals(targetHost, true))) {
                    connection.setRequestProperty(name, value)
                }
            }
        }
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
            val advice: String = "هذا رابط صفحة ويب وليس ملف فيديو مباشر. افتح الصفحة في المستكشف ثم التقط ملفاً مباشراً متاحاً للتنزيل."
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
     * Real Pre-Download Video Verifier:
     * Verifies direct media responses and identifies social landing pages without
     * pretending that a page URL is itself a downloadable video file.
     */
    suspend fun verifyVideoBeforeDownload(
        urlStr: String,
        requestHeaders: Map<String, String> = emptyMap()
    ): VideoVerificationResult = withContext(Dispatchers.IO) {
        val trimmed = normalizeUrl(urlStr)
        val parsed = Uri.parse(trimmed)
        if (trimmed.isBlank() ||
            parsed.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") ||
            parsed.host.isNullOrBlank()
        ) {
            return@withContext VideoVerificationResult.Invalid(
                "الرابط غير صالح. استخدم رابطاً يبدأ بـ https:// أو http://"
            )
        }

        val platform = socialPlatformLabel(trimmed)
        if (platform != null && !hasVideoFileExtension(trimmed)) {
            val advice = if (platform == "YouTube") {
                "هذا رابط صفحة YouTube وليس ملف فيديو مباشر. لا يستخرج CinemaX روابط YouTube الخاصة ولا يتجاوز DRM أو قيود المنصة. استخدم خيار التنزيل الرسمي المتاح من YouTube أو رابط ملف يتيحه مالك المحتوى."
            } else {
                "هذا رابط صفحة " + platform + " وليس ملف MP4. افتح الصفحة في المستكشف، وسجّل الدخول بنفسك عند الحاجة، ثم شغّل الفيديو. سيظهر فقط ملف فيديو مباشر إذا أتاحه الموقع للمتصفح دون حماية أو DRM."
            }
            return@withContext VideoVerificationResult.SocialWebPage(
                pageUrl = trimmed,
                platform = platform,
                advice = advice
            )
        }

        var currentUrl = trimmed
        var redirects = 0
        var conn: HttpURLConnection? = null
        try {
            while (redirects < 10) {
                val url = URL(currentUrl)
                val active = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "*/*")
                }
                applyRequestHeaders(active, requestHeaders, trimmed)
                conn = active

                val code = active.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = active.getHeaderField("Location")
                    active.disconnect()
                    conn = null
                    if (location.isNullOrBlank()) {
                        return@withContext VideoVerificationResult.Invalid("أعاد الخادم تحويلاً بلا عنوان وجهة.")
                    }
                    currentUrl = if (location.startsWith("http", true)) {
                        location
                    } else {
                        URL(url, location).toString()
                    }
                    redirects++
                    continue
                }
                break
            }

            val active = conn ?: return@withContext VideoVerificationResult.Invalid(
                "تعذر الاتصال بالخادم أو تجاوز الرابط عدد التحويلات المسموح."
            )
            val responseCode = active.responseCode
            if (responseCode !in 200..299) {
                val contentType = active.contentType?.substringBefore(';')?.trim().orEmpty()
                active.disconnect()
                conn = null
                return@withContext VideoVerificationResult.Invalid(
                    "الخادم رفض الطلب برمز " + responseCode + ". قد يكون الرابط منتهياً أو يحتاج جلسة دخول صالحة.",
                    contentType.ifBlank { null }
                )
            }

            val contentType = active.contentType?.lowercase(Locale.ROOT)?.substringBefore(';')?.trim() ?: "unknown"
            val contentLengthHeader = active.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            val contentRangeHeader = active.getHeaderField("Content-Range")
            val totalLength = if (!contentRangeHeader.isNullOrBlank() && contentRangeHeader.contains("/")) {
                contentRangeHeader.substringAfterLast("/").trim().toLongOrNull() ?: contentLengthHeader
            } else {
                contentLengthHeader
            }
            val acceptRanges = active.getHeaderField("Accept-Ranges")
            val isResumable = (!acceptRanges.isNullOrBlank() && acceptRanges.contains("bytes", true)) ||
                responseCode == HttpURLConnection.HTTP_PARTIAL
            val server = active.getHeaderField("Server")
            active.disconnect()
            conn = null

            if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                return@withContext VideoVerificationResult.Invalid(
                    "الرابط يعرض صفحة ويب (HTML) وليس ملف فيديو. افتح رابط الصفحة في المستكشف أو استخدم رابط ملف مباشر.",
                    contentType
                )
            }
            if (contentType.contains("application/json") ||
                (contentType.contains("application/xml") && !contentType.contains("dash+xml"))
            ) {
                return@withContext VideoVerificationResult.Invalid(
                    "الرابط يعرض بيانات API/JSON وليس ملف فيديو.",
                    contentType
                )
            }

            val lowerUrl = currentUrl.lowercase(Locale.ROOT)
            val isHls = contentType.contains("mpegurl") || lowerUrl.substringBefore('?').endsWith(".m3u8")
            val isDash = contentType.contains("dash+xml") || lowerUrl.substringBefore('?').endsWith(".mpd")
            val knownDirectExtension = hasVideoFileExtension(currentUrl)
            val knownVideoMime = contentType.startsWith("video/") ||
                contentType.contains("mp4") || contentType.contains("webm") ||
                contentType.contains("quicktime") || contentType.contains("matroska")
            val knownMediaCdn = listOf("fbcdn.net", "cdninstagram.com", "tiktokcdn.com", "twimg.com")
                .any { host -> (Uri.parse(currentUrl).host ?: "").lowercase(Locale.ROOT).endsWith(host) }
            val opaqueMedia = contentType == "application/octet-stream" && (knownDirectExtension || knownMediaCdn)

            if (!isHls && !isDash && !knownVideoMime && !knownDirectExtension && !opaqueMedia) {
                return@withContext VideoVerificationResult.Invalid(
                    "نوع المحتوى المكتشف (" + contentType + ") ليس ملف فيديو واضحاً.",
                    contentType
                )
            }

            if (isHls || isDash) {
                return@withContext VideoVerificationResult.Invalid(
                    if (isHls)
                        "هذا رابط قائمة HLS (M3U8)، وليس ملف فيديو منفرداً. تنزيل قوائم البث وتجميع مقاطعها غير متاح في هذا الإصدار."
                    else
                        "هذا رابط قائمة MPEG-DASH (MPD)، وليس ملف فيديو منفرداً. تنزيل قوائم البث وتجميع مقاطعها غير متاح في هذا الإصدار.",
                    contentType
                )
            }

            val format = when {
                contentType.contains("webm") || lowerUrl.contains(".webm") -> "WebM"
                contentType.contains("matroska") || lowerUrl.contains(".mkv") -> "MKV"
                contentType.contains("quicktime") || lowerUrl.contains(".mov") -> "MOV"
                contentType.contains("mp4") || lowerUrl.contains(".mp4") -> "MP4"
                else -> "ملف فيديو مباشر (" + contentType + ")"
            }

            VideoVerificationResult.Valid(
                directUrl = currentUrl,
                mimeType = if (contentType != "unknown") contentType else "video/mp4",
                contentLength = totalLength,
                format = format,
                isResumable = isResumable,
                server = server
            )
        } catch (e: Exception) {
            conn?.disconnect()
            val trimmedPath = Uri.parse(trimmed).path?.lowercase(Locale.ROOT).orEmpty()
            if (hasVideoFileExtension(trimmed) &&
                !trimmedPath.endsWith(".m3u8") &&
                !trimmedPath.endsWith(".mpd")
            ) {
                VideoVerificationResult.Valid(
                    directUrl = trimmed,
                    mimeType = "video/mp4",
                    contentLength = -1L,
                    format = "رابط ملف فيديو مباشر — تعذر قراءة بيانات الخادم",
                    isResumable = false,
                    server = null
                )
            } else {
                VideoVerificationResult.Invalid(
                    "تعذر التحقق من الرابط: " + (e.localizedMessage ?: "خطأ في الاتصال")
                )
            }
        }
    }

    /**
     * Resolves direct media URL, follows redirects, detects real content type and filename.
     */
    suspend fun resolveMediaDetails(
        urlStr: String,
        requestHeaders: Map<String, String> = emptyMap()
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        var currentUrl = normalizeUrl(urlStr)
        var redirects = 0
        var finalUrl = currentUrl
        var mimeType = "video/mp4"

        while (redirects < 10) {
            val url = URL(currentUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "*/*")
            }
            applyRequestHeaders(conn, requestHeaders, normalizeUrl(urlStr))
            try {
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http", true)) location else URL(url, location).toString()
                        redirects++
                        continue
                    }
                }
                finalUrl = currentUrl
                conn.contentType?.let { mimeType = it.substringBefore(';').trim() }
                conn.disconnect()
                break
            } catch (_: Exception) {
                conn.disconnect()
                break
            }
        }
        Pair(finalUrl, mimeType)
    }

    /**
     * Enqueues download using Android System DownloadManager.
     */
    fun downloadWithSystemManager(
        context: Context,
        urlStr: String,
        customFileName: String? = null,
        requestHeaders: Map<String, String> = emptyMap()
    ): Long {
        val cleanUrl = normalizeUrl(urlStr)
        val uri = Uri.parse(cleanUrl)

        val ext = getExtensionFromUrl(cleanUrl)
        val fileName = customFileName ?: "Nova_Video_${System.currentTimeMillis()}.$ext"

        val request = DownloadManager.Request(uri).apply {
            setTitle("تحميل فيديو: $fileName")
            setDescription("جاري تنزيل الفيديوهات والوسائط إلى المجلد العام Downloads...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            addRequestHeader("User-Agent", requestHeaders.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value ?: USER_AGENT_CHROME)
            addRequestHeader("Accept", requestHeaders.entries.firstOrNull { it.key.equals("Accept", true) }?.value ?: "*/*")
            requestHeaders.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank() &&
                    name.lowercase(Locale.ROOT) in setOf("referer", "origin", "cookie", "accept-language")
                ) {
                    addRequestHeader(name, value)
                }
            }
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
        requestHeaders: Map<String, String> = emptyMap(),
        onProgress: (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        resetPause()
        try {
            val normalizedUrl = normalizeUrl(urlStr)
            val (resolvedUrl, mimeType) = resolveMediaDetails(normalizedUrl, requestHeaders)
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
                    applyRequestHeaders(conn, requestHeaders, normalizedUrl)
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
                applyRequestHeaders(conn, requestHeaders, normalizedUrl)
                responseCode = conn.responseCode
            }

            val isPartial = (responseCode == HttpURLConnection.HTTP_PARTIAL)
            val isSuccess = (responseCode == HttpURLConnection.HTTP_OK)

            if (!isPartial && !isSuccess) {
                conn?.disconnect()
                val errorMsg = if (responseCode == 400) {
                    "خطأ 400: رفض الخادم الرابط. أعد فتح صفحة المصدر والتقط رابط الملف من جديد؛ قد يكون الرابط الموقّع انتهت صلاحيته."
                } else if (responseCode == 403) {
                    "خطأ 403 (وصول مقيد): انتهت صلاحية الرابط أو أن الخادم لم يقبل جلسة المتصفح. أعد فتح الصفحة والتقاط الرابط من جديد."
                } else {
                    "خطأ من السيرفر (كود $responseCode)"
                }
                onProgress(DownloadProgress(0, 0, 0, errorMsg = errorMsg))
                return@withContext
            }

            val activeConn = conn ?: return@withContext

            // Guard against downloading HTML pages when user wanted a direct video
            val detectedContentType = activeConn.contentType?.lowercase(Locale.ROOT) ?: ""
            if (detectedContentType.contains("text/html")) {
                activeConn.disconnect()
                try { tempFile.delete() } catch (_: Exception) {}
                onProgress(DownloadProgress(0, 0, 0, errorMsg = "الرابط يقود إلى صفحة ويب وليس ملف فيديو مباشر. افتح الصفحة في المستكشف وشغّل ملفاً مباشراً متاحاً للتنزيل."))
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
