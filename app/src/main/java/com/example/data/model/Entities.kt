package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val videoUri: String,
    val title: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val folderName: String = "",
    val resolution: String = ""
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val timestampMs: Long,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoUri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String, // UUID
    val originalTitle: String,
    val originalPath: String,
    val encryptedFileName: String,
    val fileSize: Long,
    val durationMs: Long,
    val encryptedAt: Long = System.currentTimeMillis(),
    val category: String = "خزنة آمنة"
)

@Entity(tableName = "downloads")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val localPath: String,
    val progressPercent: Int = 0,
    val totalBytes: Long = 0,
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)

data class LiveStreamChannel(
    val id: String,
    val title: String,
    val titleAr: String,
    val url: String,
    val category: String,
    val iconName: String,
    val description: String,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val backupUrls: List<String> = emptyList(),
    val isHealthy: Boolean = true,
    val failureCount: Int = 0,
    val lastCheckedTimestamp: Long = 0L
)

@Entity(tableName = "live_channels")
data class LiveStreamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleAr: String,
    val url: String,
    val category: String,
    val iconName: String,
    val description: String,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val backupUrlsString: String = "",
    val isHealthy: Boolean = true,
    val failureCount: Int = 0,
    val lastCheckedTimestamp: Long = 0L
) {
    fun toLiveStreamChannel(): LiveStreamChannel {
        val backups = if (backupUrlsString.isBlank()) emptyList() else backupUrlsString.split("|||")
        return LiveStreamChannel(
            id = id,
            title = title,
            titleAr = titleAr,
            url = url,
            category = category,
            iconName = iconName,
            description = description,
            isCustom = isCustom,
            isFavorite = isFavorite,
            backupUrls = backups,
            isHealthy = isHealthy,
            failureCount = failureCount,
            lastCheckedTimestamp = lastCheckedTimestamp
        )
    }

    companion object {
        fun fromChannel(c: LiveStreamChannel): LiveStreamEntity {
            return LiveStreamEntity(
                id = c.id,
                title = c.title,
                titleAr = c.titleAr,
                url = c.url,
                category = c.category,
                iconName = c.iconName,
                description = c.description,
                isCustom = c.isCustom,
                isFavorite = c.isFavorite,
                backupUrlsString = c.backupUrls.joinToString("|||"),
                isHealthy = c.isHealthy,
                failureCount = c.failureCount,
                lastCheckedTimestamp = c.lastCheckedTimestamp
            )
        }
    }
}
