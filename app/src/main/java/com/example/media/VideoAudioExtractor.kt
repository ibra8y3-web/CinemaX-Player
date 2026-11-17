package com.example.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * محرك استخراج الصوت الحقيقي وفصل النغمة من مقاطع الفيديو
 * يعتمد على MediaExtractor و MediaMuxer لإنتاج ملف صوتي نقي M4A/AAC عالي الجودة
 * وحفظه في مجلد الموسيقى العام وتسجيله في مكتبة الموسيقى بالهاتف.
 */
object VideoAudioExtractor {

    suspend fun extractAudioTrack(
        context: Context,
        videoUri: Uri,
        outputTitle: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, videoUri, null)
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

            if (audioTrackIndex < 0 || audioFormat == null) {
                return@withContext Result.failure(Exception("لا يحتوي هذا الفيديو على مسار صوتي قابل للاستخراج"))
            }

            val sanitizedTitle = outputTitle.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
            val outputFile = File(context.cacheDir, "${sanitizedTitle}_extracted.m4a")
            if (outputFile.exists()) outputFile.delete()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            extractor.selectTrack(audioTrackIndex)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            // Save to Public Music directory and register to MediaStore
            val finalName = "${sanitizedTitle}.m4a"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, finalName)
                    put(MediaStore.Audio.Media.TITLE, outputTitle)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/NovaAudio")
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        outputFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "NovaAudio")
                if (!musicDir.exists()) musicDir.mkdirs()
                val destFile = File(musicDir, finalName)
                outputFile.copyTo(destFile, overwrite = true)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            muxer?.release()
            extractor.release()
            Result.failure(e)
        }
    }
}
