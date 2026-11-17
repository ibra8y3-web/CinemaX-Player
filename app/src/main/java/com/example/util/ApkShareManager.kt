package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

object ApkShareManager {

    fun getApkSizeFormatted(context: Context): String {
        return try {
            val sourceDir = context.applicationInfo.sourceDir
            val file = File(sourceDir)
            if (file.exists()) {
                val bytes = file.length()
                formatFileSize(bytes)
            } else {
                "غير محدد"
            }
        } catch (e: Exception) {
            "غير متوفر"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun shareFullApk(context: Context, onProgress: (Boolean) -> Unit = {}) {
        withContext(Dispatchers.Main) {
            onProgress(true)
            Toast.makeText(context, "جاري تحضير ملف APK بالكامل للمشاركة...", Toast.LENGTH_SHORT).show()
        }

        withContext(Dispatchers.IO) {
            try {
                val appInfo = context.applicationInfo
                val originalApk = File(appInfo.sourceDir)

                if (!originalApk.exists()) {
                    withContext(Dispatchers.Main) {
                        onProgress(false)
                        Toast.makeText(context, "تعذر العثور على ملف APK الأساسي في النظام", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                val versionName = getAppVersion(context)
                val shareDir = File(context.cacheDir, "shared_apk")
                if (!shareDir.exists()) {
                    shareDir.mkdirs()
                }

                val cleanApk = File(shareDir, "CinemaX_Player_v$versionName.apk")
                
                // Copy APK bytes to shared cache location
                originalApk.copyTo(cleanApk, overwrite = true)

                val authority = "${context.packageName}.fileprovider"
                val apkUri = FileProvider.getUriForFile(context, authority, cleanApk)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, apkUri)
                    putExtra(Intent.EXTRA_SUBJECT, "تطبيق CinemaX Player - ملف APK كامل")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "حمّل واستمتع بتطبيق CinemaX Player لتشغيل الفيديوهات والبث المباشر (1000 قناة متنوعة وإذاعات القرآن الكريم) - ملف APK كامل جاهز للتثبيت المباشر."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    onProgress(false)
                    val chooser = Intent.createChooser(shareIntent, "مشاركة تطبيق CinemaX Player كامل (APK)")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onProgress(false)
                    Toast.makeText(context, "حدث خطأ أثناء استخراج ملف APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val df = DecimalFormat("#.##")
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "${df.format(gb)} GB"
            mb >= 1.0 -> "${df.format(mb)} MB"
            kb >= 1.0 -> "${df.format(kb)} KB"
            else -> "$bytes B"
        }
    }
}
