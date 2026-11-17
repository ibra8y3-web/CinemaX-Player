package com.example.live

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * Parses IPTV M3U channel lists. HLS media manifests are intentionally rejected:
 * an HLS video manifest is one stream, not a list of television channels.
 */
object M3uPlaylistParser {
    data class Channel(
        val id: String,
        val title: String,
        val url: String,
        val category: String,
        val tvgId: String? = null,
        val logoUrl: String? = null
    )

    data class LoadedPlaylist(val content: String, val sourceUrl: String? = null)

    private const val MAX_PLAYLIST_BYTES = 30 * 1024 * 1024
    private const val MAX_CHANNELS = 50_000
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 35_000
    private const val DEFAULT_CATEGORY = "بث M3U المستورد"

    /** Accepts raw M3U text or one HTTP(S) URL pointing to an M3U file. */
    fun loadInput(input: String): LoadedPlaylist {
        val trimmed = input.trim().removePrefix("\uFEFF")
        require(trimmed.isNotBlank()) { "أدخل رابط قائمة M3U أو محتوى القائمة." }
        return if (isSingleHttpUrl(trimmed)) fetchPlaylist(trimmed) else LoadedPlaylist(trimmed)
    }

    @Throws(Exception::class)
    fun fetchPlaylist(rawUrl: String): LoadedPlaylist {
        val parsedUrl = URL(rawUrl.trim())
        require(parsedUrl.protocol.equals("http", true) || parsedUrl.protocol.equals("https", true)) {
            "رابط القائمة يجب أن يبدأ بـ http:// أو https://"
        }

        var connection: HttpURLConnection? = null
        try {
            val active = parsedUrl.openConnection() as HttpURLConnection
            connection = active
            active.apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "CinemaX-Player/2.0 (M3U IPTV importer)")
                setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*")
            }
            val responseCode = active.responseCode
            require(responseCode in 200..299) {
                "تعذر تنزيل قائمة M3U. استجابة الخادم: HTTP $responseCode"
            }
            val contentLength = active.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            require(contentLength <= MAX_PLAYLIST_BYTES) {
                "حجم قائمة M3U أكبر من الحد المسموح (30 ميجابايت)."
            }

            val output = ByteArrayOutputStream()
            active.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_PLAYLIST_BYTES) {
                        "تجاوزت قائمة M3U الحد المسموح (30 ميجابايت)."
                    }
                    output.write(buffer, 0, count)
                }
            }
            val content = output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
            require(content.contains("#EXTM3U", ignoreCase = true) ||
                content.contains("#EXTINF:", ignoreCase = true)
            ) {
                "الرابط لا يعيد قائمة M3U صالحة. تأكد أنك وضعت رابط ملف القائمة وليس رابط قناة واحدة."
            }
            return LoadedPlaylist(content, active.url.toString())
        } finally {
            connection?.disconnect()
        }
    }

    fun parse(
        rawContent: String,
        playlistUrl: String? = null,
        fallbackCategory: String = DEFAULT_CATEGORY
    ): List<Channel> {
        val content = rawContent.removePrefix("\uFEFF")
        require(content.isNotBlank()) { "ملف القائمة فارغ." }
        val lines = content.lineSequence().map { it.trim().removePrefix("\uFEFF") }.toList()

        val hasM3uHeader = lines.any {
            it.equals("#EXTM3U", ignoreCase = true) || it.startsWith("#EXTM3U ", ignoreCase = true)
        }
        val hasExtInf = lines.any { it.startsWith("#EXTINF:", ignoreCase = true) }
        require(hasM3uHeader || hasExtInf || lines.any { isSupportedStreamUrl(it) }) {
            "المحتوى ليس قائمة M3U معروفة. استخدم ملفاً يبدأ بـ #EXTM3U أو #EXTINF."
        }

        val hlsTags = listOf(
            "#EXT-X-TARGETDURATION",
            "#EXT-X-MEDIA-SEQUENCE",
            "#EXT-X-STREAM-INF",
            "#EXT-X-KEY",
            "#EXT-X-MAP"
        )
        require(lines.none { line -> hlsTags.any { line.startsWith(it, ignoreCase = true) } }) {
            "هذا رابط قائمة HLS لفيديو واحد، وليس قائمة قنوات IPTV. أضف رابط قائمة القنوات بصيغة M3U."
        }

        data class PendingInfo(
            val title: String,
            val group: String,
            val tvgId: String?,
            val logoUrl: String?
        )

        var pending: PendingInfo? = null
        var currentGroup = fallbackCategory.ifBlank { DEFAULT_CATEGORY }
        val channels = ArrayList<Channel>()
        val seenUrls = HashSet<String>()

        for (line in lines) {
            if (line.isBlank()) continue
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val comma = findFirstUnquotedComma(line)
                    val metadata = if (comma >= 0) line.substring(0, comma) else line
                    val displayName = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    val group = attribute(metadata, "group-title")
                        ?.takeIf { it.isNotBlank() } ?: currentGroup
                    val title = displayName
                        .ifBlank { attribute(metadata, "tvg-name").orEmpty() }
                        .ifBlank { "قناة ${channels.size + 1}" }
                    currentGroup = group
                    pending = PendingInfo(title, group, attribute(metadata, "tvg-id"), attribute(metadata, "tvg-logo"))
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val group = line.substringAfter(':').trim()
                    if (group.isNotBlank()) {
                        currentGroup = group
                        pending = pending?.copy(group = group)
                    }
                }

                line.startsWith("#") -> Unit

                else -> {
                    val streamUrl = resolveStreamUrl(line, playlistUrl) ?: continue
                    if (seenUrls.add(streamUrl)) {
                        val info = pending
                        val title = info?.title?.takeIf { it.isNotBlank() }
                            ?: displayNameFromUrl(streamUrl, channels.size + 1)
                        val group = info?.group?.takeIf { it.isNotBlank() } ?: currentGroup
                        channels.add(
                            Channel(
                                id = stableId(streamUrl),
                                title = title,
                                url = streamUrl,
                                category = group,
                                tvgId = info?.tvgId,
                                logoUrl = info?.logoUrl
                            )
                        )
                        require(channels.size <= MAX_CHANNELS) {
                            "قائمة القنوات تجاوزت الحد الأقصى (50,000 قناة)."
                        }
                    }
                    pending = null
                    currentGroup = fallbackCategory.ifBlank { DEFAULT_CATEGORY }
                }
            }
        }

        require(channels.isNotEmpty()) {
            "لم يتم العثور على روابط قنوات داخل القائمة. تأكد من أن الملف يحتوي على #EXTINF متبوعاً برابط لكل قناة."
        }
        return channels
    }

    private fun isSingleHttpUrl(value: String): Boolean {
        if (value.contains('\n') || value.contains('\r') ||
            value.contains("#EXTM3U", true) || value.contains("#EXTINF:", true)
        ) return false
        return runCatching {
            val url = URL(value)
            (url.protocol.equals("http", true) || url.protocol.equals("https", true)) && url.host.isNotBlank()
        }.getOrDefault(false)
    }

    private fun isSupportedStreamUrl(value: String): Boolean = resolveStreamUrl(value, null) != null

    private fun resolveStreamUrl(raw: String, playlistUrl: String?): String? {
        val value = raw.trim()
        if (value.isBlank()) return null
        return runCatching {
            val hasScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(value)
            val resolved = if (hasScheme) URL(value) else {
                val base = playlistUrl?.let { URL(it) } ?: return null
                URL(base, value)
            }
            val scheme = resolved.protocol.lowercase(Locale.ROOT)
            if (scheme !in setOf("http", "https", "rtsp") || resolved.host.isBlank()) return null
            resolved.toString()
        }.getOrNull()
    }

    private fun findFirstUnquotedComma(line: String): Int {
        var quoted = false
        for (index in line.indices) {
            when (line[index]) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return index
            }
        }
        return -1
    }

    private fun attribute(line: String, name: String): String? {
        val key = "$name="
        val index = line.indexOf(key, ignoreCase = true)
        if (index < 0) return null
        val valueStart = index + key.length
        if (valueStart >= line.length) return null
        val quote = line[valueStart]
        val value = if (quote == '"') {
            val valueEnd = line.indexOf('"', valueStart + 1)
            if (valueEnd < 0) return null
            line.substring(valueStart + 1, valueEnd)
        } else {
            line.substring(valueStart).takeWhile { !it.isWhitespace() && it != ',' }
        }
        return value.trim().takeIf { it.isNotBlank() }
    }

    private fun displayNameFromUrl(value: String, index: Int): String {
        val url = runCatching { URL(value) }.getOrNull()
        val segment = url?.path?.substringAfterLast('/')?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
        return segment ?: url?.host ?: "قناة $index"
    }

    private fun stableId(url: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        val hex = bytes.take(12).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "m3u_$hex"
    }
}
