package com.example.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * فاحص ومحلل تفاصيل ترميز ومواصفات الفيديو بدقة هندسية عالية
 * يستخرج الكودك الفعلي، الدقة، معدل البت، ومعدل الإطارات وقنوات الصوت.
 */
object VideoMetadataInspector {

    data class VideoTechnicalSpecs(
        val resolution: String,
        val width: Int,
        val height: Int,
        val videoCodec: String,
        val audioCodec: String,
        val frameRate: Float,
        val bitrateKbps: Int,
        val durationFormatted: String,
        val mimeType: String,
        val audioChannels: Int,
        val audioSampleRate: Int
    )

    suspend fun inspectVideo(context: Context, videoUri: Uri): VideoTechnicalSpecs? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()

        try {
            retriever.setDataSource(context, videoUri)
            extractor.setDataSource(context, videoUri, null)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            var videoCodec = "غير محدد"
            var audioCodec = "غير محدد"
            var frameRate = 0f
            var audioChannels = 2
            var audioSampleRate = 44100

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (trackMime.startsWith("video/")) {
                    videoCodec = when {
                        trackMime.contains("avc") || trackMime.contains("h264") -> "AVC / H.264"
                        trackMime.contains("hevc") || trackMime.contains("h265") -> "HEVC / H.265 (4K)"
                        trackMime.contains("vp9") -> "Google VP9"
                        trackMime.contains("av01") -> "AOMedia AV1"
                        trackMime.contains("mp4v") -> "MPEG-4 Visual"
                        else -> trackMime.substringAfter("video/")
                    }
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = try { format.getFloat(MediaFormat.KEY_FRAME_RATE) } catch (_: Exception) {
                            try { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() } catch (_: Exception) { 0f }
                        }
                    }
                } else if (trackMime.startsWith("audio/")) {
                    audioCodec = when {
                        trackMime.contains("mp4a") || trackMime.contains("aac") -> "AAC Stereo / High Quality"
                        trackMime.contains("opus") -> "Opus Low Latency"
                        trackMime.contains("ac3") -> "Dolby Digital (AC-3)"
                        trackMime.contains("eac3") -> "Dolby Digital Plus (E-AC-3)"
                        trackMime.contains("flac") -> "FLAC Lossless Audio"
                        trackMime.contains("mpeg") -> "MP3 / MPEG Audio"
                        else -> trackMime.substringAfter("audio/")
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                }
            }

            extractor.release()
            retriever.release()

            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            val durationFormatted = String.format(Locale.US, "%02d:%02d دقيقة", minutes, seconds)

            val resolutionLabel = when {
                width >= 3840 || height >= 2160 -> "4K Ultra HD ($width × $height)"
                width >= 1920 || height >= 1080 -> "Full HD 1080p ($width × $height)"
                width >= 1280 || height >= 720 -> "HD 720p ($width × $height)"
                width > 0 -> "SD ($width × $height)"
                else -> "دقة قياسية"
            }

            VideoTechnicalSpecs(
                resolution = resolutionLabel,
                width = width,
                height = height,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                frameRate = if (frameRate > 0) frameRate else 30f,
                bitrateKbps = bitrate,
                durationFormatted = durationFormatted,
                mimeType = mime,
                audioChannels = audioChannels,
                audioSampleRate = audioSampleRate
            )
        } catch (_: Exception) {
            try { extractor.release() } catch (_: Exception) {}
            try { retriever.release() } catch (_: Exception) {}
            null
        }
    }
}
