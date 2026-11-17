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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    val requestHeaders: Map<String, String>
)

/**
 * Captures direct, unencrypted video files exposed by the page to Android WebView.
 * It does not bypass DRM, private content controls, or platform download restrictions.
 */
object WebVideoSnifferHelper {

    fun isMediaStreamUrl(url: String, mimeType: String? = null): Boolean {
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) return false
        val responseMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (responseMime.startsWith("video/") || responseMime.startsWith("audio/")) return true
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        val query = uri.encodedQuery?.lowercase().orEmpty()

        val extension = path.substringAfterLast('.', "")
        if (extension in setOf("mp4", "m4v", "webm", "mov", "mkv", "3gp", "avi", "flv")) {
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
            "https://www.facebook.com"
        }
    }

    var capturedSources by remember { mutableStateOf<List<CapturedVideoSource>>(emptyList()) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var browserError by remember { mutableStateOf<String?>(null) }
    var currentPageUrl by remember { mutableStateOf(safeInitialUrl) }
    var addressInput by remember { mutableStateOf(safeInitialUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // shouldInterceptRequest runs off the UI thread. These snapshots keep it
    // from reading Compose state or WebView settings on that background thread.
    val latestPageUrl = remember(safeInitialUrl) { AtomicReference(safeInitialUrl) }
    val captureUserAgent = remember(context) { WebSettings.getDefaultUserAgent(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun captureCandidate(url: String, pageUrl: String, userAgent: String, mimeType: String? = null) {
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

            val candidate = CapturedVideoSource(url, pageUrl, headers)
            if (capturedSources.none { it.url == candidate.url }) {
                capturedSources = (listOf(candidate) + capturedSources).take(5)
            }
        }
    }

    fun scanCurrentPage() {
        val view = webViewRef ?: return
        runCatching {
            scanWebViewForVideoSources(view) { url ->
                captureCandidate(url, latestPageUrl.get(), captureUserAgent)
            }
        }.onFailure {
            browserError = "تعذر فحص الصفحة بحثاً عن ملفات فيديو مباشرة."
        }
    }

    fun openCurrentPageExternally() {
        val target = currentPageUrl
        if (!target.startsWith("http://", true) && !target.startsWith("https://", true)) return
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
                            if (capturedSources.isEmpty()) "شغّل الفيديو ثم اضغط فحص" else "تم العثور على " + capturedSources.size + " رابط",
                            color = Color(0xFF22E3FF),
                            fontSize = 10.sp
                        )
                    }
                }

                if (capturedSources.isNotEmpty()) {
                    val selected = capturedSources.first()
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تم العثور على ملف فيديو مباشر", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(selected.url, color = Color(0xFF80CBC4), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                                        browserError = "الموقع طلب فتح رابط خارجي (" + scheme + "). تم إيقافه حتى لا تظهر صفحة بيضاء أو خطأ بروتوكول."
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (!url.isNullOrBlank()) {
                                            currentPageUrl = url
                                            latestPageUrl.set(url)
                                            addressInput = url
                                        }
                                        browserError = null
                                        isLoadingPage = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoadingPage = false
                                        if (!url.isNullOrBlank()) {
                                            currentPageUrl = url
                                            latestPageUrl.set(url)
                                            addressInput = url
                                        }
                                        view?.let {
                                            scanWebViewForVideoSources(it) { mediaUrl ->
                                                captureCandidate(mediaUrl, latestPageUrl.get(), captureUserAgent)
                                            }
                                        }
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        // This callback runs on a WebView worker thread. Inspect the response MIME
                                        // instead of relying only on URL suffixes: CDNs often use opaque URLs.
                                        val response = super.shouldInterceptRequest(view, request)
                                        val mediaUrl = request?.url?.toString().orEmpty()
                                        if (mediaUrl.isNotBlank()) {
                                            val pageReferer = request?.requestHeaders?.entries
                                                ?.firstOrNull { it.key.equals("Referer", true) }?.value
                                                ?: latestPageUrl.get()
                                            captureCandidate(
                                                mediaUrl,
                                                pageReferer,
                                                captureUserAgent,
                                                response?.mimeType
                                            )
                                        }
                                        return response
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

                    browserError?.let { message ->
                        Card(
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF20294A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("تعذر عرض الصفحة داخل التطبيق", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(message, color = Color(0xFFD3DAF1), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { openCurrentPageExternally() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح بالمتصفح الخارجي")
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
                        onVideoFound(capturedSources.first())
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استخدام الرابط وبدء التنزيل", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}

private fun scanWebViewForVideoSources(view: WebView, onFound: (String) -> Unit) {
    val script = """
        (function() {
            var results = [];
            document.querySelectorAll('video').forEach(function(video) {
                if (video.currentSrc) results.push(video.currentSrc);
                if (video.src) results.push(video.src);
                video.querySelectorAll('source').forEach(function(source) {
                    if (source.src) results.push(source.src);
                });
            });
            return JSON.stringify(Array.from(new Set(results)));
        })();
    """.trimIndent()

    view.evaluateJavascript(script) { rawResult ->
        runCatching {
            val jsonText = JSONTokener(rawResult).nextValue() as? String ?: "[]"
            val values = JSONArray(jsonText)
            for (index in 0 until values.length()) {
                values.optString(index).takeIf { it.isNotBlank() }?.let(onFound)
            }
        }
    }
}
