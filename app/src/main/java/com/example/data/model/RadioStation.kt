package com.example.data.model

enum class RadioCategory(val titleAr: String, val badgeColor: Long) {
    QURAN("إذاعات القرآن الكريم", 0xFF22E3FF),
    ATHKAR("الأذكار والرقية الشرعية", 0xFF2EE6A6),
    SONGS_HITS("أغاني عربية حديثة وتريند", 0xFFFF4081),
    TARAB("طرب أصيل والزمن الجميل", 0xFFE040FB),
    MUSIC_RELAX("منوعات وهدوء عربي", 0xFFFFC857),
    FRANCE_GLOBAL("إذاعات فرنسا وأوروبا 🇫🇷", 0xFF2979FF),
    ARABIC_RADIO("محطات مصر والخليج 🇪🇬🇸🇦", 0xFFFF9100),
    TURKISH_RADIO("إذاعات تركية 🇹🇷", 0xFFFF1744),
    UK_USA("إذاعات بريطانيا وأمريكا 🇬🇧🇺🇸", 0xFF7C4DFF),
    WORLD_GLOBAL("الإذاعات العالمية 🌍", 0xFF5B8CFF)
}

data class RadioStation(
    val id: String,
    val titleAr: String,
    val reciterOrDesc: String,
    val streamUrl: String,
    val backupUrl: String? = null,
    val category: RadioCategory,
    val bitrateKbps: Int = 64,
    val isUltraLowData: Boolean = true,
    val iconName: String = "radio",
    val country: String = "العالم",
    val faviconUrl: String? = null
)
