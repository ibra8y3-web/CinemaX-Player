package com.example.live

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Read-only Xtream Codes-compatible IPTV client.
 *
 * Uses player_api.php for account validation and the get_live_categories /
 * get_live_streams actions to retrieve the complete live-TV catalogue.
 */
object XtreamCodesImporter {

    data class ImportResult(
        val channels: List<M3uPlaylistParser.Channel>,
        val accountStatus: String,
        val expiryTimestamp: String?
    )

    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 35_000
    private const val MAX_RESPONSE_BYTES = 32 * 1024 * 1024

    fun fetchLiveChannels(
        server: String,
        username: String,
        password: String,
        outputFormat: String = "ts"
    ): ImportResult {
        require(username.isNotBlank()) { "أدخل اسم مستخدم Xtream Codes." }
        require(password.isNotBlank()) { "أدخل كلمة مرور Xtream Codes." }

        val baseUrl = normalizeServerUrl(server)
        val format = normalizeOutputFormat(outputFormat)

        val accountJson = parseObject(
            requestText(buildApiUrl(baseUrl, username, password, null)),
            "بيانات حساب Xtream"
        )
        val userInfo = accountJson.optJSONObject("user_info")
            ?: throw IllegalStateException(
                accountJson.optString("message").takeIf { it.isNotBlank() }
                    ?: "استجابة Xtream غير صحيحة: لم يتم العثور على بيانات الحساب."
            )

        val auth = userInfo.optString("auth")
        if (auth == "0" || auth.equals("false", ignoreCase = true)) {
            throw IllegalStateException("بيانات Xtream غير صحيحة أو أن الاشتراك غير مفعل.")
        }

        val status = userInfo.optString("status", "Unknown")
        if (status.equals("Disabled", true) ||
            status.equals("Expired", true) ||
            status.equals("Banned", true)
        ) {
            throw IllegalStateException("اشتراك Xtream غير نشط: $status")
        }

        // These two API actions normally return JSON arrays, not JSON objects.
        val categoriesJson = requestText(
            buildApiUrl(baseUrl, username, password, "get_live_categories")
        )
        val streamsJson = requestText(
            buildApiUrl(baseUrl, username, password, "get_live_streams")
        )

        val channels = parseLiveChannels(
            server = baseUrl,
            username = username,
            password = password,
            categoriesJson = categoriesJson,
            streamsJson = streamsJson,
            outputFormat = format
        )
        require(channels.isNotEmpty()) {
            "تم الاتصال بحساب Xtream، لكن الخادم لم يُرجع أي قنوات مباشرة."
        }

        return ImportResult(
            channels = channels,
            accountStatus = status,
            expiryTimestamp = userInfo.optString("exp_date").takeIf {
                it.isNotBlank() && it != "null" && it != "0"
            }
        )
    }

    /** Pure JSON mapping method, also used by unit tests. */
    fun parseLiveChannels(
        server: String,
        username: String,
        password: String,
        categoriesJson: String,
        streamsJson: String,
        outputFormat: String = "ts"
    ): List<M3uPlaylistParser.Channel> {
        val baseUrl = normalizeServerUrl(server)
        val format = normalizeOutputFormat(outputFormat)
        val categories = parseArray(categoriesJson, "تصنيفات القنوات")
        val streams = parseArray(streamsJson, "قنوات Xtream")

        val categoryNames = HashMap<String, String>(categories.length())
        for (index in 0 until categories.length()) {
            val item = categories.optJSONObject(index) ?: continue
            val id = item.optString("category_id").takeIf { it.isNotBlank() && it != "null" }
                ?: continue
            val name = item.optString("category_name").takeIf { it.isNotBlank() && it != "null" }
                ?: continue
            categoryNames[id] = name
        }

        val channels = ArrayList<M3uPlaylistParser.Channel>(streams.length())
        val seenUrls = HashSet<String>()

        for (index in 0 until streams.length()) {
            val item = streams.optJSONObject(index) ?: continue
            val streamId = item.optString("stream_id").takeIf {
                it.isNotBlank() && it != "null" && it != "0"
            } ?: continue

            val title = item.optString("name").takeIf {
                it.isNotBlank() && it != "null"
            } ?: "قناة Xtream $streamId"

            val categoryId = item.optString("category_id")
            val category = categoryNames[categoryId]
                ?: item.optString("category_name").takeIf { it.isNotBlank() && it != "null" }
                ?: "Xtream · بث مباشر"

            val directSource = item.optString("direct_source")
            val streamUrl = if (isHttpUrl(directSource)) {
                directSource
            } else {
                "$baseUrl/live/${encodePath(username)}/${encodePath(password)}/${encodePath(streamId)}.$format"
            }

            if (!seenUrls.add(streamUrl)) continue

            val logoUrl = item.optString("stream_icon").takeIf(::isHttpUrl)
            val epgId = item.optString("epg_channel_id").takeIf {
                it.isNotBlank() && it != "null"
            }

            channels.add(
                M3uPlaylistParser.Channel(
                    id = M3uPlaylistParser.stableIdForUrl(streamUrl),
                    title = title,
                    url = streamUrl,
                    category = category,
                    tvgId = epgId,
                    logoUrl = logoUrl
                )
            )
        }

        return channels
    }

    private fun normalizeOutputFormat(value: String): String {
        val format = value.lowercase(Locale.ROOT).removePrefix(".")
        require(format == "ts" || format == "m3u8") {
            "صيغة بث Xtream يجب أن تكون TS أو M3U8."
        }
        return format
    }

    private fun buildApiUrl(
        baseUrl: String,
        username: String,
        password: String,
        action: String?
    ): String {
        val query = buildList {
            add("username=${encodeQuery(username)}")
            add("password=${encodeQuery(password)}")
            if (!action.isNullOrBlank()) add("action=${encodeQuery(action)}")
        }.joinToString("&")
        return "$baseUrl/player_api.php?$query"
    }

    private fun requestText(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            val active = URL(url).openConnection() as HttpURLConnection
            connection = active
            active.apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "CinemaX-Player/3.0 (Xtream IPTV)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = active.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "خادم Xtream أعاد HTTP $responseCode. تحقق من عنوان الخادم وبيانات الدخول."
                )
            }

            val output = active.inputStream.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_RESPONSE_BYTES) {
                        "استجابة خادم Xtream أكبر من الحد المسموح (32 ميجابايت)."
                    }
                    result.write(buffer, 0, count)
                }
                String(result.toByteArray(), Charsets.UTF_8)
            }
            require(output.isNotBlank()) { "خادم Xtream أعاد استجابة فارغة." }
            return output
        } catch (error: Exception) {
            if (error is IllegalStateException) throw error
            throw IllegalStateException(
                "تعذر الاتصال بخادم Xtream: ${error.localizedMessage ?: "خطأ شبكة"}",
                error
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseObject(raw: String, label: String): JSONObject {
        return try {
            JSONObject(raw).also { value ->
                if (value.has("error") && !value.isNull("error")) {
                    throw IllegalStateException(value.optString("error").take(300))
                }
            }
        } catch (error: Exception) {
            if (error is IllegalStateException) throw error
            throw IllegalStateException("استجابة $label من Xtream غير صالحة.", error)
        }
    }

    private fun parseArray(raw: String, label: String): JSONArray {
        return try {
            val value = raw.trim()
            when {
                value.startsWith("[") -> JSONArray(value)
                value.startsWith("{") -> {
                    val obj = JSONObject(value)
                    obj.optJSONArray("data")
                        ?: obj.optJSONArray("results")
                        ?: throw IllegalStateException("لم يتم العثور على $label في استجابة Xtream.")
                }
                else -> throw IllegalStateException("استجابة $label من Xtream غير صالحة.")
            }
        } catch (error: Exception) {
            if (error is IllegalStateException) throw error
            throw IllegalStateException("تعذر تحليل $label من خادم Xtream.", error)
        }
    }

    private fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "أدخل عنوان خادم Xtream Codes." }
        val base = when {
            trimmed.endsWith("/player_api.php", true) ->
                trimmed.dropLast("/player_api.php".length).trimEnd('/')
            trimmed.endsWith("/get.php", true) ->
                trimmed.dropLast("/get.php".length).trimEnd('/')
            trimmed.endsWith("/playlist.php", true) ->
                trimmed.dropLast("/playlist.php".length).trimEnd('/')
            else -> trimmed
        }
        val parsed = runCatching { URL(base) }.getOrElse {
            throw IllegalArgumentException("عنوان خادم Xtream غير صالح.")
        }
        require(
            (parsed.protocol.equals("http", true) || parsed.protocol.equals("https", true)) &&
                parsed.host.isNotBlank() && parsed.query.isNullOrBlank()
        ) {
            "أدخل عنوان الخادم فقط، مثل https://provider.example:8080"
        }
        return base
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val parsed = URL(value)
        (parsed.protocol.equals("https", true) || parsed.protocol.equals("http", true)) &&
            parsed.host.isNotBlank()
    }.getOrDefault(false)

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun encodePath(value: String): String =
        encodeQuery(value).replace("+", "%20")
}
