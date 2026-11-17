package com.example.player

data class VideoQualityOption(
    val id: String, // "auto", "1080p", "720p", "480p", "360p", "240p", or track format ID
    val label: String,
    val resolutionLabel: String,
    val height: Int = 0,
    val width: Int = 0,
    val bitrate: Int = 0, // bps
    val fps: Float = 0f,
    val isAuto: Boolean = false,
    val isSelected: Boolean = false
)

data class VideoTechStats(
    val resolution: String = "",
    val codec: String = "",
    val frameRate: Float = 0f,
    val bitrateKbps: Long = 0L,
    val audioChannels: String = "",
    val audioSampleRate: Int = 0,
    val audioCodec: String = "",
    val decoderName: String = "",
    val isHardwareAccelerated: Boolean = true,
    val containerFormat: String = ""
)

data class SubtitleTrackOption(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false,
    val format: String = "SRT"
)
