package com.example.duas.model

import androidx.compose.ui.graphics.Color

enum class DuaCategory(
    val titleAr: String,
    val iconName: String,
    val descriptionAr: String,
    val accentColor: Color
) {
    ALL(
        titleAr = "كافة الأدعية والأذكار",
        iconName = "all",
        descriptionAr = "جميع الأدعية والأذكار النبوية والقرآنية الموثقة",
        accentColor = Color(0xFF22E3FF)
    ),
    QURANIC(
        titleAr = "أدعية القرآن الكريم",
        iconName = "quran",
        descriptionAr = "الأدعية المباركة المذكورة في آيات القرآن الحكيم",
        accentColor = Color(0xFF2EE6A6)
    ),
    MORNING(
        titleAr = "أذكار الصباح",
        iconName = "sun",
        descriptionAr = "أذكار الصباح الصحيحة الثابتة عن النبي صلى الله عليه وسلم",
        accentColor = Color(0xFFFFB300)
    ),
    EVENING(
        titleAr = "أذكار المساء",
        iconName = "moon",
        descriptionAr = "أذكار المساء الثابتة للحفظ والتحصين من كل شر",
        accentColor = Color(0xFF7C4DFF)
    ),
    AFTER_PRAYER(
        titleAr = "أذكار بعد الصلاة",
        iconName = "prayer",
        descriptionAr = "الأذكار والاستغفار والتسبيح بعد الصلوات المكتوبة",
        accentColor = Color(0xFF29B6F6)
    ),
    SLEEP_WAKING(
        titleAr = "أذكار النوم والاستيقاظ",
        iconName = "bed",
        descriptionAr = "أدعية النوم والقلق وتفسير المنام والاستيقاظ",
        accentColor = Color(0xFF5C6BC0)
    ),
    DISTRESS_RELIEF(
        titleAr = "الهم والكرب وتفريج الشدائد",
        iconName = "relief",
        descriptionAr = "أدعية الفرج ودفع الحزن والغم وضيق الصدر",
        accentColor = Color(0xFFFF6B8B)
    ),
    RUQYAH(
        titleAr = "الرقية الشرعية والشفاء",
        iconName = "shield",
        descriptionAr = "آيات وأدعية الرقية من العين والحسد والمرض",
        accentColor = Color(0xFF26A69A)
    ),
    ISTIKHARA_NEED(
        titleAr = "الاستخارة وتيسير الأمور",
        iconName = "compass",
        descriptionAr = "دعاء الاستخارة النبوي وأدعية قضاء الحوائج",
        accentColor = Color(0xFFAB47BC)
    ),
    TRAVEL_OUTDOOR(
        titleAr = "السفر والخروج ودخول المنزل",
        iconName = "travel",
        descriptionAr = "أدعية ركوب الدابة والسفر والخروج والعودة",
        accentColor = Color(0xFF26C6DA)
    ),
    PROVISION_DEBT(
        titleAr = "الرزق والبركة وقضاء الدين",
        iconName = "coins",
        descriptionAr = "أدعية سعة الرزق وسداد الديون ونماء المال الحلال",
        accentColor = Color(0xFF66BB6A)
    ),
    PROPHETIC_COMPREHENSIVE(
        titleAr = "جوامع الدعاء النبوية",
        iconName = "star",
        descriptionAr = "أشمل وأعظم الأدعية النبوية الجامعة لخيري الدنيا والآخرة",
        accentColor = Color(0xFFEC407A)
    ),
    FORGIVENESS_TAWBAH(
        titleAr = "الاستغفار والتوبة وسيد الاستغفار",
        iconName = "heart",
        descriptionAr = "صيغ الاستغفار العظيمة ومحو الذنوب والخطايا",
        accentColor = Color(0xFF8D6E63)
    ),
    PARENTS_FAMILY(
        titleAr = "للوالدين والذرية والأهل",
        iconName = "family",
        descriptionAr = "أدعية البر بالوالدين وصلاح الأولاد والزوجين والذرية",
        accentColor = Color(0xFFFF7043)
    )
}
