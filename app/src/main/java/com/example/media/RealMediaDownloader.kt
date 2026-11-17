package com.example.media

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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

    private fun socialPlatformLabel(url: String): String? =
        DownloadLinkAnalyzer.analyze(url).provider?.displayName

    private fun hasVideoFileExtension(url: String): Boolean {
        val path = Uri.parse(url).path?.lowercase(Locale.ROOT).orEmpty()
        val extension = path.substringAfterLast('.', "").takeIf { '.' in path }
        return DownloadLinkAnalyzer.isDownloadableExtension(extension)
    }

    private fun fileExtensionFromUrl(url: String): String? {
        val path = Uri.parse(url).path?.substringAfterLast('/').orEmpty()
        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension.takeIf { '.' in path && it.matches(Regex("[a-z0-9]{1,10}")) }
    }

    private fun extractFileName(contentDisposition: String?, url: String): String? {
        val disposition = contentDisposition.orEmpty()
        val encodedName = Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE)
            .find(disposition)?.groupValues?.getOrNull(1)
        val regularName = Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            .find(disposition)?.groupValues?.getOrNull(1)
        val raw = (encodedName?.let {
            runCatching { URLDecoder.decode(it.trim(), StandardCharsets.UTF_8.name()) }.getOrNull()
        } ?: regularName)?.trim()?.trim('"')
            ?: Uri.parse(url).lastPathSegment?.let {
                runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrNull()
            }
        return sanitizeFileName(raw)
    }

    private fun sanitizeFileName(raw: String?): String? {
        val name = raw?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            ?.replace(Regex("[<>\"/\\\\|?*:]"), "_")
            ?.trim()
            ?.trim('.')
        return name?.takeIf { it.isNotBlank() && it != "." && it != ".." }?.take(180)
    }

    private fun categoryFor(mimeType: String, extension: String): String = when {
        mimeType.startsWith("video/") || extension in setOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v", "flv", "mpeg", "mpg", "ts") -> "فيديو"
        mimeType.startsWith("audio/") || extension in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "aiff") -> "صوت"
        mimeType.startsWith("image/") || extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "avif", "svg") -> "صورة"
        mimeType.contains("pdf") || extension in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "epub", "mobi") -> "مستند"
        extension in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso") -> "أرشيف"
        else -> "ملف"
    }

    private fun mimeTypeForExtension(extension: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
            ?: "application/octet-stream"

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
            val server: String?,
            val fileName: String = "CinemaX_download.bin",
            val category: String = "ملف"
        ) : VideoVerificationResult() {
            val formattedSize: String
                get() = if (contentLength > 0) {
                    val mb = contentLength / (1024.0 * 1024.0)
                    if (mb >= 1024) String.format(Locale.US, "%.2f جيجابايت", mb / 1024.0)
                    else String.format(Locale.US, "%.1f ميجابايت", mb)
                } else "الحجم غير معلن من الخادم"
        }

        data class SocialWebPage(
            val pageUrl: String,
            val platform: String,
            val advice: String = "هذا رابط صفحة ويب وليس ملفاً مباشراً. افتح الصفحة في المستكشف والتقط ملفاً مباشراً متاحاً إذا سمح الموقع بذلك."
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
        val analysis = DownloadLinkAnalyzer.analyze(urlStr)
        val trimmed = analysis.normalizedUrl

        if (analysis.route == DownloadLinkAnalyzer.Route.INVALID) {
            return@withContext VideoVerificationResult.Invalid(analysis.advice)
        }
        if (analysis.route == DownloadLinkAnalyzer.Route.HLS_MANIFEST ||
            analysis.route == DownloadLinkAnalyzer.Route.DASH_MANIFEST
        ) {
            return@withContext VideoVerificationResult.Invalid(analysis.advice)
        }
        if (analysis.route == DownloadLinkAnalyzer.Route.SOCIAL_PAGE) {
            return@withContext VideoVerificationResult.SocialWebPage(
                pageUrl = trimmed,
                platform = analysis.provider?.displayName ?: "موقع ويب",
                advice = analysis.advice
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
                    setRequestProperty("Range", "bytes=0-511")
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
                    val nextUrl = if (location.startsWith("http", true)) location else URL(url, location).toString()
                    val nextScheme = Uri.parse(nextUrl).scheme?.lowercase(Locale.ROOT)
                    if (nextScheme !in setOf("http", "https") ||
                        (url.protocol.equals("https", true) && nextScheme != "https")
                    ) {
                        return@withContext VideoVerificationResult.Invalid("تم رفض تحويل غير آمن للرابط.")
                    }
                    currentUrl = nextUrl
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
                    "الخادم رفض الطلب برمز $responseCode. قد يكون الرابط منتهياً أو يحتاج جلسة دخول صالحة.",
                    contentType.ifBlank { null }
                )
            }

            val contentType = active.contentType?.lowercase(Locale.ROOT)?.substringBefore(';')?.trim()
                ?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
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
            val contentDisposition = active.getHeaderField("Content-Disposition")
            val suggestedName = extractFileName(contentDisposition, currentUrl)
            val extension = fileExtensionFromUrl(currentUrl)
                ?: suggestedName?.substringAfterLast('.', "")?.takeIf { '.' in suggestedName }
                ?: ""
            val mimeIsPage = contentType.contains("text/html") || contentType.contains("application/xhtml")
            val hasFileEvidence = !suggestedName.isNullOrBlank() ||
                DownloadLinkAnalyzer.isDownloadableExtension(extension)
            active.disconnect()
            conn = null

            if (mimeIsPage) {
                val provider = analysis.provider?.displayName ?: "موقع ويب"
                val advice = if (analysis.provider == DownloadLinkAnalyzer.Provider.YOUTUBE) {
                    analysis.advice
                } else {
                    "الرابط يفتح صفحة ويب ($provider) وليس ملفاً مباشراً. افتحها في المستكشف، وسجّل الدخول بنفسك عند الحاجة، ثم اختر ملفاً مباشراً إذا أتاحه الموقع دون حماية أو DRM."
                }
                return@withContext VideoVerificationResult.SocialWebPage(trimmed, provider, advice)
            }

            val isHls = contentType.contains("mpegurl") ||
                currentUrl.substringBefore('?').endsWith(".m3u8", true)
            val isDash = contentType.contains("dash+xml") ||
                currentUrl.substringBefore('?').endsWith(".mpd", true)
            if (isHls || isDash) {
                return@withContext VideoVerificationResult.Invalid(
                    if (isHls) {
                        "تم التعرف على قائمة بث HLS (M3U8). يستطيع مشغل CinemaX تشغيلها، لكن تنزيل وتجميع الأجزاء غير متاح من خلال محرك الملفات المباشرة."
                    } else {
                        "تم التعرف على قائمة MPEG-DASH (MPD). يستطيع مشغل CinemaX تشغيلها، لكن تنزيل وتجميع الأجزاء غير متاح من خلال محرك الملفات المباشرة."
                    },
                    contentType
                )
            }

            val isStructuredData = contentType.contains("application/json") ||
                contentType.contains("application/xml") || contentType.contains("text/xml")
            val hasKnownExtension = DownloadLinkAnalyzer.isDownloadableExtension(extension)
            val isTextFile = contentType.startsWith("text/") && hasFileEvidence
            if (isStructuredData && !hasFileEvidence) {
                return@withContext VideoVerificationResult.Invalid(
                    "الخادم أعاد بيانات API أو JSON/XML وليس ملف تنزيل واضحاً. استخدم رابط الملف أو مرفق التنزيل الأصلي.",
                    contentType
                )
            }

            val isKnownFileMime = contentType.startsWith("video/") ||
                contentType.startsWith("audio/") ||
                contentType.startsWith("image/") ||
                contentType.startsWith("font/") ||
                (contentType.startsWith("application/") && !isStructuredData)
            val isOpaqueBinary = contentType == "application/octet-stream"
            if (!isKnownFileMime && !hasKnownExtension && !isTextFile && !isOpaqueBinary) {
                return@withContext VideoVerificationResult.Invalid(
                    "لم يستطع الخادم تحديد نوع ملف قابل للتنزيل. نوع المحتوى: $contentType",
                    contentType
                )
            }

            val finalExtension = extension.ifBlank {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.ROOT) ?: "bin"
            }
            val finalMime = if (contentType == "application/octet-stream" && finalExtension != "bin") {
                mimeTypeForExtension(finalExtension)
            } else contentType
            val fileName = suggestedName
                ?: Uri.parse(currentUrl).lastPathSegment?.let(::sanitizeFileName)
                ?: "CinemaX_\${System.currentTimeMillis()}.$finalExtension"
            val category = categoryFor(finalMime, finalExtension)
            val format = if (finalExtension.isNotBlank()) {
                finalExtension.uppercase(Locale.ROOT)
            } else {
                finalMime
            }

            VideoVerificationResult.Valid(
                directUrl = currentUrl,
                mimeType = finalMime,
                contentLength = totalLength,
                format = format,
                isResumable = isResumable,
                server = server,
                fileName = fileName,
                category = category
            )
        } catch (e: Exception) {
            conn?.disconnect()
            val extension = fileExtensionFromUrl(trimmed)
            if (DownloadLinkAnalyzer.isDownloadableExtension(extension)) {
                val mimeType = mimeTypeForExtension(extension.orEmpty())
                VideoVerificationResult.Valid(
                    directUrl = trimmed,
                    mimeType = mimeType,
                    contentLength = -1L,
                    format = extension.orEmpty().uppercase(Locale.ROOT),
                    isResumable = false,
                    server = null,
                    fileName = Uri.parse(trimmed).lastPathSegment?.let(::sanitizeFileName)
                        ?: "CinemaX_\${System.currentTimeMillis()}.$extension",
                    category = categoryFor(mimeType, extension.orEmpty())
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
        mimeType: String? = null,
        requestHeaders: Map<String, String> = emptyMap()
    ): Long {
        val cleanUrl = normalizeUrl(urlStr)
        val uri = Uri.parse(cleanUrl)

        val ext = getExtensionFromUrl(cleanUrl)
        val fileName = sanitizeFileName(customFileName) ?: "CinemaX_${System.currentTimeMillis()}.$ext"

        val request = DownloadManager.Request(uri).apply {
            setTitle("تحميل ملف: $fileName")
            setDescription("CinemaX Player — تنزيل ملف إلى مجلد Downloads")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            mimeType?.takeIf { it.isNotBlank() }?.let { setMimeType(it) }
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
        customFileName: String? = null,
        mimeTypeHint: String? = null,
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

            // Preserve the server MIME type and filename rather than forcing every download to MP4.
            val hintedMime = mimeTypeHint?.takeIf { it.isNotBlank() && it != "unknown" }
            val finalMime = hintedMime ?: mimeType
            val hintedName = sanitizeFileName(customFileName)
            val nameExtension = hintedName?.substringAfterLast('.', "")?.takeIf { '.' in hintedName }
            val extension = nameExtension?.lowercase(Locale.ROOT)
                ?: getExtensionFromMimeOrUrl(finalMime, resolvedUrl)
            val mappedMime = if (finalMime == "application/octet-stream" && extension != "bin") {
                mimeTypeForExtension(extension)
            } else finalMime
            val finalFileName = hintedName ?: "CinemaX_\${System.currentTimeMillis()}.$extension"
            val savedPath = saveDownloadedFile(context, tempFile, finalFileName, mappedMime)

            // Cleanup temp file
            runCatching { tempFile.delete() }

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

    data class SharedImportResult(
        val fileName: String,
        val mimeType: String,
        val bytesCopied: Long,
        val savedLocation: String
    )

    /**
     * Imports a file shared from another Android app (Telegram, browser, file manager, etc.).
     * The caller must retain the temporary URI permission granted by Android's Sharesheet.
     */
    suspend fun importSharedContent(context: Context, uri: Uri): SharedImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName: String? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) displayName = cursor.getString(index)
            }
        }
        val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        val fallbackExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val safeName = sanitizeFileName(displayName) ?: "CinemaX_\${System.currentTimeMillis()}.$fallbackExtension"
        val tempFile = File(
            context.cacheDir,
            "share_import_\${System.currentTimeMillis()}_\${safeName.hashCode().toUInt().toString(16)}.tmp"
        )
        try {
            val input = resolver.openInputStream(uri)
                ?: throw IllegalStateException("تعذر فتح الملف المرسل من التطبيق الآخر.")
            var byteCount = 0L
            input.use { source ->
                FileOutputStream(tempFile).use { target ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                        byteCount += count
                    }
                    target.flush()
                }
            }
            if (byteCount <= 0L) throw IllegalStateException("الملف المرسل فارغ أو غير قابل للقراءة.")
            val saved = saveDownloadedFile(context, tempFile, safeName, mimeType)
            SharedImportResult(safeName, mimeType, byteCount, saved)
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun saveDownloadedFile(
        context: Context,
        sourceFile: File,
        requestedName: String,
        mimeType: String
    ): String {
        val name = sanitizeFileName(requestedName) ?: "CinemaX_\${System.currentTimeMillis()}.bin"
        val mime = mimeType.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        val isVideo = mime.startsWith("video/")
        val isAudio = mime.startsWith("audio/")
        val isImage = mime.startsWith("image/")
        val folderName = when {
            isVideo -> Environment.DIRECTORY_MOVIES
            isAudio -> Environment.DIRECTORY_MUSIC
            isImage -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_DOWNLOADS
        }
        val appFolder = "CinemaXPlayer"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when {
                isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$folderName/$appFolder")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val destination = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف الوجهة في التخزين العام.")
            try {
                context.contentResolver.openOutputStream(destination, "w")?.use { output ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(output, 64 * 1024) }
                } ?: throw IllegalStateException("تعذر فتح ملف الوجهة للكتابة.")
                val completed = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(destination, completed, null, null)
                return "$folderName/$appFolder/$name"
            } catch (error: Exception) {
                runCatching { context.contentResolver.delete(destination, null, null) }
                throw error
            }
        }

        val publicBase = Environment.getExternalStoragePublicDirectory(folderName)
        val targetDirectory = File(publicBase, appFolder)
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw IllegalStateException("تعذر إنشاء مجلد التنزيلات.")
        }
        val targetFile = File(targetDirectory, name)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output -> input.copyTo(output, 64 * 1024) }
        }
        runCatching {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mime),
                null
            )
        }
        return targetFile.absolutePath
    }

    private fun getExtensionFromUrl(url: String): String =
        fileExtensionFromUrl(url) ?: "bin"

    private fun getExtensionFromMimeOrUrl(mime: String, url: String): String {
        fileExtensionFromUrl(url)?.let { return it }
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?.takeIf { it.isNotBlank() } ?: "bin"
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
