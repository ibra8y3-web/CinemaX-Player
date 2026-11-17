package com.example.prayer

import com.example.data.model.AdhanVoice

object AdhanVoiceSelector {

    /**
     * Automatically selects the most authentic regional Muezzin voice based on GPS / city location.
     */
    fun selectVoiceForLocation(lat: Double, lon: Double, country: String = "", cityName: String = ""): AdhanVoice {
        val lowerCountry = country.lowercase()
        val lowerCity = cityName.lowercase()

        // 1. Makkah Al-Mukarramah
        if (cityName.contains("مكة") || lowerCity.contains("makkah") || (lat in 20.8..22.2 && lon in 39.0..40.8)) {
            return AdhanVoicesCatalog.getVoiceById("adhan_makkah") // الشيخ علي أحمد ملا
        }

        // 2. Madinah Al-Munawwarah
        if (cityName.contains("المدينة") || lowerCity.contains("madinah") || (lat in 23.8..25.2 && lon in 39.0..40.5)) {
            return AdhanVoicesCatalog.getVoiceById("adhan_madinah") // أذان الحرم النبوي
        }

        // 3. Rest of Saudi Arabia
        if (country.contains("السعودية") || lowerCountry.contains("saudi")) {
            if (cityName.contains("الرياض") || lowerCity.contains("riyadh")) {
                return AdhanVoicesCatalog.getVoiceById("adhan_qatami") // الشيخ ناصر القطامي
            }
            return AdhanVoicesCatalog.getVoiceById("adhan_makkah")
        }

        // 4. Egypt & Sudan
        if (country.contains("مصر") || lowerCountry.contains("egypt") || country.contains("سودان") || (lat in 21.0..32.0 && lon in 24.0..37.0)) {
            return AdhanVoicesCatalog.getVoiceById("adhan_refaat") // الشيخ محمد رفعت - صوت مصر المصلح الخالد
        }

        // 5. Palestine & Levant (Jordan, Syria, Lebanon)
        if (country.contains("فلسطين") || lowerCountry.contains("palestine") || country.contains("الأردن") || country.contains("سوريا") || country.contains("لبنان") || (lat in 29.0..37.0 && lon in 34.0..42.0)) {
            return AdhanVoicesCatalog.getVoiceById("adhan_alaqsa") // الشيخ ناجي قزاز - المسجد الأقصى بالقدس
        }

        // 6. Kuwait, UAE, Qatar, Bahrain, Oman
        if (country.contains("الكويت") || country.contains("الإمارات") || country.contains("قطر") || country.contains("البحرين") || country.contains("عُمان") || (lat in 16.0..30.0 && lon in 47.0..60.0)) {
            return AdhanVoicesCatalog.getVoiceById("adhan_alafasy") // الشيخ مشاري راشد العفاسي
        }

        // 7. Global / North Africa / Europe / Americas / Asia
        return AdhanVoicesCatalog.getVoiceById("adhan_abdulbasit") // الشيخ عبد الباسط عبد الصمد
    }
}
