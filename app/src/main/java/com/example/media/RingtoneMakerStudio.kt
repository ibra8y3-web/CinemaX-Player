package com.example.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sin

/**
 * Ringtone Maker Studio & Audio Waveform Cutter.
 * Extracts high fidelity lossless audio segments from any audio or video file and saves them to Ringtones.
 */
object RingtoneMakerStudio {

    data class RingtoneResult(
        val success: Boolean,
        val filePath: String? = null,
        val uri: Uri? = null,
        val durationFormatted: String = "",
        val fileSizeFormatted: String = "",
        val errorMessage: String? = null
    )

    /**
     * Generates a normalized waveform amplitude curve (0.1f .. 1.0f) for visual audio editing.
     */
    fun generateWaveformPoints(sampleCount: Int = 40, seedTitle: String = ""): List<Float> {
        val hash = abs(seedTitle.hashCode())
        val points = mutableListOf<Float>()
        for (i in 0 until sampleCount) {
            val freq1 = sin((i + (hash % 10)) * 0.45).toFloat()
            val freq2 = sin((i + (hash % 7)) * 0.9).toFloat() * 0.5f
            val base = (abs(freq1 + freq2) / 1.5f).coerceIn(0.15f, 0.98f)
            points.add(base)
        }
        return points
    }

    suspend fun cutRingtone(
        context: Context,
        sourceUri: Uri,
        ringtoneTitle: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): RingtoneResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var tempFile: File? = null

        try {
            extractor.setDataSource(context, sourceUri, null)

            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext RingtoneResult(
                    success = false,
                    errorMessage = "لم يتم العثور على مسار صوتي صالح في الملف المختار."
                )
            }

            extractor.selectTrack(audioTrackIndex)

            // Setup temporary output file
            val sanitizedTitle = ringtoneTitle.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                .take(30).ifEmpty { "CinemaX_Ringtone" }
            tempFile = File(context.cacheDir, "${sanitizedTitle}_${System.currentTimeMillis()}.m4a")

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            // Seek to start position
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var presentationTimeUsOffset = -1L
            val totalSliceUs = (endUs - startUs).coerceAtLeast(1000L)

            withContext(Dispatchers.Main) {
                onProgress(0.1f, "جاري اقتطاع النغمة بدقة متناهية...")
            }

            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    if (presentationTimeUsOffset < 0L) {
                        presentationTimeUsOffset = sampleTime
                    }

                    bufferInfo.offset = 0
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = sampleTime - presentationTimeUsOffset

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                    val progress = 0.1f + ((sampleTime - startUs).toFloat() / totalSliceUs.toFloat()) * 0.7f
                    withContext(Dispatchers.Main) {
                        onProgress(progress.coerceIn(0.1f, 0.85f), "جاري كتابة النغمة النيرة...")
                    }
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()

            if (!tempFile.exists() || tempFile.length() < 100) {
                return@withContext RingtoneResult(
                    success = false,
                    errorMessage = "فشل إنشاء النغمة، الملف الناتج فارغ."
                )
            }

            withContext(Dispatchers.Main) {
                onProgress(0.9f, "جاري الحفظ في مجلد النغمات بالهاتف (Ringtones)...")
            }

            // Save to public Ringtones folder via MediaStore
            val fileName = "${sanitizedTitle}_Ringtone.m4a"
            val savedUri = saveToRingtones(context, tempFile, fileName, sanitizedTitle)

            val sliceDurationSeconds = ((endMs - startMs) / 1000L).coerceAtLeast(1)
            val formattedDur = String.format("%02d:%02d", sliceDurationSeconds / 60, sliceDurationSeconds % 60)
            val formattedSize = formatBytes(tempFile.length())

            withContext(Dispatchers.Main) {
                onProgress(1.0f, "تم حفظ النغمة بنجاح في مجلد النغمات! 🔔")
            }

            return@withContext RingtoneResult(
                success = true,
                filePath = tempFile.absolutePath,
                uri = savedUri,
                durationFormatted = formattedDur,
                fileSizeFormatted = formattedSize
            )
        } catch (e: Exception) {
            return@withContext RingtoneResult(
                success = false,
                errorMessage = "خطأ أثناء صنع النغمة: ${e.message}"
            )
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun saveToRingtones(context: Context, sourceFile: File, fileName: String, title: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_RINGTONES}/CinemaX_Ringtones")
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
                put(MediaStore.Audio.Media.IS_ALARM, 1)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return uri
            }
        }

        // Direct file fallback
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), "CinemaX_Ringtones")
        if (!dir.exists()) dir.mkdirs()
        val dest = File(dir, fileName)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(dest).use { out ->
                input.copyTo(out)
            }
        }
        return Uri.fromFile(dest)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
    }
}
