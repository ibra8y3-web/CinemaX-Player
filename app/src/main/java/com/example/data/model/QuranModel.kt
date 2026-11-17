package com.example.data.model

data class QuranSurah(
    val number: Int,
    val nameAr: String,
    val englishName: String,
    val englishTranslation: String,
    val revelationType: String, // "مكية" or "مدنية"
    val numberOfAyahs: Int,
    val juzNumber: Int,
    val pageNumber: Int
)

data class QuranAyah(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val ayahNumberOverall: Int,
    val textAr: String,
    val juz: Int,
    val page: Int
)

data class QuranBookmark(
    val surahNumber: Int,
    val surahNameAr: String,
    val ayahNumber: Int,
    val timestampMs: Long
)
