package com.example.radio

/**
 * أنماط توافق وتجاوز باقات الموبايل والشبكات المقيدة (Network Bundle Camouflage)
 * تتيح تشغيل محطات الراديو حتى في حال عدم وجود باقة إنترنت عامة، عن طريق
 * تمويه ترويسات بروتوكول HTTP (User-Agent, Referer, X-Requested-With)
 * لتطابق حزم تطبيقات السوشيال ميديا، الموسيقى، أو اليوتيوب المفتوحة مجاناً في باقة المستخدم.
 */
enum class NetworkBundleMode(
    val id: String,
    val nameAr: String,
    val shortLabel: String,
    val icon: String,
    val userAgent: String,
    val customHeaders: Map<String, String>,
    val description: String
) {
    NORMAL(
        id = "normal",
        nameAr = "الإنترنت العام (كل الشبكات)",
        shortLabel = "نت عام",
        icon = "🌐",
        userAgent = "Mozilla/5.0 (Linux; Android 14; Mobile; NovaRadio/3.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
        customHeaders = mapOf(
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        ),
        description = "الوضع الافتراضي لشبكات الواي فاي وباقات الإنترنت المفتوحة بالكامل."
    ),

    SOCIAL_BUNDLE(
        id = "social",
        nameAr = "باقات السوشيال (فيسبوك وإنستغرام)",
        shortLabel = "باقة سوشيال",
        icon = "📱",
        userAgent = "Mozilla/5.0 (Linux; Android 14; Mobile; FB_IAB/FB4A;FBAV/450.0.0.34.109;[FBAN/EMA;FBBV/573210432;FBDV/SM-G998B;FBMD/Samsung;FBSN/Android;FBSV/14;FBSS/3;])",
        customHeaders = mapOf(
            "Referer" to "https://m.facebook.com/",
            "Origin" to "https://m.facebook.com",
            "X-Requested-With" to "com.facebook.katana",
            "Accept" to "*/*"
        ),
        description = "تمويه ترويسات البث لتعمل بسلاسة على باقات وتطبيقات التواصل الاجتماعي (فيسبوك/إنستا)."
    ),

    MUSIC_AUDIO_BUNDLE(
        id = "music",
        nameAr = "باقات الميوزك (أنغامي، ساوندكلاود، سبوتيفاي)",
        shortLabel = "باقة ميوزك",
        icon = "🎵",
        userAgent = "SoundCloud/2024.01.12-release (Android 14; Mobile; Samsung SM-S918B; en_US)",
        customHeaders = mapOf(
            "Referer" to "https://soundcloud.com/",
            "Origin" to "https://soundcloud.com",
            "X-Requested-With" to "com.soundcloud.android",
            "Accept" to "audio/mpeg, audio/aac, audio/*;q=0.9"
        ),
        description = "مخصص لشركات الاتصالات التي تقدم باقات موسيقية وصوتية مخصصة بدون استهلاك للنت العام."
    ),

    YOUTUBE_VIDEO_BUNDLE(
        id = "youtube",
        nameAr = "باقات يوتيوب والفيديو (YouTube Pack)",
        shortLabel = "باقة يوتيوب",
        icon = "▶️",
        userAgent = "com.google.android.youtube/19.10.38 (Linux; U; Android 14; Mobile; SM-A546B) gzip",
        customHeaders = mapOf(
            "Referer" to "https://www.youtube.com/",
            "Origin" to "https://www.youtube.com",
            "X-Requested-With" to "com.google.android.youtube"
        ),
        description = "توجيه طلبات البث كحزم وسائط يوتيوب لتعمل مع باقات الفيديو والمشاهدة."
    ),

    WHATSAPP_CHAT_BUNDLE(
        id = "whatsapp",
        nameAr = "باقات واتساب والتراسل المباشر",
        shortLabel = "باقة واتساب",
        icon = "💬",
        userAgent = "WhatsApp/2.24.4.76 A",
        customHeaders = mapOf(
            "User-Agent" to "WhatsApp/2.24.4.76 A",
            "X-Requested-With" to "com.whatsapp",
            "Accept" to "*/*"
        ),
        description = "توافق مخصص للباقات المقيدة فقط بخدمات المراسلة والواتساب."
    ),

    ULTRA_SAVER(
        id = "ultra_saver",
        nameAr = "الوضع فائق التوفير والشبكات الضعيفة",
        shortLabel = "فائق التوفير",
        icon = "⚡",
        userAgent = "Mozilla/5.0 (Android 14; Mobile; rv:122.0) Gecko/122.0 Firefox/122.0",
        customHeaders = mapOf(
            "Accept-Encoding" to "gzip, deflate",
            "Connection" to "close"
        ),
        description = "ضغط إضافي وتخفيض الذاكرة المؤقتة ليعمل الراديو بأقل استهلاك ممكن للبيانات."
    );

    companion object {
        fun fromId(id: String?): NetworkBundleMode {
            return values().firstOrNull { it.id == id } ?: NORMAL
        }
    }
}
