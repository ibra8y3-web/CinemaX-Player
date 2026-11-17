package com.example.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/**
 * Multi-site media extractor for public, non-DRM media sources supported by yt-dlp.
 *
 * This engine complements RealMediaDownloader:
 * - RealMediaDownloader handles direct files and ordinary multi-link file queues.
 * - UniversalMediaEngine resolves supported website pages and can download media
 *   or extract audio using yt-dlp + FFmpeg + aria2c.
 *
 * It does not bypass account access controls, paywalls, DRM, or platform restrictions.
 */
object UniversalMediaEngine {

    data class FormatOption(
        val formatId: String,
        val extension: String,
        val note: String,
        val width: Int,
        val height: Int,
        val hasVideo: Boolean,
        val hasAudio: Boolean,
        val approximateSizeBytes: Long
    ) {
        val qualityLabel: String
            get() = when {
                height > 0 -> "${height}p · ${extension.uppercase()}"
                hasAudio && !hasVideo -> "صوت · ${extension.uppercase()}"
                else -> note.ifBlank { extension.uppercase() }
            }
    }

    data class MediaInfo(
        val title: String,
        val extractor: String,
        val durationSeconds: Long,
        val thumbnailUrl: String?,
        val webpageUrl: String,
        val formats: List<FormatOption>
    )

    data class DownloadedFile(
        val displayName: String,
        val sizeBytes: Long,
        val savedUri: String,
        val mimeType: String
    )

    data class Progress(
        val percent: Int,
        val etaSeconds: Long,
        val message: String
    )

    private const val OUTPUT_DIRECTORY = "cinemax-ytdlp"
    @Volatile private var initialized = false

    @Synchronized
    private fun ensureInitialized(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        try {
            YoutubeDL.getInstance().init(appContext)
            FFmpeg.getInstance().init(appContext)
            Aria2c.getInstance().init(appContext)
            initialized = true
        } catch (error: Exception) {
            initialized = false
            throw IllegalStateException(
                "تعذر تهيئة محركات استخراج المواقع. تحقق من توافق معمارية الهاتف ومساحة التخزين.",
                error
            )
        }
    }

    suspend fun inspect(context: Context, url: String): MediaInfo = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        requireHttpUrl(url)
        val info = YoutubeDL.getInstance().getInfo(url)
        val formats = info.formats.orEmpty()
            .filter { !it.formatId.isNullOrBlank() && !it.ext.isNullOrBlank() }
            .distinctBy { it.formatId }
            .sortedWith(
                compareByDescending<VideoFormat> { it.height }
                    .thenByDescending { it.hasVideoCodec() }
                    .thenByDescending { it.tbr }
            )
            .take(30)
            .map { format ->
                FormatOption(
                    formatId = format.formatId.orEmpty(),
                    extension = format.ext.orEmpty(),
                    note = format.formatNote.orEmpty(),
                    width = format.width,
                    height = format.height,
                    hasVideo = !format.vcodec.isNullOrBlank() && format.vcodec != "none",
                    hasAudio = !format.acodec.isNullOrBlank() && format.acodec != "none",
                    approximateSizeBytes = format.fileSize.takeIf { it > 0L }
                        ?: format.fileSizeApproximate
                )
            }

        MediaInfo(
            title = info.fulltitle?.takeIf { it.isNotBlank() }
                ?: info.title?.takeIf { it.isNotBlank() }
                ?: url,
            extractor = info.extractor?.takeIf { it.isNotBlank() } ?: "مستخرج عام",
            durationSeconds = info.duration.toLong().coerceAtLeast(0L),
            thumbnailUrl = info.thumbnail,
            webpageUrl = info.webpageUrl?.takeIf { it.isNotBlank() } ?: url,
            formats = formats
        )
    }

    /**
     * Downloads one or more supported URLs. Multiple URLs and supported public
     * playlists are processed by yt-dlp as one queue. Files are published to
     * Downloads/CinemaX on Android 10+, or app-scoped Downloads on older Android.
     */
    suspend fun download(
        context: Context,
        urls: List<String>,
        audioOnly: Boolean = false,
        formatId: String? = null,
        processId: String = UUID.randomUUID().toString(),
        onProgress: (Progress) -> Unit = {}
    ): List<DownloadedFile> = withContext(Dispatchers.IO) {
        require(urls.isNotEmpty()) { "أدخل رابطاً واحداً على الأقل." }
        urls.forEach(::requireHttpUrl)
        ensureInitialized(context)

        val appContext = context.applicationContext
        val workDirectory = File(appContext.cacheDir, "$OUTPUT_DIRECTORY/$processId")
        if (workDirectory.exists()) workDirectory.deleteRecursively()
        require(workDirectory.mkdirs() || workDirectory.isDirectory) {
            "تعذر إنشاء مجلد مؤقت لمحرك التنزيل."
        }

        val request = YoutubeDLRequest(urls)
        request.addOption("--no-warnings")
        request.addOption("--no-mtime")
        request.addOption("--newline")
        request.addOption("--continue")
        request.addOption("--downloader", "libaria2c.so")
        request.addOption("--downloader-args", "aria2c:-x16 -s16 -k1M --summary-interval=1")
        request.addOption(
            "-o",
            File(workDirectory, "%(title).160B [%(id)s].%(ext)s").absolutePath
        )

        if (audioOnly) {
            request.addOption("-x")
            request.addOption("--audio-format", "mp3")
            request.addOption("--audio-quality", "0")
        } else {
            val selected = formatId?.takeIf { it.isNotBlank() }
            if (selected != null) {
                // A video-only format is paired with the best available audio stream.
                val option = if (selected.contains("+")) selected else selected
                request.addOption("-f", option)
            } else {
                request.addOption("-f", "bv*+ba/best")
            }
            request.addOption("--merge-output-format", "mkv")
        }

        val startedAt = System.currentTimeMillis()
        try {
            val response = YoutubeDL.getInstance().execute(request, processId) { percent, eta, line ->
                runCatching {
                    onProgress(
                        Progress(
                            percent = percent.toInt().coerceIn(0, 100),
                            etaSeconds = eta.coerceAtLeast(0L),
                            message = line.take(500)
                        )
                    )
                }
            }

            val completedFiles = workDirectory.walkTopDown()
                .filter { file ->
                    file.isFile &&
                        file.lastModified() >= startedAt - 2_000L &&
                        !file.name.endsWith(".part", ignoreCase = true) &&
                        !file.name.endsWith(".ytdl", ignoreCase = true) &&
                        file.extension.lowercase() !in setOf("json", "description", "jpg", "jpeg", "png", "webp", "vtt", "srt", "ass")
                }
                .sortedBy { it.lastModified() }
                .toList()

            require(completedFiles.isNotEmpty()) {
                response.err.takeIf { it.isNotBlank() }
                    ?: response.out.takeIf { it.isNotBlank() }
                    ?: "انتهى محرك المواقع دون إنشاء ملف قابل للحفظ."
            }

            completedFiles.map { file ->
                publishFile(appContext, file)
            }
        } catch (error: Exception) {
            throw IllegalStateException(
                error.message?.take(700) ?: "تعذر استخراج أو تنزيل الوسائط من هذا الرابط.",
                error
            )
        }
    }

    fun stop(processId: String): Boolean = runCatching {
        YoutubeDL.getInstance().destroyProcessById(processId)
    }.getOrDefault(false)

    suspend fun updateExtractor(context: Context): String = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val status = YoutubeDL.getInstance().updateYoutubeDL(
            context.applicationContext,
            YoutubeDL.UpdateChannel.STABLE
        )
        status?.name ?: "UP_TO_DATE"
    }

    private fun publishFile(context: Context, source: File): DownloadedFile {
        val extension = source.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "mkv" -> "video/x-matroska"
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                else -> "application/octet-stream"
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/CinemaX"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri: Uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("تعذر إنشاء ملف في مجلد Downloads/CinemaX.")

            try {
                context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    FileInputStream(source).use { input -> input.copyTo(output, bufferSize = 1024 * 1024) }
                } ?: throw IllegalStateException("تعذر فتح ملف الوجهة لحفظ التنزيل.")
                val completed = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, completed, null, null)
            } catch (error: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
            val size = source.length()
            source.delete()
            return DownloadedFile(source.name, size, uri.toString(), mimeType)
        }

        val fallbackDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "CinemaX"
        )
        require(fallbackDirectory.exists() || fallbackDirectory.mkdirs()) {
            "تعذر إنشاء مجلد التنزيل على هذا الجهاز."
        }
        val target = File(fallbackDirectory, source.name)
        source.copyTo(target, overwrite = true)
        val size = target.length()
        source.delete()
        return DownloadedFile(target.name, size, target.absolutePath, mimeType)
    }

    private fun requireHttpUrl(value: String) {
        val uri = Uri.parse(value.trim())
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "محرك المواقع يقبل روابط HTTP أو HTTPS فقط."
        }
        require(!uri.host.isNullOrBlank()) { "الرابط غير صالح أو لا يحتوي على اسم موقع." }
    }

    private fun VideoFormat.hasVideoCodec(): Boolean =
        !vcodec.isNullOrBlank() && vcodec != "none"
}
