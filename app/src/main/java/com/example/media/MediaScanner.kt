package com.example.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.ai.AiVideoCategorizer
import com.example.data.AppDatabase
import com.example.media.model.LocalVideo
import com.example.media.model.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MediaScanner(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun scanVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<LocalVideo>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn) ?: "Video_$id"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
                    val dataPath = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""

                    val folderName = if (bucketColumn >= 0) {
                        cursor.getString(bucketColumn) ?: "Videos"
                    } else {
                        extractFolderNameFromPath(dataPath)
                    }

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val resolution = if (width > 0 && height > 0) {
                        if (width >= 3840 || height >= 3840) "4K UHD"
                        else if (width >= 1920 || height >= 1920) "1080p FHD"
                        else if (width >= 1280 || height >= 1280) "720p HD"
                        else "${width}x${height}"
                    } else "HD"

                    val cleanTitle = displayName.substringBeforeLast(".")
                    val aiCategory = AiVideoCategorizer.categorize(
                        title = cleanTitle,
                        folderName = folderName,
                        durationMs = duration,
                        width = width,
                        height = height
                    )

                    videoList.add(
                        LocalVideo(
                            id = id,
                            uri = contentUri.toString(),
                            title = cleanTitle,
                            displayName = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            path = dataPath,
                            folderName = folderName,
                            dateAdded = dateAdded,
                            width = width,
                            height = height,
                            resolution = resolution,
                            aiCategory = aiCategory
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    suspend fun scanAudioFiles(): List<com.example.media.model.LocalAudio> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<com.example.media.model.LocalAudio>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.DURATION} >= 3000",
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val artist = if (artistCol >= 0) cursor.getString(artistCol) ?: "<فنان مجهول>" else "<فنان مجهول>"
                    val album = if (albumCol >= 0) cursor.getString(albumCol) ?: "<ألبوم عام>" else "<ألبوم عام>"
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L

                    val fileName = if (dataPath.isNotBlank()) dataPath.substringAfterLast("/") else "Audio_$id.mp3"
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else fileName.substringBeforeLast(".")

                    val folderName = extractFolderNameFromPath(dataPath)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val format = if (fileName.contains(".")) fileName.substringAfterLast(".").uppercase(Locale.ROOT) else "MP3"

                    audioList.add(
                        com.example.media.model.LocalAudio(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>" || artist.isBlank()) "فنان مجهول" else artist,
                            album = if (album == "<unknown>" || album.isBlank()) "موسيقى وأغاني" else album,
                            durationMs = duration,
                            sizeBytes = size,
                            contentUri = contentUri,
                            filePath = dataPath,
                            folderName = folderName,
                            dateAddedSec = dateAdded,
                            albumId = albumId,
                            format = format
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audioList
    }

    suspend fun getFolders(videos: List<LocalVideo>): List<VideoFolder> = withContext(Dispatchers.Default) {
        val folderMap = mutableMapOf<String, MutableList<LocalVideo>>()
        for (video in videos) {
            val list = folderMap.getOrPut(video.folderName) { mutableListOf() }
            list.add(video)
        }

        folderMap.map { (name, list) ->
            VideoFolder(
                name = name,
                videoCount = list.size,
                totalSizeBytes = list.sumOf { it.sizeBytes },
                firstVideoUri = list.firstOrNull()?.uri
            )
        }.sortedByDescending { it.videoCount }
    }

    sealed class MediaDeleteResult {
        object DirectSuccess : MediaDeleteResult()
        data class RequiresConsent(val intentSender: android.content.IntentSender) : MediaDeleteResult()
        data class Failed(val reason: String) : MediaDeleteResult()
    }

    suspend fun requestDeleteVideo(video: LocalVideo): MediaDeleteResult = withContext(Dispatchers.IO) {
        try {
            val uri = if (video.uri.startsWith("content://")) Uri.parse(video.uri) else null

            // Android 11+ (API 30+) MediaStore Scoped Storage requires system consent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri != null) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    return@withContext MediaDeleteResult.RequiresConsent(pendingIntent.intentSender)
                } catch (e: Exception) {
                    // Fallback to direct delete attempt
                }
            }

            // Android 10 (API 29) RecoverableSecurityException
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && uri != null) {
                try {
                    val rows = context.contentResolver.delete(uri, null, null)
                    if (rows > 0) {
                        finalizeDeletion(video)
                        return@withContext MediaDeleteResult.DirectSuccess
                    }
                } catch (secEx: SecurityException) {
                    if (secEx is android.app.RecoverableSecurityException) {
                        return@withContext MediaDeleteResult.RequiresConsent(secEx.userAction.actionIntent.intentSender)
                    }
                }
            }

            // Android 9 or below, or direct file path deletion
            var deleted = false
            if (uri != null) {
                try {
                    val rows = context.contentResolver.delete(uri, null, null)
                    deleted = rows > 0
                } catch (_: Exception) {}
            }
            if (!deleted && video.path.isNotBlank()) {
                try {
                    val file = java.io.File(video.path)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                } catch (_: Exception) {}
            }

            if (deleted) {
                finalizeDeletion(video)
                MediaDeleteResult.DirectSuccess
            } else {
                MediaDeleteResult.Failed("تعذر حذف الملف من ذاكرة الجهاز")
            }
        } catch (e: Exception) {
            MediaDeleteResult.Failed(e.localizedMessage ?: "حدث خطأ غير متوقع أثناء الحذف")
        }
    }

    suspend fun finalizeDeletion(video: LocalVideo): Boolean = withContext(Dispatchers.IO) {
        try {
            if (video.path.isNotBlank()) {
                try {
                    val file = java.io.File(video.path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            db.videoDao().deleteHistory(video.uri)
            db.videoDao().removeFavorite(video.uri)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteVideo(video: LocalVideo): Boolean = withContext(Dispatchers.IO) {
        val result = requestDeleteVideo(video)
        result is MediaDeleteResult.DirectSuccess
    }

    suspend fun requestDeleteAudio(audio: com.example.media.model.LocalAudio): MediaDeleteResult = withContext(Dispatchers.IO) {
        try {
            val uri = audio.contentUri

            // Android 11+ (API 30+) MediaStore Scoped Storage requires system consent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    return@withContext MediaDeleteResult.RequiresConsent(pendingIntent.intentSender)
                } catch (_: Exception) {
                    // Fallback to direct delete attempt
                }
            }

            // Android 10 (API 29) RecoverableSecurityException
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                try {
                    val rows = context.contentResolver.delete(uri, null, null)
                    if (rows > 0) {
                        finalizeAudioDeletion(audio)
                        return@withContext MediaDeleteResult.DirectSuccess
                    }
                } catch (secEx: SecurityException) {
                    if (secEx is android.app.RecoverableSecurityException) {
                        return@withContext MediaDeleteResult.RequiresConsent(secEx.userAction.actionIntent.intentSender)
                    }
                }
            }

            // Android 9 or below, or direct file deletion
            var deleted = false
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                deleted = rows > 0
            } catch (_: Exception) {}

            if (!deleted && audio.filePath.isNotBlank()) {
                try {
                    val file = java.io.File(audio.filePath)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                } catch (_: Exception) {}
            }

            if (deleted) {
                finalizeAudioDeletion(audio)
                MediaDeleteResult.DirectSuccess
            } else {
                MediaDeleteResult.Failed("تعذر حذف الملف الصوتي من ذاكرة الجهاز")
            }
        } catch (e: Exception) {
            MediaDeleteResult.Failed(e.localizedMessage ?: "حدث خطأ غير متوقع أثناء حذف الصوت")
        }
    }

    suspend fun finalizeAudioDeletion(audio: com.example.media.model.LocalAudio): Boolean = withContext(Dispatchers.IO) {
        try {
            if (audio.filePath.isNotBlank()) {
                try {
                    val file = java.io.File(audio.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun requestBatchDeleteUris(uris: List<Uri>, fallbackPaths: List<String> = emptyList()): MediaDeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext MediaDeleteResult.DirectSuccess
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    return@withContext MediaDeleteResult.RequiresConsent(pendingIntent.intentSender)
                } catch (_: Exception) {}
            }

            var deletedAny = false
            uris.forEach { uri ->
                try {
                    if (context.contentResolver.delete(uri, null, null) > 0) {
                        deletedAny = true
                    }
                } catch (_: Exception) {}
            }
            fallbackPaths.filter { it.isNotBlank() }.forEach { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists() && file.delete()) {
                        deletedAny = true
                    }
                } catch (_: Exception) {}
            }

            if (deletedAny) MediaDeleteResult.DirectSuccess
            else MediaDeleteResult.Failed("تعذر حذف الملفات المحددة من ذاكرة الجهاز")
        } catch (e: Exception) {
            MediaDeleteResult.Failed(e.localizedMessage ?: "حدث خطأ أثناء حذف الملفات المكررة")
        }
    }

    private fun extractFolderNameFromPath(path: String): String {
        return try {
            val parts = path.split("/")
            if (parts.size >= 2) parts[parts.size - 2] else "Internal"
        } catch (_: Exception) {
            "Videos"
        }
    }

    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
