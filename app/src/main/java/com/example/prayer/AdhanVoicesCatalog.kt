package com.example.prayer

import com.example.R
import com.example.data.model.AdhanVoice

object AdhanVoicesCatalog {

    val voices: List<AdhanVoice> by lazy {
        listOf(
            AdhanVoice(
                id = "adhan_makkah",
                nameAr = "أذان المسجد الحرام (مكة المكرمة)",
                muazzinName = "الشيخ علي أحمد ملا (شيخ مؤذني الحرم المكي)",
                rawResId = R.raw.adhan_makkah,
                durationSec = 246
            ),
            AdhanVoice(
                id = "adhan_madinah",
                nameAr = "أذان المسجد النبوي الشريف",
                muazzinName = "أذان الحرم النبوي الشريف (المدينة المنورة)",
                rawResId = R.raw.adhan_madinah,
                durationSec = 190
            ),
            AdhanVoice(
                id = "adhan_surayhi",
                nameAr = "أذان المسجد النبوي (الشيخ السريحي)",
                muazzinName = "الشيخ عبد المجيد السريحي (مؤذن المسجد النبوي)",
                rawResId = R.raw.adhan_surayhi,
                durationSec = 210
            ),
            AdhanVoice(
                id = "adhan_alaqsa",
                nameAr = "أذان المسجد الأقصى المبارك",
                muazzinName = "الشيخ ناجي قزاز (مؤذن المسجد الأقصى - القدس)",
                rawResId = R.raw.adhan_alaqsa,
                durationSec = 152
            ),
            AdhanVoice(
                id = "adhan_refaat",
                nameAr = "أذان مصر التاريخي الخاشع",
                muazzinName = "الشيخ محمد رفعت (صوت القاهرة الخالد)",
                rawResId = R.raw.adhan_refaat,
                durationSec = 204
            ),
            AdhanVoice(
                id = "adhan_abdulbasit",
                nameAr = "أذان الشيخ عبد الباسط عبد الصمد",
                muazzinName = "الشيخ عبد الباسط عبد الصمد (مصر)",
                rawResId = R.raw.adhan_abdulbasit,
                durationSec = 245
            ),
            AdhanVoice(
                id = "adhan_alafasy",
                nameAr = "أذان الشيخ مشاري العفاسي",
                muazzinName = "الشيخ مشاري راشد العفاسي (الكويت)",
                rawResId = R.raw.adhan_alafasy,
                durationSec = 241
            ),
            AdhanVoice(
                id = "adhan_qatami",
                nameAr = "أذان الشيخ ناصر القطامي",
                muazzinName = "الشيخ ناصر القطامي (الرياض)",
                rawResId = R.raw.adhan_qatami,
                durationSec = 132
            ),
            AdhanVoice(
                id = "adhan_fajr_hijaz",
                nameAr = "أذان الفجر بالمقام الحجازي",
                muazzinName = "أذان الفجر الندي بالمقام الحجازي الأصيل",
                rawResId = R.raw.adhan_fajr_hijaz,
                durationSec = 201
            ),
            AdhanVoice(
                id = "adhan_sham_khashia",
                nameAr = "الأذان الشامي الخاشع",
                muazzinName = "الأذان بالمقام الشامي الخاشع (بصوت ندي)",
                rawResId = R.raw.adhan_sham_khashia,
                durationSec = 212
            )
        )
    }

    fun getVoiceById(id: String): AdhanVoice {
        val migratedId = when (id) {
            "adhan_dosari" -> "adhan_surayhi"
            "adhan_hussary" -> "adhan_fajr_hijaz"
            "adhan_minshawi" -> "adhan_sham_khashia"
            else -> id
        }
        return voices.find { it.id == migratedId } ?: voices.first()
    }
}
