package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * استوديو استخراج لقطات الشاشة والصور فائقة الدقة من الفيديو (HD Video Snapshot Studio)
 * يتيح التقاط أي لقطة بالملي ثانية بدقتها الكاملة، تطبيق فلاتر فوتوغرافية، وحفظها في المعرض.
 */
object VideoSnapshotStudio {

    enum class SnapshotFilter(val displayName: String) {
        ORIGINAL("أصلي (بدون تعديل)"),
        VIVID("ألوان حيوية (Vivid HDR)"),
        CINEMATIC_WARM("سينمائي دافئ (Warm Gold)"),
        BLACK_WHITE("أبيض وأسود كلاسيكي (B&W)")
    }

    suspend fun captureFrame(
        context: Context,
        videoUri: Uri,
        timeMs: Long,
        filter: SnapshotFilter = SnapshotFilter.ORIGINAL
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeUs = timeMs * 1000L
            val frameBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, 1920, 1080)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            }

            if (frameBitmap == null) {
                return@withContext Result.failure(Exception("تعذر استخراج اللقطة عند هذا التوقيت"))
            }

            val filtered = applyFilter(frameBitmap, filter)
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في استخراج اللقطة: ${e.localizedMessage}"))
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun applyFilter(src: Bitmap, filter: SnapshotFilter): Bitmap {
        if (filter == SnapshotFilter.ORIGINAL) return src
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = ColorMatrix()
        when (filter) {
            SnapshotFilter.ORIGINAL -> {}
            SnapshotFilter.VIVID -> {
                // Boost saturation and contrast slightly
                colorMatrix.setSaturation(1.35f)
            }
            SnapshotFilter.CINEMATIC_WARM -> {
                // Add warm golden-hour tint
                val warmMatrix = floatArrayOf(
                    1.15f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(warmMatrix)
            }
            SnapshotFilter.BLACK_WHITE -> {
                colorMatrix.setSaturation(0f)
            }
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    suspend fun saveSnapshotToGallery(
        context: Context,
        bitmap: Bitmap,
        videoTitle: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = videoTitle.replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
            val fileName = "CinemaX_Shot_${cleanTitle}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CinemaX_Snapshots")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(Exception("تعذر إنشاء مدخل في المعرض"))

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                Result.success(uri)
            } else {
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CinemaX_Snapshots")
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                }
                val uri = Uri.fromFile(file)
                Result.success(uri)
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر حفظ اللقطة: ${e.localizedMessage}"))
        }
    }

    suspend fun shareSnapshot(
        context: Context,
        bitmap: Bitmap,
        title: String
    ): Result<Intent> = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "shared_shot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", cacheFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "لقطة من فيديو: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "مشاركة اللقطة عبر"))
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تجهيز المشاركة: ${e.localizedMessage}"))
        }
    }
}
