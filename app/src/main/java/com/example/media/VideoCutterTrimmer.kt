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
import java.nio.ByteBuffer

/**
 * محرك تقطيع وقص الفيديوهات الحقيقي فائق السرعة دون إعادة ترميز (Fast Lossless Video Trimmer)
 * يعتمد على MediaExtractor و MediaMuxer لقص أي جزء بدقة زمنية وحفظ المقطع المستخرج
 * في مجلد الفيديوهات بالهاتف مع تسجيله تلقائياً في مكتبة وسائط النظام.
 */
object VideoCutterTrimmer {

    data class TrimProgress(
        val percent: Int,
        val status: String
    )

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        outputName: String,
        onProgress: (TrimProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            onProgress(TrimProgress(5, "جاري فحص مسارات الفيديو والصوت..."))
            extractor.setDataSource(context, sourceUri, null)
            val trackCount = extractor.trackCount

            val trackMap = mutableMapOf<Int, Int>() // extractorTrackIndex -> muxerTrackIndex
            var videoTrackIndex = -1
            var maxBufferSize = 1024 * 1024

            val sanitizedTitle = outputName.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
            val tempOutputFile = File(context.cacheDir, "clip_${System.currentTimeMillis()}_$sanitizedTitle.mp4")
            if (tempOutputFile.exists()) tempOutputFile.delete()

            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerIndex = muxer.addTrack(format)
                    trackMap[i] = muxerIndex
                    if (mime.startsWith("video/")) {
                        videoTrackIndex = i
                    }
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (size > maxBufferSize) maxBufferSize = size
                    }
                }
            }

            if (trackMap.isEmpty()) {
                return@withContext Result.failure(Exception("لم يتم العثور على مسارات فيديو أو صوت مدعومة في الملف"))
            }

            muxer.start()
            onProgress(TrimProgress(15, "جاري بدء اقتصاص المقطع المحدد..."))

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val durationUs = (endUs - startUs).coerceAtLeast(1L)

            // Seek to start position on video or keyframe
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var samplesWritten = 0
            var lastProgressPercent = 15

            while (true) {
                val sampleTrackIndex = extractor.sampleTrackIndex
                if (sampleTrackIndex < 0) break

                val muxerTrack = trackMap[sampleTrackIndex]
                val sampleTimeUs = extractor.sampleTime

                if (sampleTimeUs > endUs) {
                    // Check if all tracks are beyond endUs or done
                    extractor.unselectTrack(sampleTrackIndex)
                    var anyActive = false
                    for (t in trackMap.keys) {
                        if (extractor.sampleTrackIndex == t) anyActive = true
                    }
                    if (!anyActive && extractor.sampleTrackIndex < 0) break
                    extractor.advance()
                    continue
                }

                if (muxerTrack != null && sampleTimeUs >= startUs) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize >= 0) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        // Normalize timestamp to start from 0 for clean playback
                        bufferInfo.presentationTimeUs = (sampleTimeUs - startUs).coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags

                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        samplesWritten++

                        val progress = 15 + (((sampleTimeUs - startUs).toFloat() / durationUs.toFloat()) * 70f).toInt().coerceIn(0, 70)
                        if (progress > lastProgressPercent) {
                            lastProgressPercent = progress
                            onProgress(TrimProgress(progress, "جاري الحفظ السريع: $progress%"))
                        }
                    }
                }

                extractor.advance()
            }

            onProgress(TrimProgress(90, "جاري إنهاء الملف وتسجيله بالهاتف..."))
            try {
                muxer.stop()
                muxer.release()
                muxer = null
            } catch (e: Exception) {
                // If no samples were written, return failure
                if (samplesWritten == 0) {
                    return@withContext Result.failure(Exception("فشل حفظ المقطع: النطاق الزمني المحدد لا يحتوي على بيانات."))
                }
            }

            // Save to Public Movies folder via MediaStore
            val finalFile = saveToPublicMovies(context, tempOutputFile, sanitizedTitle)
            onProgress(TrimProgress(100, "اكتمل اقتصاص الفيديو بنجاح!"))
            Result.success(finalFile)
        } catch (e: Exception) {
            Result.failure(Exception("خطأ أثناء اقتصاص الفيديو: ${e.localizedMessage ?: "فشل غير معروف"}"))
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun saveToPublicMovies(context: Context, tempFile: File, title: String): File {
        val fileName = "CinemaX_Trim_${title}_${System.currentTimeMillis()}.mp4"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CinemaX_Clips")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(tempFile).use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            tempFile
        } else {
            val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CinemaX_Clips")
            if (!moviesDir.exists()) moviesDir.mkdirs()
            val destFile = File(moviesDir, fileName)
            tempFile.copyTo(destFile, overwrite = true)
            destFile
        }
    }
}
