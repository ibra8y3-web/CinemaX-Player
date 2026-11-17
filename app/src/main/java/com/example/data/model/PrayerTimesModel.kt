package com.example.data.model

enum class PrayerType(val id: String, val titleAr: String, val iconName: String) {
    FAJR("fajr", "الفجر", "nights_stay"),
    SUNRISE("sunrise", "الشروق", "wb_sunny"),
    DHUHR("dhuhr", "الظهر", "light_mode"),
    ASR("asr", "العصر", "wb_twilight"),
    MAGHRIB("maghrib", "المغرب", "bedtime"),
    ISHA("isha", "العشاء", "dark_mode")
}

data class PrayerTimeEntry(
    val type: PrayerType,
    val timeFormatted: String,
    val hour24: Int,
    val minute: Int,
    val isNext: Boolean = false,
    val isPassed: Boolean = false,
    val adhanEnabled: Boolean = true
)

enum class CalculationMethod(val titleAr: String, val fajrAngle: Double, val ishaAngle: Double) {
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MAKKAH("جامعة أم القرى - مكة المكرمة", 18.5, 90.0),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية", 15.0, 15.0),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0)
}

data class CityLocation(
    val nameAr: String,
    val nameEn: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double
)

data class AdhanVoice(
    val id: String,
    val nameAr: String,
    val muazzinName: String,
    val rawResId: Int = 0,
    val streamUrl: String = "",
    val durationSec: Int = 180
)

enum class AdhanAlarmType(val code: String, val labelAr: String) {
    ADHAN("ADHAN", "أذان الصلاة"),
    PRE_ADHAN("PRE_ADHAN", "تنبيه قبل الأذان"),
    POST_ADHAN("POST_ADHAN", "تنبيه بعد الأذان / الإقامة"),
    TEST("TEST", "اختبار الأذان"),
    STOPPED("STOPPED", "تم إيقاف الأذان"),
    SCHEDULED("SCHEDULED", "جدولة المنبهات")
}

data class AdhanLogEntry(
    val id: String,
    val timestampMs: Long,
    val formattedTime: String,
    val prayerName: String,
    val cityName: String,
    val voiceName: String,
    val eventType: AdhanAlarmType,
    val note: String = ""
)

data class DhikrPhrase(
    val id: String,
    val textAr: String,
    val virtueAr: String,
    val defaultTarget: Int
)

