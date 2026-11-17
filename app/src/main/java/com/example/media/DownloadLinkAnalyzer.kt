package com.example.media

import android.net.Uri
import java.util.Locale

/**
 * Classifies user-provided links before the app chooses a download strategy.
 *
 * This is a routing layer, not a promise that every platform exposes a downloadable
 * asset. Protected or private content remains unavailable unless the platform grants access.
 */
object DownloadLinkAnalyzer {

    enum class Route {
        DIRECT_FILE, SOCIAL_PAGE, WEB_PAGE, HLS_MANIFEST, DASH_MANIFEST, INVALID
    }

    enum class Provider(val displayName: String) {
        TELEGRAM("Telegram"),
        FACEBOOK("Facebook"),
        INSTAGRAM("Instagram"),
        TIKTOK("TikTok"),
        YOUTUBE("YouTube"),
        X("X / Twitter"),
        REDDIT("Reddit"),
        SNAPCHAT("Snapchat"),
        PINTEREST("Pinterest"),
        TWITCH("Twitch"),
        VIMEO("Vimeo"),
        DAILYMOTION("Dailymotion"),
        RUMBLE("Rumble"),
        STREAMABLE("Streamable"),
        SOUNDCLOUD("SoundCloud"),
        MIXCLOUD("Mixcloud"),
        THREADS("Threads"),
        LINKEDIN("LinkedIn")
    }

    data class Analysis(
        val input: String,
        val normalizedUrl: String,
        val host: String?,
        val provider: Provider?,
        val route: Route,
        val extension: String?,
        val advice: String
    )

    private val downloadableExtensions = setOf(
        "mp4", "m4v", "webm", "mov", "mkv", "3gp", "avi", "flv", "mpeg", "mpg", "ts",
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma", "aiff",
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg", "avif",
        "pdf", "txt", "rtf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml",
        "epub", "mobi", "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "apk", "aab", "obb", "iso",
        "bin", "dmg", "exe", "msi", "torrent", "ics", "vcf"
    )

    fun normalizeUrl(raw: String): String {
        var value = raw.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (value.startsWith("//")) return "https:$value"

        val hasScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(value)
        if (!hasScheme) {
            val hostCandidate = value.substringBefore('/').substringBefore('?').substringBefore('#')
            if (hostCandidate.startsWith("www.", ignoreCase = true) ||
                (hostCandidate.contains('.') && !hostCandidate.startsWith('.'))
            ) {
                value = "https://$value"
            }
        }
        return value
    }

    fun extractUrls(text: String): List<String> {
        val regex = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        return regex.findAll(text)
            .map { it.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}') }
            .map(::normalizeUrl)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    fun extractFirstUrl(text: String?): String? =
        text?.let(::extractUrls)?.firstOrNull()

    fun isDownloadableExtension(extension: String?): Boolean =
        extension?.lowercase(Locale.ROOT) in downloadableExtensions

    fun analyze(raw: String): Analysis {
        val normalized = normalizeUrl(raw)
        val uri = runCatching { Uri.parse(normalized) }.getOrNull()
        val scheme = uri?.scheme?.lowercase(Locale.ROOT)
        val host = uri?.host?.lowercase(Locale.ROOT)?.trimEnd('.')
        if (normalized.isBlank() || scheme !in setOf("http", "https") || host.isNullOrBlank()) {
            return Analysis(
                input = raw, normalizedUrl = normalized, host = host, provider = null,
                route = Route.INVALID, extension = null,
                advice = "الرابط غير صالح. أرسل رابط HTTP أو HTTPS يبدأ بـ https:// أو http://"
            )
        }

        val path = uri.path.orEmpty()
        val extension = path.substringAfterLast('.', "")
            .takeIf { '.' in path && it.length in 1..10 }
            ?.lowercase(Locale.ROOT)
        val provider = providerFor(host)
        val route = when (extension) {
            "m3u8" -> Route.HLS_MANIFEST
            "mpd" -> Route.DASH_MANIFEST
            else -> when {
                isDownloadableExtension(extension) -> Route.DIRECT_FILE
                provider != null -> Route.SOCIAL_PAGE
                else -> Route.WEB_PAGE
            }
        }

        val advice = when (route) {
            Route.DIRECT_FILE ->
                "يبدو أن الرابط يشير إلى ملف قابل للتنزيل. سيفحص CinemaX نوع المحتوى والحجم قبل بدء التحميل."
            Route.SOCIAL_PAGE -> when (provider) {
                Provider.YOUTUBE ->
                    "هذا رابط صفحة YouTube وليس ملفاً مباشراً. لا يستخرج CinemaX روابط YouTube الخاصة ولا يتجاوز DRM أو قيود المنصة؛ استخدم طرق التنزيل الرسمية أو رابط ملف يتيحه مالك المحتوى."
                Provider.TELEGRAM ->
                    "هذا رابط Telegram. افتح المنشور إذا كان متاحاً، أو استخدم مشاركة الملف من Telegram إلى CinemaX لتنزيل نسخة أرسلها لك التطبيق."
                else ->
                    "هذا رابط صفحة ${provider?.displayName ?: "موقع ويب"} وليس رابط ملف مباشر. يمكن للمستكشف التقاط ملف مباشر إذا أتاحه الموقع للمتصفح."
            }
            Route.WEB_PAGE ->
                "هذا رابط صفحة ويب. سيفحص CinemaX الاستجابة أولاً، وإذا كانت صفحة HTML فسيتيح فتحها في المستكشف بدلاً من حفظ صفحة ويب كأنها فيديو."
            Route.HLS_MANIFEST ->
                "هذا رابط قائمة بث HLS (M3U8). يمكن تشغيله عبر مشغّل الوسائط، لكن تنزيل وتجميع مقاطعه يحتاج إلى مصدر يسمح بذلك ومحرك بث مخصص."
            Route.DASH_MANIFEST ->
                "هذا رابط قائمة MPEG-DASH (MPD). يمكن تشغيله عبر مشغّل الوسائط، لكن تنزيل وتجميع المقاطع يحتاج إلى مصدر يسمح بذلك ومحرك بث مخصص."
            Route.INVALID -> "الرابط غير صالح."
        }

        return Analysis(
            input = raw, normalizedUrl = normalized, host = host, provider = provider,
            route = route, extension = extension, advice = advice
        )
    }

    private fun providerFor(host: String): Provider? {
        fun matches(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            matches("t.me") || matches("telegram.me") || matches("telegram.dog") -> Provider.TELEGRAM
            matches("facebook.com") || matches("fb.watch") || matches("fb.com") -> Provider.FACEBOOK
            matches("instagram.com") -> Provider.INSTAGRAM
            matches("tiktok.com") || matches("vm.tiktok.com") -> Provider.TIKTOK
            matches("youtube.com") || matches("youtu.be") || matches("youtube-nocookie.com") -> Provider.YOUTUBE
            matches("x.com") || matches("twitter.com") -> Provider.X
            matches("reddit.com") || matches("redd.it") -> Provider.REDDIT
            matches("snapchat.com") -> Provider.SNAPCHAT
            matches("pinterest.com") || matches("pin.it") -> Provider.PINTEREST
            matches("twitch.tv") -> Provider.TWITCH
            matches("vimeo.com") -> Provider.VIMEO
            matches("dailymotion.com") || matches("dai.ly") -> Provider.DAILYMOTION
            matches("rumble.com") -> Provider.RUMBLE
            matches("streamable.com") -> Provider.STREAMABLE
            matches("soundcloud.com") -> Provider.SOUNDCLOUD
            matches("mixcloud.com") -> Provider.MIXCLOUD
            matches("threads.net") -> Provider.THREADS
            matches("linkedin.com") -> Provider.LINKEDIN
            else -> null
        }
    }
}
