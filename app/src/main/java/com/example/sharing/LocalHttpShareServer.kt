package com.example.sharing

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.media.model.LocalVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections

class LocalHttpShareServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl

    private var videoListProvider: () -> List<LocalVideo> = { emptyList() }

    fun setVideosProvider(provider: () -> List<LocalVideo>) {
        this.videoListProvider = provider
    }

    fun start(port: Int = 8080) {
        if (_isRunning.value) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val ip = getDeviceIpAddress()
                val url = "http://$ip:$port"
                _serverUrl.value = url
                _isRunning.value = true

                while (_isRunning.value && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        _serverUrl.value = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val tokens = requestLine.split(" ")
            if (tokens.size < 2) return@withContext

            val method = tokens[0]
            val rawPath = tokens[1]
            val path = URLDecoder.decode(rawPath, "UTF-8")

            var rangeHeader: String? = null
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substringAfter(":").trim()
                }
                line = reader.readLine()
            }

            if (method.equals("GET", ignoreCase = true)) {
                if (path == "/" || path.startsWith("/index")) {
                    serveHtmlIndex(out)
                } else if (path.startsWith("/stream/")) {
                    val videoIdStr = path.substringAfter("/stream/").substringBefore("?")
                    val videoId = videoIdStr.toLongOrNull()
                    val video = videoListProvider().find { it.id == videoId }
                    if (video != null && video.path.isNotEmpty() && File(video.path).exists()) {
                        serveVideoFile(File(video.path), out, rangeHeader)
                    } else {
                        sendNotFound(out)
                    }
                } else {
                    sendNotFound(out)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun serveHtmlIndex(out: OutputStream) {
        val videos = videoListProvider()
        val html = buildString {
            append("""
                <!DOCTYPE html>
                <html lang="ar" dir="rtl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>CinemaX Player - المشاركة المحلية</title>
                    <style>
                        body {
                            background-color: #0d1117;
                            color: #c9d1d9;
                            font-family: system-ui, -apple-system, sans-serif;
                            margin: 0;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #1f2937, #111827);
                            padding: 24px;
                            border-radius: 16px;
                            margin-bottom: 24px;
                            border: 1px solid #374151;
                            text-align: center;
                        }
                        h1 { color: #00E5FF; margin: 0 0 8px 0; font-size: 26px; }
                        p { color: #9ca3af; margin: 0; font-size: 14px; }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                            gap: 16px;
                        }
                        .card {
                            background: #161b22;
                            border: 1px solid #30363d;
                            border-radius: 12px;
                            padding: 16px;
                            display: flex;
                            flex-direction: column;
                            justify-content: space-between;
                        }
                        .card h3 {
                            margin: 0 0 8px 0;
                            font-size: 16px;
                            color: #f0f6fc;
                            word-break: break-all;
                        }
                        .meta {
                            font-size: 12px;
                            color: #8b949e;
                            margin-bottom: 12px;
                        }
                        .badge {
                            display: inline-block;
                            padding: 2px 8px;
                            border-radius: 6px;
                            font-size: 11px;
                            background: #238636;
                            color: #fff;
                            margin-right: 6px;
                        }
                        .btn-group {
                            display: flex;
                            gap: 8px;
                        }
                        .btn {
                            flex: 1;
                            padding: 8px 12px;
                            text-align: center;
                            text-decoration: none;
                            border-radius: 8px;
                            font-size: 13px;
                            font-weight: bold;
                        }
                        .btn-stream {
                            background: #00E5FF;
                            color: #0b0f19;
                        }
                        .btn-download {
                            background: #21262d;
                            color: #c9d1d9;
                            border: 1px solid #30363d;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>✦ CinemaX Video Player</h1>
                        <p>بث وتحميل الفيديوهات عبر الشبكة المحلية (Wi-Fi) مباشرة وبسرعة فائقة</p>
                    </div>
                    <div class="grid">
            """.trimIndent())

            if (videos.isEmpty()) {
                append("<p style='text-align:center;grid-column:1/-1;'>لا توجد فيديوهات متاحة للمشاركة حالياً.</p>")
            } else {
                for (v in videos) {
                    val streamUrl = "/stream/${v.id}"
                    append("""
                        <div class="card">
                            <div>
                                <h3>${escapeHtml(v.title)}</h3>
                                <div class="meta">
                                    <span class="badge">${escapeHtml(v.aiCategory)}</span>
                                    <span>${v.resolution}</span> • 
                                    <span>${v.folderName}</span>
                                </div>
                            </div>
                            <div class="btn-group">
                                <a href="$streamUrl" target="_blank" class="btn btn-stream">▶ تشغيل مباشر</a>
                                <a href="$streamUrl" download="${escapeHtml(v.displayName)}" class="btn btn-download">⤓ تحميل</a>
                            </div>
                        </div>
                    """.trimIndent())
                }
            }

            append("""
                    </div>
                </body>
                </html>
            """.trimIndent())
        }

        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun serveVideoFile(file: File, out: OutputStream, rangeHeader: String?) {
        val fileLength = file.length()
        var start = 0L
        var end = fileLength - 1

        val isRange = rangeHeader != null && rangeHeader.startsWith("bytes=")
        if (isRange) {
            val rangeVal = rangeHeader!!.substringAfter("bytes=").trim()
            val parts = rangeVal.split("-")
            val pStart = parts.getOrNull(0)?.toLongOrNull()
            val pEnd = parts.getOrNull(1)?.toLongOrNull()

            if (pStart != null) {
                start = pStart
                if (pEnd != null && pEnd >= pStart) {
                    end = pEnd.coerceAtMost(fileLength - 1)
                }
            } else if (pEnd != null) {
                start = (fileLength - pEnd).coerceAtLeast(0L)
            }
        }

        val contentLength = (end - start + 1).coerceAtLeast(0L)

        val header = if (isRange) {
            "HTTP/1.1 206 Partial Content\r\n" +
                    "Content-Type: video/mp4\r\n" +
                    "Content-Range: bytes $start-$end/$fileLength\r\n" +
                    "Content-Length: $contentLength\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: keep-alive\r\n\r\n"
        } else {
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: video/mp4\r\n" +
                    "Content-Length: $fileLength\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: keep-alive\r\n\r\n"
        }

        out.write(header.toByteArray(Charsets.UTF_8))

        java.io.RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val buffer = ByteArray(64 * 1024)
            var remaining = contentLength
            while (remaining > 0) {
                val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                val read = raf.read(buffer, 0, toRead)
                if (read == -1) break
                out.write(buffer, 0, read)
                remaining -= read
            }
            out.flush()
        }
    }

    private fun sendNotFound(out: OutputStream) {
        val msg = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        out.write(msg.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (hostAddress.indexOf(':') < 0) { // IPv4
                            return hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return "127.0.0.1"
    }
}
