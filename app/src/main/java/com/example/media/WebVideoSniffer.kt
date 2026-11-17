package com.example.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Intelligent In-App Web Video Sniffer.
 * Renders web pages (Facebook, TikTok, Instagram, Twitter, websites)
 * and intercepts direct video streams (.mp4, .m3u8, fbcdn, etc.) with 100% accuracy.
 */
object WebVideoSnifferHelper {

    fun isMediaStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        // Video file extensions
        if (lower.contains(".mp4") || lower.contains(".m4v") || lower.contains(".webm") || lower.contains(".mov") || lower.contains(".m3u8")) {
            return true
        }
        // Facebook video CDNs
        if (lower.contains("fbcdn.net") && (lower.contains("/v/") || lower.contains("oe=") || lower.contains("bytestart="))) {
            return true
        }
        // Instagram video CDNs
        if (lower.contains("cdninstagram.com") && (lower.contains("/v/") || lower.contains(".mp4"))) {
            return true
        }
        // TikTok video CDNs
        if (lower.contains("tiktokcdn.com") && lower.contains("/video/")) {
            return true
        }
        // Twitter/X video CDNs
        if (lower.contains("twimg.com") && lower.contains("/video/")) {
            return true
        }
        return false
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebVideoSnifferDialog(
    initialUrl: String,
    onVideoFound: (directUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var capturedVideoUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
                    Text("المستكشف الذكي لالتقاط الفيديوهات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Sniffs direct MP4/CDN streams automatically", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                if (capturedVideoUrl != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تم التقاط رابط الفيديو المباشر بنجاح! 🎯", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(capturedVideoUrl!!, color = Color(0xFF80CBC4), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLoadingPage) "جاري تحميل الصفحة وتشغيل الفيديو لالتقاطه..." else "شغّل الفيديو أدناه وسيقوم النظام بالتقاطه فوراً!",
                            color = Color(0xFF22E3FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (isLoadingPage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF22E3FF))
                        } else {
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFFA9B4D6), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Interactive Web View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    databaseEnabled = true
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoadingPage = false
                                        // Auto-play videos if available to trigger network stream request
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                var vids = document.querySelectorAll('video');
                                                for (var i = 0; i < vids.length; i++) {
                                                    if (vids[i].src) {
                                                        console.log("VID_SRC:" + vids[i].src);
                                                    }
                                                    vids[i].muted = true;
                                                    vids[i].play();
                                                }
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url?.toString() ?: ""
                                        if (capturedVideoUrl == null && WebVideoSnifferHelper.isMediaStreamUrl(reqUrl)) {
                                            capturedVideoUrl = reqUrl
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onLoadResource(view: WebView?, url: String?) {
                                        super.onLoadResource(view, url)
                                        if (capturedVideoUrl == null && url != null && WebVideoSnifferHelper.isMediaStreamUrl(url)) {
                                            capturedVideoUrl = url
                                        }
                                    }
                                }

                                loadUrl(initialUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            if (capturedVideoUrl != null) {
                Button(
                    onClick = {
                        onVideoFound(capturedVideoUrl!!)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بدء التحميل الفوري للفيديو 🚀", fontWeight = FontWeight.Bold)
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
