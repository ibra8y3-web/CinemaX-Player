package com.example.ai

object AiVideoCategorizer {

    fun categorize(
        title: String,
        folderName: String,
        durationMs: Long,
        width: Int,
        height: Int
    ): String {
        val lowerTitle = title.lowercase()
        val lowerFolder = folderName.lowercase()
        val seconds = durationMs / 1000

        // 1. Quran & Religious content detection
        if (matchesAny(lowerTitle, "quran", "قرآن", "سورة", "تلاوة", "اذكار", "أذكار", "دعاء", "خطبة", "تفسير", "صلاة")) {
            return "قرآن وتلاوات"
        }

        // 2. Vertical Shorts / Reels
        val isVertical = height > 0 && width > 0 && height > width
        if (isVertical && seconds in 1..90) {
            return "شورتس وريلز"
        }
        if (matchesAny(lowerTitle, "short", "reel", "tiktok", "status", "stories", "حالة")) {
            return "شورتس وريلز"
        }

        // 3. Movies & Long Features
        if (seconds > 2400) { // > 40 minutes
            return "أفلام وسينما"
        }
        if (matchesAny(lowerTitle, "movie", "film", "فيلم", "مسلسل", "season", "episode", "حلقة")) {
            return "أفلام وسينما"
        }

        // 4. Podcasts & Talks
        if (matchesAny(lowerTitle, "podcast", "بودكاست", "لقاء", "حوار", "ندوة", "talk", "interview") ||
            matchesAny(lowerFolder, "podcast", "بودكاست")) {
            return "بودكاست ولقاءات"
        }

        // 5. Tutorials & Education
        if (matchesAny(lowerTitle, "tutorial", "شرح", "كورس", "course", "learn", "درس", "تعليم", "how to") ||
            matchesAny(lowerFolder, "courses", "تعليم", "شروحات")) {
            return "شروحات وتعليم"
        }

        // 6. Camera & Family Memories
        if (matchesAny(lowerFolder, "dcim", "camera", "كاميرا") || lowerTitle.startsWith("vid_") || lowerTitle.startsWith("rec_")) {
            return "تصوير الكاميرا"
        }

        // 7. Social Downloads
        if (matchesAny(lowerFolder, "whatsapp", "telegram", "download", "facebook", "instagram", "تنزيلات")) {
            return "تنزيلات وتواصل"
        }

        return "مقاطع عامة"
    }

    private fun matchesAny(text: String, vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true)) return true
        }
        return false
    }

    fun getCategoryColorHex(category: String): Long {
        return when (category) {
            "قرآن وتلاوات" -> 0xFF2EE6A6 // Emerald
            "شورتس وريلز" -> 0xFFFF4081 // Pink/Neon
            "أفلام وسينما" -> 0xFF7C4DFF // Deep Violet
            "بودكاست ولقاءات" -> 0xFFFFC857 // Warm Amber
            "شروحات وتعليم" -> 0xFF5B8CFF // Electric Cyan
            "تصوير الكاميرا" -> 0xFFFF6D00 // Sunset Orange
            "تنزيلات وتواصل" -> 0xFF22E3FF // Turquoise
            else -> 0xFF9E9E9E
        }
    }
}

enum class AiEnhanceMode(val titleAr: String, val titleEn: String) {
    STANDARD("قياسي (طبيعي)", "Standard (Normal)"),
    AMOLED_PURE_BLACK("شاشات أموليد والتباين الأسود الفائق (AMOLED Black)", "AMOLED Pure Black"),
    CINEMA_WARM("دفء سينمائي هوليوودي (Cinema Warm)", "Cinema Warm Filter"),
    NIGHT_SHIELD("حماية العين الليلية (Night Eye)", "Night Eye Comfort Shield"),
    VIVID_SUPER("وضوح فائق ذكي (AI Vivid)", "AI Vivid Super-Clarity"),
    NOIR_DRAMATIC("أبيض وأسود درامي كلاسيكي (Noir B&W)", "Cinematic Noir B&W"),
    CYBERPUNK_NEON("نيون وسايبربانك مستقبلي (Cyberpunk Neon)", "Cyberpunk Neon"),
    VINTAGE_SEPIA("سيبيا كلاسيكي قديم (Vintage 70s)", "Vintage Film Sepia"),
    COLD_SCIFI("خيال علمي فضائي بارد (Cold Sci-Fi)", "Cold Sci-Fi Space"),
    EMERALD_NATURE("طبيعة زمردية خضراء (Vibrant Nature)", "Emerald Vibrant Nature"),
    SUNSET_GOLD("شروق وغروب ذهبي (Sunset Glow)", "Sunset Golden Glow"),
    MATTE_CINEMA("مات سينمائي هادئ (Matte Cinema)", "Matte Cinema"),
    DEEP_BLUES("أزرق عميق سينمائي (Deep Ocean Blues)", "Deep Ocean Blues"),
    ROSE_GOLD("روز جولد أنيق (Rose Gold Elegance)", "Rose Gold Elegance"),
    MATRIX_GREEN("أخضر ماتركس رقمي (Matrix Digital)", "Matrix Digital Green"),
    AUTUMN_ORANGE("خريفي دافئ برتقالي (Autumn Warmth)", "Autumn Warmth"),
    ARCTIC_FROST("صقيع قطبي ناصع (Arctic Frost)", "Arctic Frost"),
    CHOCOLATE_BROWN("شوكولاتة دافئة كلاسيكية (Chocolate Warm)", "Chocolate Warm"),
    NEON_PURPLE("بنفسجي أرجواني ساطع (Neon Purple Glow)", "Neon Purple Glow"),
    VINTAGE_8MM("كاميرا 8 مم قديمة (Vintage 8mm Film)", "Vintage 8mm Film"),
    HDR_ULTRA_BOOST("تعزيز HDR فائق القوة (HDR Ultra Boost)", "HDR Ultra Boost"),
    TEAL_AND_ORANGE("تيل وأورانج هوليوودي (Teal & Orange)", "Teal & Orange Hollywood"),
    CYAN_DREAM("حلم السيان السماوي (Cyan Dream)", "Cyan Dream"),
    MOONLIGHT_SILVER("ضوء القمر الفضي الهادئ (Moonlight Silver)", "Moonlight Silver"),
    VAMPIRE_BLOOD_RED("أحمر درامي داكن (Cinematic Blood Red)", "Cinematic Blood Red")
}
