package com.example.media

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicReference

data class CapturedVideoSource(
    val url: String,
    val pageUrl: String,
    val requestHeaders: Map<String, String>,
    val sourceKind: String = "network",
    val mimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val currentTimeMs: Long = 0L,
    val isPlaying: Boolean = false
) {
    val confidenceScore: Int
        get() = when (sourceKind) {
            "active-video", "active-audio" -> if (isPlaying) 120 else 100
            "video-source", "audio-source" -> 80
            else -> 10
        } + if (width > 0 && height > 0) 10 else 0
}

/**
 * Captures direct, unencrypted video files exposed by the page to Android WebView.
 * It does not bypass DRM, private content controls, or platform download restrictions.
 */
object WebVideoSnifferHelper {

    fun isMediaStreamUrl(url: String, mimeType: String? = null): Boolean {
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) return false
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        val query = uri.encodedQuery?.lowercase().orEmpty()

        val extension = path.substringAfterLast('.', "")
        if (extension in setOf("m3u8", "mpd")) return false
        val responseMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (responseMime.startsWith("video/") || responseMime.startsWith("audio/")) return true
        if (extension in setOf(
                "mp4", "m4v", "webm", "mov", "mkv", "3gp", "avi", "flv",
                "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "aiff"
            )
        ) {
            return true
        }
        if (query.contains("mime_type=video_mp4") || query.contains("content_type=video")) {
            return true
        }

        val facebookCdn = host.endsWith("fbcdn.net") &&
            path.contains("/v/") &&
            (query.contains("oe=") || query.contains("bytestart=") ||
                query.contains("efg=") || query.contains("stp="))
        val instagramCdn = host.endsWith("cdninstagram.com") &&
            path.contains("/v/") &&
            (query.contains("oe=") || query.contains("stp=") || query.contains("efg="))
        val tiktokCdn = (host.contains("tiktokcdn") || host.contains("byteoversea")) &&
            path.contains("/video/")
        val xCdn = host == "video.twimg.com" || host.endsWith(".video.twimg.com")

        return facebookCdn || instagramCdn || tiktokCdn ||
            (xCdn && (path.contains("amplify_video") || path.contains("ext_tw_video") ||
                path.contains("tweet_video")))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebVideoSnifferDialog(
    initialUrl: String,
    onVideoFound: (CapturedVideoSource) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val safeInitialUrl = remember(initialUrl) {
        val normalized = RealMediaDownloader.normalizeUrl(initialUrl)
        if (normalized.startsWith("https://", true) || normalized.startsWith("http://", true)) {
            normalized
        } else {
            "about:blank"
        }
    }

    var capturedSources by remember { mutableStateOf<List<CapturedVideoSource>>(emptyList()) }
    var selectedSourceUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var browserError by remember { mutableStateOf<String?>(null) }
    var currentPageUrl by remember { mutableStateOf(safeInitialUrl.takeIf { it != "about:blank" }.orEmpty()) }
    var addressInput by remember { mutableStateOf(safeInitialUrl.takeIf { it != "about:blank" }.orEmpty()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // shouldInterceptRequest runs off the UI thread. These snapshots keep it
    // from reading Compose state or WebView settings on that background thread.
    val latestPageUrl = remember(safeInitialUrl) { AtomicReference(safeInitialUrl) }
    val captureUserAgent = remember(context) { WebSettings.getDefaultUserAgent(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun captureCandidate(
        url: String,
        pageUrl: String,
        userAgent: String,
        mimeType: String? = null,
        sourceKind: String = "network",
        width: Int = 0,
        height: Int = 0,
        durationMs: Long = 0L,
        currentTimeMs: Long = 0L,
        isPlaying: Boolean = false
    ) {
        if (!WebVideoSnifferHelper.isMediaStreamUrl(url, mimeType)) return
        mainHandler.post {
            val headers = linkedMapOf(
                "User-Agent" to userAgent,
                "Referer" to pageUrl,
                "Accept" to "*/*"
            )
            runCatching { CookieManager.getInstance().getCookie(url) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { headers["Cookie"] = it }

            val candidate = CapturedVideoSource(
                url = url,
                pageUrl = pageUrl,
                requestHeaders = headers,
                sourceKind = sourceKind,
                mimeType = mimeType,
                width = width,
                height = height,
                durationMs = durationMs,
                currentTimeMs = currentTimeMs,
                isPlaying = isPlaying
            )
            val existing = capturedSources.firstOrNull { it.url == candidate.url }
            if (existing == null || candidate.confidenceScore >= existing.confidenceScore) {
                val updated = (capturedSources.filterNot { it.url == candidate.url } + candidate)
                    .sortedByDescending { it.confidenceScore }
                    .take(10)
                capturedSources = updated
                if (selectedSourceUrl == null || updated.firstOrNull()?.url == candidate.url) {
                    selectedSourceUrl = updated.firstOrNull()?.url
                }
            }
        }
    }

    fun scanCurrentPage() {
        val view = webViewRef ?: return
        runCatching {
            scanWebViewForVideoSources(view) { media ->
                captureCandidate(
                    url = media.url,
                    pageUrl = latestPageUrl.get(),
                    userAgent = captureUserAgent,
                    mimeType = media.mimeType,
                    sourceKind = media.sourceKind,
                    width = media.width,
                    height = media.height,
                    durationMs = media.durationMs,
                    currentTimeMs = media.currentTimeMs,
                    isPlaying = media.isPlaying
                )
            }
        }.onFailure {
            browserError = "تعذر فحص الصفحة بحثاً عن ملفات فيديو مباشرة."
        }
    }

    fun openCurrentPageExternally() {
        val target = currentPageUrl
        if (!target.startsWith("http://", true) && !target.startsWith("https://", true)) {
            browserError = "أدخل رابط صفحة أولاً ثم افتحها في المتصفح الخارجي."
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
        }.onFailure {
            browserError = "تعذر فتح المتصفح الخارجي على هذا الجهاز."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.clearHistory()
                    view.removeAllViews()
                    view.destroy()
                }
            }
            webViewRef = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x3322E3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("المستكشف الذكي للفيديو", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("التقاط ملفات الفيديو المباشرة المتاحة", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(440.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        singleLine = true,
                        placeholder = { Text("الصق رابط الصفحة أو الموقع") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22E3FF),
                            unfocusedBorderColor = Color(0xFF596080),
                            focusedPlaceholderColor = Color(0xFFA9B4D6),
                            unfocusedPlaceholderColor = Color(0xFFA9B4D6)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val target = RealMediaDownloader.normalizeUrl(addressInput)
                            if (target.startsWith("https://", true) || target.startsWith("http://", true)) {
                                capturedSources = emptyList()
                                selectedSourceUrl = null
                                currentPageUrl = target
                                latestPageUrl.set(target)
                                browserError = null
                                isLoadingPage = true
                                webViewRef?.loadUrl(target)
                            } else {
                                browserError = "أدخل رابط موقع يبدأ بـ https:// أو http://"
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "فتح الرابط", tint = Color(0xFF22E3FF))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color(0xFFA9B4D6))
                        }
                        IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFFA9B4D6))
                        }
                        IconButton(onClick = { scanCurrentPage() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "فحص الصفحة", tint = Color(0xFF22E3FF))
                        }
                        IconButton(onClick = { openCurrentPageExternally() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح بالمتصفح", tint = Color(0xFFA9B4D6))
                        }
                    }
                    if (isLoadingPage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF22E3FF)
                        )
                    } else {
                        Text(
                            if (capturedSources.isEmpty()) "شغّل الفيديو أو الصوت ثم اضغط فحص" else "تم العثور على " + capturedSources.size + " مصدر وسائط",
                            color = Color(0xFF22E3FF),
                            fontSize = 10.sp
                        )
                    }
                }

                if (capturedSources.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 88.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "مصادر متاحة: ${capturedSources.size} — اختر المصدر الصحيح",
                            color = Color(0xFFB8C5EB),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                        capturedSources.forEach { source ->
                            val isSelected = selectedSourceUrl == source.url
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSourceUrl = source.url },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF15372F) else Color(0xFF20294A)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFF2EE6A6) else Color(0xFF465176)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF2EE6A6) else Color(0xFF22E3FF),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            when {
                                                source.isPlaying && source.mimeType?.startsWith("audio/") == true -> "الصوت الذي يعمل الآن"
                                                source.isPlaying -> "الفيديو الذي يعمل الآن"
                                                source.sourceKind == "active-audio" -> "مشغل صوت في الصفحة"
                                                source.sourceKind == "audio-source" -> "مصدر صوت في الصفحة"
                                                source.sourceKind == "active-video" -> "عنصر فيديو في الصفحة"
                                                source.sourceKind == "video-source" -> "مصدر فيديو في الصفحة"
                                                else -> "طلب شبكة مرشح — تحقق قبل التنزيل"
                                            },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            if (isSelected) "محدد" else "اختيار",
                                            color = if (isSelected) Color(0xFF2EE6A6) else Color(0xFF9EABD0),
                                            fontSize = 9.sp
                                        )
                                    }
                                    if (source.width > 0 && source.height > 0) {
                                        Text(
                                            "${source.width} × ${source.height} · " +
                                                if (source.durationMs > 0) String.format(java.util.Locale.US, "%.0f ثانية", source.durationMs / 1000.0)
                                                else "المدة غير معلنة",
                                            color = Color(0xFF9EABD0),
                                            fontSize = 8.sp
                                        )
                                    }
                                    Text(
                                        Uri.parse(source.url).host ?: "مصدر فيديو مباشر",
                                        color = Color(0xFF80CBC4),
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
                                webViewRef = this
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowFileAccess = false
                                    allowContentAccess = true
                                    userAgentString = captureUserAgent
                                }
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        isLoadingPage = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        val scheme = request.url.scheme?.lowercase().orEmpty()
                                        if (scheme == "http" || scheme == "https") return false
                                        // A social page may hand off to its official app.
                                        browserError = "الموقع طلب فتح تطبيق خارجي. إذا لم يظهر المحتوى هنا، استخدم فتح الصفحة في المتصفح."
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                                            currentPageUrl = url
                                            latestPageUrl.set(url)
                                            addressInput = url
                                        }
                                        browserError = null
                                        isLoadingPage = !url.equals("about:blank", ignoreCase = true)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoadingPage = false
                                        if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                                            currentPageUrl = url
                                            latestPageUrl.set(url)
                                            addressInput = url
                                        }
                                        view?.let {
                                            scanWebViewForVideoSources(it) { media ->
                                                captureCandidate(
                                                    url = media.url,
                                                    pageUrl = latestPageUrl.get(),
                                                    userAgent = captureUserAgent,
                                                    mimeType = media.mimeType,
                                                    sourceKind = media.sourceKind,
                                                    width = media.width,
                                                    height = media.height,
                                                    durationMs = media.durationMs,
                                                    currentTimeMs = media.currentTimeMs,
                                                    isPlaying = media.isPlaying
                                                )
                                            }
                                        }
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        // The default WebView callback does not expose response headers.
                                        // Element discovery below supplies the known video/audio media type.
                                        val mediaUrl = request?.url?.toString().orEmpty()
                                        if (mediaUrl.isNotBlank()) {
                                            val pageReferer = request?.requestHeaders?.entries
                                                ?.firstOrNull { it.key.equals("Referer", true) }?.value
                                                ?: latestPageUrl.get()
                                            captureCandidate(mediaUrl, pageReferer, captureUserAgent)
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onLoadResource(view: WebView?, url: String?) {
                                        super.onLoadResource(view, url)
                                        if (!url.isNullOrBlank()) {
                                            captureCandidate(url, latestPageUrl.get(), captureUserAgent)
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            browserError = error?.description?.toString()
                                                ?: "تعذر تحميل الصفحة داخل المستكشف."
                                            isLoadingPage = false
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?
                                    ) {
                                        super.onReceivedHttpError(view, request, errorResponse)
                                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                            browserError = "رفض الموقع عرض الصفحة. رمز HTTP: " +
                                                (errorResponse?.statusCode?.toString() ?: "غير معروف")
                                            isLoadingPage = false
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        isLoadingPage = false
                                        browserError = if (detail?.didCrash() == true) {
                                            "تعطل محرك عرض الصفحة. لم يتم إغلاق CinemaX؛ يمكنك فتح الرابط في المتصفح الخارجي أو تجربة صفحة أخرى."
                                        } else {
                                            "أوقف النظام محرك عرض الصفحة لتحرير الذاكرة. يمكنك إعادة فتح الصفحة أو استخدام المتصفح الخارجي."
                                        }
                                        if (view != null) {
                                            if (webViewRef === view) webViewRef = null
                                            runCatching { (view.parent as? ViewGroup)?.removeView(view) }
                                            runCatching { view.destroy() }
                                        }
                                        return true
                                    }
                                }
                                runCatching { loadUrl(safeInitialUrl) }
                                    .onFailure {
                                        browserError = "تعذر فتح الصفحة داخل المستكشف: ${it.localizedMessage ?: "خطأ WebView"}"
                                        isLoadingPage = false
                                    }
                                }
                            } catch (error: Exception) {
                                webViewRef = null
                                isLoadingPage = false
                                browserError = "تعذر تشغيل المستكشف المدمج على هذا الجهاز: ${error.localizedMessage ?: "مكوّن WebView غير متاح"}"
                                TextView(ctx).apply {
                                    text = "تعذر تشغيل مستعرض الويب داخل التطبيق. استخدم زر فتح بالمتصفح الخارجي."
                                    setTextColor(android.graphics.Color.WHITE)
                                    textSize = 14f
                                    gravity = android.view.Gravity.CENTER
                                    setPadding(24, 24, 24, 24)
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (currentPageUrl.isBlank() && browserError == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF101633)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("ابدأ برابط الموقع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "الصق رابط Facebook أو TikTok أو أي صفحة تريد فحصها في الأعلى، ثم اضغط زر البحث. شغّل الوسائط واضغط فحص.",
                                    color = Color(0xFFB8C5EB),
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    browserError?.let { message ->
                        Card(
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF20294A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("تعذر تحميل الصفحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(message, color = Color(0xFFD3DAF1), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { browserError = null }) {
                                        Text("متابعة", color = Color(0xFF2EE6A6), fontSize = 10.sp)
                                    }
                                    TextButton(onClick = { openCurrentPageExternally() }) {
                                        Text("فتح خارجياً", color = Color(0xFF22E3FF), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (capturedSources.isNotEmpty()) {
                Button(
                    onClick = {
                        val selected = capturedSources.firstOrNull { it.url == selectedSourceUrl }
                            ?: capturedSources.firstOrNull()
                        if (selected != null) onVideoFound(selected)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استخدام الفيديو المحدد", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
                if (currentPageUrl.startsWith("http://", true) || currentPageUrl.startsWith("https://", true)) {
                    TextButton(onClick = { openCurrentPageExternally() }) {
                        Text("فتح الصفحة في المتصفح الخارجي", color = Color(0xFF22E3FF))
                    }
                }
            }
        }
    )
}

private data class WebPageVideoSource(
    val url: String,
    val mimeType: String?,
    val sourceKind: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val currentTimeMs: Long,
    val isPlaying: Boolean
)

private fun scanWebViewForVideoSources(
    view: WebView,
    onFound: (WebPageVideoSource) -> Unit
) {
    val script = """
        (function() {
            var results = [];
            function pushMedia(url, mimeType, sourceKind, video) {
                if (!url || !/^https?:/i.test(url)) return;
                var currentTime = Number(video.currentTime || 0);
                var duration = Number(video.duration || 0);
                var playing = !video.paused && !video.ended &&
                    video.readyState >= 2 && currentTime > 0;
                results.push({
                    url: url,
                    mimeType: mimeType || 'video/*',
                    sourceKind: sourceKind,
                    width: Number(video.videoWidth || 0),
                    height: Number(video.videoHeight || 0),
                    durationMs: isFinite(duration) ? Math.round(duration * 1000) : 0,
                    currentTimeMs: isFinite(currentTime) ? Math.round(currentTime * 1000) : 0,
                    isPlaying: playing
                });
            }
            document.querySelectorAll('video, audio').forEach(function(video) {
                var isAudio = video.tagName.toLowerCase() === 'audio';
                var fallbackMime = isAudio ? 'audio/*' : 'video/*';
                var activeKind = isAudio ? 'active-audio' : 'active-video';
                var sourceKind = isAudio ? 'audio-source' : 'video-source';
                var activeUrl = video.currentSrc || '';
                if (activeUrl) pushMedia(activeUrl, video.getAttribute('type') || fallbackMime, activeKind, video);
                if (video.src && video.src !== activeUrl) {
                    pushMedia(video.src, video.getAttribute('type') || fallbackMime, sourceKind, video);
                }
                video.querySelectorAll('source').forEach(function(source) {
                    if (source.src && source.src !== activeUrl && source.src !== video.src) {
                        pushMedia(source.src, source.type || fallbackMime, sourceKind, video);
                    }
                });
            });
            return JSON.stringify(results);
        })();
    """.trimIndent()

    view.evaluateJavascript(script) { rawResult ->
        runCatching {
            val jsonText = JSONTokener(rawResult).nextValue() as? String ?: "[]"
            val values = JSONArray(jsonText)
            for (index in 0 until values.length()) {
                val media = values.optJSONObject(index) ?: continue
                val url = media.optString("url").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val mimeType = media.optString("mimeType").takeIf { it.isNotBlank() && it != "null" }
                val sourceKind = media.optString("sourceKind", "video-source")
                onFound(
                    WebPageVideoSource(
                        url = url,
                        mimeType = mimeType,
                        sourceKind = sourceKind,
                        width = media.optInt("width", 0),
                        height = media.optInt("height", 0),
                        durationMs = media.optLong("durationMs", 0L),
                        currentTimeMs = media.optLong("currentTimeMs", 0L),
                        isPlaying = media.optBoolean("isPlaying", false)
                    )
                )
            }
        }
    }
}
