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

/**
 * Lossless Video Mute & Audio Remover Studio.
 * Extracts video frames without audio tracks in seconds without re-encoding loss.
 */
object VideoMuteAudioRemover {

    data class MuteResult(
        val success: Boolean,
        val filePath: String? = null,
        val uri: Uri? = null,
        val durationFormatted: String = "",
        val fileSizeFormatted: String = "",
        val errorMessage: String? = null
    )

    suspend fun removeAudioFromVideo(
        context: Context,
        sourceUri: Uri,
        videoTitle: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): MuteResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var tempFile: File? = null

        try {
            extractor.setDataSource(context, sourceUri, null)

            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                return@withContext MuteResult(
                    success = false,
                    errorMessage = "لم يتم العثور على مسار فيديو صالح في الملف المختار."
                )
            }

            val sanitizedTitle = videoTitle.replace(Regex("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]"), "_")
                .take(30).ifEmpty { "CinemaX_Silent" }
            tempFile = File(context.cacheDir, "silent_${sanitizedTitle}_${System.currentTimeMillis()}.mp4")

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            extractor.selectTrack(videoTrackIndex)
            val muxerTrack = muxer.addTrack(videoFormat)
            muxer.start()

            val maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 512 * 1024)
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val durationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                videoFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            withContext(Dispatchers.Main) {
                onProgress(0.1f, "جاري إزالة المسارات الصوتية والموسيقى بدون فقدان للجودة...")
            }

            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.offset = 0
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.presentationTimeUs = extractor.sampleTime

                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

                if (durationUs > 0) {
                    val progress = 0.1f + ((extractor.sampleTime.toFloat() / durationUs.toFloat()) * 0.8f)
                    withContext(Dispatchers.Main) {
                        onProgress(progress.coerceIn(0.1f, 0.9f), "جاري حفظ الفيديو الصامت النقي...")
                    }
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            if (!tempFile.exists() || tempFile.length() < 100) {
                return@withContext MuteResult(
                    success = false,
                    errorMessage = "فشلت العملية، الملف الناتج غير صالح."
                )
            }

            withContext(Dispatchers.Main) {
                onProgress(0.95f, "جاري الحفظ في مجلد الفيديوهات (Movies)...")
            }

            val fileName = "Silent_Muted_${sanitizedTitle}.mp4"
            val savedUri = saveToMovies(context, tempFile, fileName)

            val durationSec = (durationUs / 1_000_000L).coerceAtLeast(1)
            val formattedDur = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
            val formattedSize = formatBytes(tempFile.length())

            withContext(Dispatchers.Main) {
                onProgress(1.0f, "تم كتم الصوت وحفظ الفيديو الصامت بنجاح! 🔇")
            }

            return@withContext MuteResult(
                success = true,
                filePath = tempFile.absolutePath,
                uri = savedUri,
                durationFormatted = formattedDur,
                fileSizeFormatted = formattedSize
            )
        } catch (e: Exception) {
            return@withContext MuteResult(
                success = false,
                errorMessage = "خطأ أثناء إزالة الصوت من الفيديو: ${e.message}"
            )
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun saveToMovies(context: Context, sourceFile: File, fileName: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CinemaX_Silent")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
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

        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CinemaX_Silent")
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
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
    }
}
