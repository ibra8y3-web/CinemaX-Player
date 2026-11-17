package com.example.media.model

import android.net.Uri

data class LocalAudio(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val contentUri: Uri,
    val filePath: String,
    val folderName: String,
    val dateAddedSec: Long,
    val albumId: Long = 0L,
    val format: String = "MP3"
)
