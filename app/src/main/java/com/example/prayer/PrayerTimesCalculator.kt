package com.example.prayer

import com.example.data.model.CalculationMethod
import com.example.data.model.CityLocation
import com.example.data.model.PrayerTimeEntry
import com.example.data.model.PrayerType
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PrayerTimesCalculator {

    // Kaaba Coordinates in Makkah al-Mukarramah
    const val KAABA_LATITUDE = 21.422524
    const val KAABA_LONGITUDE = 39.826182

    val defaultCities: List<CityLocation> = listOf(
        // المملكة العربية السعودية
        CityLocation("مكة المكرمة", "Makkah", "المملكة العربية السعودية", 21.4225, 39.8262, 3.0),
        CityLocation("المدينة المنورة", "Madinah", "المملكة العربية السعودية", 24.4672, 39.6111, 3.0),
        CityLocation("الرياض", "Riyadh", "المملكة العربية السعودية", 24.7136, 46.6753, 3.0),
        CityLocation("جدة", "Jeddah", "المملكة العربية السعودية", 21.5433, 39.1728, 3.0),
        CityLocation("الدمام", "Dammam", "المملكة العربية السعودية", 26.4207, 50.0888, 3.0),
        CityLocation("الخبر", "Khobar", "المملكة العربية السعودية", 26.2172, 50.1971, 3.0),
        CityLocation("الطائف", "Taif", "المملكة العربية السعودية", 21.2854, 40.4244, 3.0),
        CityLocation("تبوك", "Tabuk", "المملكة العربية السعودية", 28.3835, 36.5662, 3.0),
        CityLocation("أبها", "Abha", "المملكة العربية السعودية", 18.2164, 42.5053, 3.0),
        CityLocation("خميس مشيط", "Khamis Mushait", "المملكة العربية السعودية", 18.3000, 42.7333, 3.0),
        CityLocation("جازان", "Jizan", "المملكة العربية السعودية", 16.8892, 42.5511, 3.0),
        CityLocation("نجران", "Najran", "المملكة العربية السعودية", 17.4933, 44.1277, 3.0),
        CityLocation("بريدة", "Buraidah", "المملكة العربية السعودية", 26.3260, 43.9750, 3.0),
        CityLocation("عنيزة", "Unaizah", "المملكة العربية السعودية", 26.0843, 43.9937, 3.0),
        CityLocation("حائل", "Hail", "المملكة العربية السعودية", 27.5219, 41.6907, 3.0),
        CityLocation("حفر الباطن", "Hafar Al-Batin", "المملكة العربية السعودية", 28.4328, 45.9708, 3.0),
        CityLocation("ينبع", "Yanbu", "المملكة العربية السعودية", 24.0891, 38.0637, 3.0),
        CityLocation("الجبيل", "Jubail", "المملكة العربية السعودية", 27.0046, 49.6592, 3.0),
        CityLocation("الأحساء (الهفوف)", "Al Ahsa", "المملكة العربية السعودية", 25.3833, 49.5833, 3.0),

        // جمهورية مصر العربية (محافظات ومدن ومراكز)
        CityLocation("القاهرة", "Cairo", "مصر", 30.0444, 31.2357, 2.0),
        CityLocation("الجيزة", "Giza", "مصر", 30.0131, 31.2089, 2.0),
        CityLocation("الإسكندرية", "Alexandria", "مصر", 31.2001, 29.9187, 2.0),
        CityLocation("طنطا (الغربية)", "Tanta", "مصر", 30.7865, 31.0004, 2.0),
        CityLocation("المنصورة (الدقهلية)", "Mansoura", "مصر", 31.0409, 31.3785, 2.0),
        CityLocation("الزقازيق (الشرقية)", "Zagazig", "مصر", 30.5877, 31.5020, 2.0),
        CityLocation("شبين الكوم (المنوفية)", "Shibin El Kom", "مصر", 30.5503, 31.0102, 2.0),
        CityLocation("بنها (القليوبية)", "Banha", "مصر", 30.4628, 31.1797, 2.0),
        CityLocation("كفر الشيخ", "Kafr El Sheikh", "مصر", 31.1107, 30.9388, 2.0),
        CityLocation("دمنهور (البحيرة)", "Damanhour", "مصر", 31.0425, 30.4721, 2.0),
        CityLocation("المحلة الكبرى", "El Mahalla El Kubra", "مصر", 30.9706, 31.1669, 2.0),
        CityLocation("دمياط", "Damietta", "مصر", 31.4165, 31.8133, 2.0),
        CityLocation("بورسعيد", "Port Said", "مصر", 31.2653, 32.3019, 2.0),
        CityLocation("الإسماعيلية", "Ismailia", "مصر", 30.5965, 32.2715, 2.0),
        CityLocation("السويس", "Suez", "مصر", 29.9668, 32.5498, 2.0),
        CityLocation("الفيوم", "Fayoum", "مصر", 29.3084, 30.8428, 2.0),
        CityLocation("بني سويف", "Beni Suef", "مصر", 29.0661, 31.0994, 2.0),
        CityLocation("المنيا", "Minya", "مصر", 28.0871, 30.7618, 2.0),
        CityLocation("أسيوط", "Asyut", "مصر", 27.1810, 31.1837, 2.0),
        CityLocation("سوهاج", "Sohag", "مصر", 26.5570, 31.6948, 2.0),
        CityLocation("قنا", "Qena", "مصر", 26.1551, 32.7160, 2.0),
        CityLocation("الأقصر", "Luxor", "مصر", 25.6872, 32.6396, 2.0),
        CityLocation("أسوان", "Aswan", "مصر", 24.0889, 32.8998, 2.0),
        CityLocation("مرسى مطروح", "Marsa Matrouh", "مصر", 31.3543, 27.2373, 2.0),
        CityLocation("الغردقة (البحر الأحمر)", "Hurghada", "مصر", 27.2579, 33.8116, 2.0),
        CityLocation("شرم الشيخ (جنوب سيناء)", "Sharm El Sheikh", "مصر", 27.9158, 34.3299, 2.0),
        CityLocation("العريش (شمال سيناء)", "El Arish", "مصر", 31.1316, 33.8033, 2.0),

        // دولة فلسطين
        CityLocation("القدس الشريف", "Jerusalem", "فلسطين", 31.7683, 35.2137, 2.0),
        CityLocation("غزة", "Gaza", "فلسطين", 31.5017, 34.4668, 2.0),
        CityLocation("الخليل", "Hebron", "فلسطين", 31.5326, 35.0998, 2.0),
        CityLocation("نابلس", "Nablus", "فلسطين", 32.2211, 35.2544, 2.0),
        CityLocation("رام الله", "Ramallah", "فلسطين", 31.9029, 35.2062, 2.0),
        CityLocation("بيت لحم", "Bethlehem", "فلسطين", 31.7054, 35.2024, 2.0),
        CityLocation("جنين", "Jenin", "فلسطين", 32.4594, 35.2989, 2.0),

        // المملكة الأردنية الهاشمية
        CityLocation("عَمّان", "Amman", "الأردن", 31.9454, 35.9284, 3.0),
        CityLocation("الزرقاء", "Zarqa", "الأردن", 32.0728, 36.0880, 3.0),
        CityLocation("إربد", "Irbid", "الأردن", 32.5568, 35.8500, 3.0),
        CityLocation("العقبة", "Aqaba", "الأردن", 29.5321, 35.0063, 3.0),

        // الإمارات العربية المتحدة
        CityLocation("أبوظبي", "Abu Dhabi", "الإمارات العربية المتحدة", 24.4539, 54.3773, 4.0),
        CityLocation("دبي", "Dubai", "الإمارات العربية المتحدة", 25.2048, 55.2708, 4.0),
        CityLocation("الشارقة", "Sharjah", "الإمارات العربية المتحدة", 25.3463, 55.4209, 4.0),
        CityLocation("العين", "Al Ain", "الإمارات العربية المتحدة", 24.2075, 55.7447, 4.0),

        // الكويت وقطر والبحرين وعُمان
        CityLocation("الكويت", "Kuwait City", "الكويت", 29.3759, 47.9774, 3.0),
        CityLocation("الدوحة", "Doha", "قطر", 25.2854, 51.5310, 3.0),
        CityLocation("المنامة", "Manama", "البحرين", 26.2285, 50.5860, 3.0),
        CityLocation("مسقط", "Muscat", "عُمان", 23.5859, 58.4059, 4.0),

        // الشام والعراق واليمن
        CityLocation("دمشق", "Damascus", "سوريا", 33.5138, 36.2765, 3.0),
        CityLocation("حلب", "Aleppo", "سوريا", 36.2021, 37.1343, 3.0),
        CityLocation("بيروت", "Beirut", "لبنان", 33.8938, 35.5018, 2.0),
        CityLocation("بغداد", "Baghdad", "العراق", 33.3152, 44.3661, 3.0),
        CityLocation("أربيل", "Erbil", "العراق", 36.1901, 44.0091, 3.0),
        CityLocation("البصرة", "Basra", "العراق", 30.5081, 47.7835, 3.0),
        CityLocation("الموصل", "Mosul", "العراق", 36.3400, 43.1300, 3.0),
        CityLocation("صنعاء", "Sanaa", "اليمن", 15.3694, 44.1910, 3.0),
        CityLocation("عدن", "Aden", "اليمن", 12.7855, 45.0187, 3.0),

        // المغرب العربي والسودان
        CityLocation("الرباط", "Rabat", "المغرب", 34.0209, -6.8416, 1.0),
        CityLocation("الدار البيضاء", "Casablanca", "المغرب", 33.5731, -7.5898, 1.0),
        CityLocation("فاس", "Fes", "المغرب", 34.0331, -5.0003, 1.0),
        CityLocation("مراكش", "Marrakech", "المغرب", 31.6295, -7.9811, 1.0),
        CityLocation("الجزائر", "Algiers", "الجزائر", 36.7538, 3.0588, 1.0),
        CityLocation("وهران", "Oran", "الجزائر", 35.6971, -0.6308, 1.0),
        CityLocation("تونس", "Tunis", "تونس", 36.8065, 10.1815, 1.0),
        CityLocation("صفاقس", "Sfax", "تونس", 34.7406, 10.7603, 1.0),
        CityLocation("طرابلس", "Tripoli", "ليبيا", 32.8872, 13.1913, 2.0),
        CityLocation("بنغازي", "Benghazi", "ليبيا", 32.1167, 20.0667, 2.0),
        CityLocation("الخرطوم", "Khartoum", "السودان", 15.5007, 32.5599, 2.0),

        // إسطنبول وأوروبا وأمريكا وآسيا
        CityLocation("إسطنبول", "Istanbul", "تركيا", 41.0082, 28.9784, 3.0),
        CityLocation("أنقرة", "Ankara", "تركيا", 39.9334, 32.8597, 3.0),
        CityLocation("لندن", "London", "المملكة المتحدة", 51.5074, -0.1278, 0.0),
        CityLocation("باريس", "Paris", "فرنسا", 48.8566, 2.3522, 1.0),
        CityLocation("برلين", "Berlin", "ألمانيا", 52.5200, 13.4050, 1.0),
        CityLocation("مدريد", "Madrid", "إسبانيا", 40.4168, -3.7038, 1.0),
        CityLocation("نيويورك", "New York", "الولايات المتحدة", 40.7128, -74.0060, -5.0),
        CityLocation("واشنطن", "Washington", "الولايات المتحدة", 38.9072, -77.0369, -5.0),
        CityLocation("لوس أنجلوس", "Los Angeles", "الولايات المتحدة", 34.0522, -118.2437, -8.0),
        CityLocation("تورونتو", "Toronto", "كندا", 43.6532, -79.3832, -5.0),
        CityLocation("سيدني", "Sydney", "أستراليا", -33.8688, 151.2093, 10.0),
        CityLocation("جاكرتا", "Jakarta", "إندونيسيا", -6.2088, 106.8456, 7.0),
        CityLocation("كوالالمبور", "Kuala Lumpur", "ماليزيا", 3.1390, 101.6869, 8.0),
        CityLocation("إسلام آباد", "Islamabad", "باكستان", 33.6844, 73.0479, 5.0)
    )

    /**
     * Calculates Qibla direction (Angle from True North in degrees 0..360).
     */
    fun calculateQiblaBearing(lat: Double, lon: Double): Float {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val kaabaLonRad = Math.toRadians(KAABA_LONGITUDE)

        val dLon = kaabaLonRad - lonRad
        val y = sin(dLon)
        val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(dLon)
        var qibla = Math.toDegrees(atan2(y, x))
        if (qibla < 0) {
            qibla += 360.0
        }
        return qibla.toFloat()
    }

    /**
     * Distance to Kaaba in kilometers (Haversine formula).
     */
    fun calculateDistanceToKaabaKm(lat: Double, lon: Double): Int {
        val r = 6371.0 // Earth radius in KM
        val dLat = Math.toRadians(KAABA_LATITUDE - lat)
        val dLon = Math.toRadians(KAABA_LONGITUDE - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LATITUDE)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt()
    }

    /**
     * Astronomical calculation of exact prayer times for given coordinates and date.
     */
    fun calculatePrayerTimes(
        lat: Double,
        lon: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.EGYPT
    ): List<PrayerTimeEntry> {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        // Approximate solar calculations
        val gamma = 2 * Math.PI / 365 * (dayOfYear - 1)
        val eqtime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val declDeg = Math.toDegrees(decl)

        // Solar Noon (Dhuhr)
        val solarNoonUtcMinutes = 720 - 4 * lon - eqtime
        val dhuhrMinutes = solarNoonUtcMinutes + tzOffset * 60

        // Sunrise & Sunset angle (0.833 degrees refraction)
        val sunAngle = 0.833
        val haSunrise = calculateHourAngle(lat, declDeg, 90.0 + sunAngle)

        val sunriseMinutes = dhuhrMinutes - haSunrise * 4
        val sunsetMinutes = dhuhrMinutes + haSunrise * 4

        // Fajr
        val haFajr = calculateHourAngle(lat, declDeg, 90.0 + method.fajrAngle)
        val fajrMinutes = dhuhrMinutes - haFajr * 4

        // Asr (Shafi standard: shadow factor = 1)
        val asrAngle = Math.toDegrees(atan(1.0 + tan(Math.toRadians(abs(lat - declDeg)))))
        val haAsr = calculateHourAngle(lat, declDeg, 90.0 - asrAngle)
        val asrMinutes = dhuhrMinutes + haAsr * 4

        // Maghrib = Sunset + 2 minutes safety buffer
        val maghribMinutes = sunsetMinutes + 2

        // Isha
        val ishaMinutes = if (method == CalculationMethod.MAKKAH) {
            maghribMinutes + 90 // Umm Al Qura adds 90 mins to Maghrib
        } else {
            val haIsha = calculateHourAngle(lat, declDeg, 90.0 + method.ishaAngle)
            dhuhrMinutes + haIsha * 4
        }

        val nowCal = Calendar.getInstance()
        val currentMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        val rawTimes = listOf(
            PrayerType.FAJR to fajrMinutes.roundToInt(),
            PrayerType.SUNRISE to sunriseMinutes.roundToInt(),
            PrayerType.DHUHR to dhuhrMinutes.roundToInt(),
            PrayerType.ASR to asrMinutes.roundToInt(),
            PrayerType.MAGHRIB to maghribMinutes.roundToInt(),
            PrayerType.ISHA to ishaMinutes.roundToInt()
        )

        // Find next prayer index
        var nextFound = false
        var nextType: PrayerType? = null
        for ((type, minutes) in rawTimes) {
            if (minutes > currentMinutesOfDay) {
                nextType = type
                nextFound = true
                break
            }
        }
        if (!nextFound) {
            nextType = PrayerType.FAJR // Next is tomorrow's Fajr
        }

        return rawTimes.map { (type, minutes) ->
            val normalizedMins = (minutes % 1440 + 1440) % 1440
            val h24 = normalizedMins / 60
            val m = normalizedMins % 60
            val formatted = formatTime12H(h24, m)
            val isPassed = minutes < currentMinutesOfDay
            val isNext = (type == nextType)

            PrayerTimeEntry(
                type = type,
                timeFormatted = formatted,
                hour24 = h24,
                minute = m,
                isNext = isNext,
                isPassed = isPassed,
                adhanEnabled = type != PrayerType.SUNRISE // Adhan for all prayers except Sunrise
            )
        }
    }

    private fun calculateHourAngle(lat: Double, declDeg: Double, angle: Double): Double {
        val latRad = Math.toRadians(lat)
        val declRad = Math.toRadians(declDeg)
        val angleRad = Math.toRadians(angle)

        val cosHA = (cos(angleRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        val clampedCosHA = cosHA.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clampedCosHA))
    }

    fun formatTime12H(hour24: Int, minute: Int): String {
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val amPm = if (hour24 < 12) "ص" else "م"
        return "%02d:%02d %s".format(h12, minute, amPm)
    }
}
