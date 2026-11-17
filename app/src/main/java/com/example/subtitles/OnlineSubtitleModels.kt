package com.example.subtitles

data class OnlineSubtitleItem(
    val id: String,
    val movieTitle: String,
    val releaseName: String = "",
    val language: String,
    val languageCode: String,
    val downloadUrl: String,
    val rating: Float = 4.8f,
    val downloadsCount: Int = 1250,
    val format: String = "SRT",
    val isDirectContent: Boolean = false,
    val directSrtContent: String = ""
)

data class SubtitleStyleConfig(
    val fontSizeSp: Int = 18,
    val textColor: Long = 0xFFFFFFFF, // White by default
    val backgroundColor: Long = 0x99000000, // Semi-transparent black
    val hasShadow: Boolean = true,
    val syncOffsetMs: Long = 0L,
    val isLiveTranslationEnabled: Boolean = false,
    val targetTranslationLang: String = "ar"
)
