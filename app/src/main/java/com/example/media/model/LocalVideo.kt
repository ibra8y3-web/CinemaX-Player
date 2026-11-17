package com.example.media.model

data class LocalVideo(
    val id: Long,
    val uri: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val path: String,
    val folderName: String,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val resolution: String,
    val aiCategory: String,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0
)

data class VideoFolder(
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val firstVideoUri: String?
)
