package com.example.player

data class PlaylistItem(
    val uri: String,
    val title: String,
    val durationMs: Long = 0L,
    val isLiveStream: Boolean = false,
    val backupUrls: List<String> = emptyList()
)

data class AutoPlayState(
    val isCountingDown: Boolean = false,
    val remainingSeconds: Int = 5,
    val nextVideo: PlaylistItem? = null
)
