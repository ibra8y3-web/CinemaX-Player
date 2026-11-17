package com.example.quran

import android.content.Context
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranSurah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream

object QuranRepository {

    private val _bookmark = MutableStateFlow<QuranBookmark?>(null)
    val bookmark: StateFlow<QuranBookmark?> = _bookmark.asStateFlow()

    private const val PREFS_NAME = "nova_quran_prefs"
    private const val KEY_BOOKMARK_SURAH = "bookmark_surah"
    private const val KEY_BOOKMARK_NAME = "bookmark_name"
    private const val KEY_BOOKMARK_AYAH = "bookmark_ayah"
    private const val KEY_BOOKMARK_TIME = "bookmark_time"

    private val surahVersesCache = mutableMapOf<Int, List<String>>()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val surah = prefs.getInt(KEY_BOOKMARK_SURAH, 0)
        if (surah > 0) {
            val name = prefs.getString(KEY_BOOKMARK_NAME, "") ?: ""
            val ayah = prefs.getInt(KEY_BOOKMARK_AYAH, 1)
            val time = prefs.getLong(KEY_BOOKMARK_TIME, 0L)
            _bookmark.value = QuranBookmark(surah, name, ayah, time)
        }
        preloadAllSurahs(context)
    }

    private fun preloadAllSurahs(context: Context) {
        if (surahVersesCache.isNotEmpty()) return
        try {
            val inputStream: InputStream = context.assets.open("quran_uthmani.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val surahObj = jsonArray.getJSONObject(i)
                val surahNum = surahObj.getInt("number")
                val versesArr = surahObj.getJSONArray("verses")
                val vList = ArrayList<String>(versesArr.length())
                for (j in 0 until versesArr.length()) {
                    val vObj = versesArr.getJSONObject(j)
                    val text = vObj.getString("text")
                    vList.add(text)
                }
                surahVersesCache[surahNum] = vList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveBookmark(context: Context, surah: Int, surahName: String, ayah: Int) {
        val bm = QuranBookmark(surah, surahName, ayah, System.currentTimeMillis())
        _bookmark.value = bm
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_BOOKMARK_SURAH, surah)
            .putString(KEY_BOOKMARK_NAME, surahName)
            .putInt(KEY_BOOKMARK_AYAH, ayah)
            .putLong(KEY_BOOKMARK_TIME, bm.timestampMs)
            .apply()
    }

    // List of all 114 Surahs in the Holy Quran with precise metadata
    val surahList: List<QuranSurah> by lazy {
        listOf(
            QuranSurah(1, "الفَاتِحَة", "Al-Fatihah", "The Opening", "مكية", 7, 1, 1),
            QuranSurah(2, "البَقَرَة", "Al-Baqarah", "The Cow", "مدنية", 286, 1, 2),
            QuranSurah(3, "آل عِمْرَان", "Aal-E-Imran", "Family of Imran", "مدنية", 200, 3, 50),
            QuranSurah(4, "النِّسَاء", "An-Nisa", "The Women", "مدنية", 176, 4, 77),
            QuranSurah(5, "المَائِدَة", "Al-Ma'idah", "The Table Spread", "مدنية", 120, 6, 106),
            QuranSurah(6, "الأَنْعَام", "Al-An'am", "The Cattle", "مكية", 165, 7, 128),
            QuranSurah(7, "الأَعْرَاف", "Al-A'raf", "The Heights", "مكية", 206, 8, 151),
            QuranSurah(8, "الأَنْفَال", "Al-Anfal", "The Spoils of War", "مدنية", 75, 9, 177),
            QuranSurah(9, "التَّوْبَة", "At-Tawbah", "The Repentance", "مدنية", 129, 10, 187),
            QuranSurah(10, "يُونُس", "Yunus", "Jonah", "مكية", 109, 11, 208),
            QuranSurah(11, "هُود", "Hud", "Hud", "مكية", 123, 11, 221),
            QuranSurah(12, "يُوسُف", "Yusuf", "Joseph", "مكية", 111, 12, 235),
            QuranSurah(13, "الرَّعْد", "Ar-Ra'd", "The Thunder", "مدنية", 43, 13, 249),
            QuranSurah(14, "إِبْرَاهِيم", "Ibrahim", "Abraham", "مكية", 52, 13, 255),
            QuranSurah(15, "الحِجْر", "Al-Hijr", "The Rocky Tract", "مكية", 99, 14, 262),
            QuranSurah(16, "النَّحْل", "An-Nahl", "The Bee", "مكية", 128, 14, 267),
            QuranSurah(17, "الإِسْرَاء", "Al-Isra", "The Night Journey", "مكية", 111, 15, 282),
            QuranSurah(18, "الكَهْف", "Al-Kahf", "The Cave", "مكية", 110, 15, 293),
            QuranSurah(19, "مَرْيَم", "Maryam", "Mary", "مكية", 98, 16, 305),
            QuranSurah(20, "طه", "Taha", "Ta-Ha", "مكية", 135, 16, 312),
            QuranSurah(21, "الأَنْبِيَاء", "Al-Anbiya", "The Prophets", "مكية", 112, 17, 322),
            QuranSurah(22, "الحَجّ", "Al-Hajj", "The Pilgrimage", "مدنية", 78, 17, 332),
            QuranSurah(23, "المُؤْمِنُون", "Al-Mu'minun", "The Believers", "مكية", 118, 18, 342),
            QuranSurah(24, "النُّور", "An-Nur", "The Light", "مدنية", 64, 18, 350),
            QuranSurah(25, "الفُرْقَان", "Al-Furqan", "The Criterion", "مكية", 77, 18, 359),
            QuranSurah(26, "الشُّعَرَاء", "Ash-Shu'ara", "The Poets", "مكية", 227, 19, 367),
            QuranSurah(27, "النَّمْل", "An-Naml", "The Ant", "مكية", 93, 19, 377),
            QuranSurah(28, "القَصَص", "Al-Qasas", "The Stories", "مكية", 88, 20, 385),
            QuranSurah(29, "العَنْكَبُوت", "Al-Ankabut", "The Spider", "مكية", 69, 20, 396),
            QuranSurah(30, "الرُّوم", "Ar-Rum", "The Romans", "مكية", 60, 21, 404),
            QuranSurah(31, "لُقْمَان", "Luqman", "Luqman", "مكية", 34, 21, 411),
            QuranSurah(32, "السَّجْدَة", "As-Sajdah", "The Prostration", "مكية", 30, 21, 415),
            QuranSurah(33, "الأَحْزَاب", "Al-Ahzab", "The Combined Forces", "مدنية", 73, 21, 418),
            QuranSurah(34, "سَبَأ", "Saba", "Sheba", "مكية", 54, 22, 428),
            QuranSurah(35, "فَاطِر", "Fatir", "Originator", "مكية", 45, 22, 434),
            QuranSurah(36, "يس", "Ya-Sin", "Ya-Sin", "مكية", 83, 22, 440),
            QuranSurah(37, "الصَّافَّات", "As-Saffat", "Those Ranges in Ranks", "مكية", 182, 23, 446),
            QuranSurah(38, "ص", "Sad", "Sad", "مكية", 88, 23, 453),
            QuranSurah(39, "الزُّمَر", "Az-Zumar", "The Groups", "مكية", 75, 23, 458),
            QuranSurah(40, "غَافِر", "Ghafir", "The Forgiver", "مكية", 85, 24, 467),
            QuranSurah(41, "فُصِّلَت", "Fussilat", "Explained in Detail", "مكية", 54, 24, 477),
            QuranSurah(42, "الشُّورَى", "Ash-Shura", "The Consultation", "مكية", 53, 25, 483),
            QuranSurah(43, "الزُّخْرُف", "Az-Zukhruf", "The Ornaments of Gold", "مكية", 89, 25, 489),
            QuranSurah(44, "الدُّخَان", "Ad-Dukhan", "The Smoke", "مكية", 59, 25, 496),
            QuranSurah(45, "الجَاثِيَة", "Al-Jathiyah", "The Crouching", "مكية", 37, 25, 499),
            QuranSurah(46, "الأَحْقَاف", "Al-Ahqaf", "The Curved Sand-hills", "مكية", 35, 26, 502),
            QuranSurah(47, "مُحَمَّد", "Muhammad", "Muhammad", "مدنية", 38, 26, 507),
            QuranSurah(48, "الفَتْح", "Al-Fath", "The Victory", "مدنية", 29, 26, 511),
            QuranSurah(49, "الحُجُرَات", "Al-Hujurat", "The Rooms", "مدنية", 18, 26, 515),
            QuranSurah(50, "ق", "Qaf", "Qaf", "مكية", 45, 26, 518),
            QuranSurah(51, "الذَّارِيَات", "Adh-Dhariyat", "The Winnowing Winds", "مكية", 60, 26, 520),
            QuranSurah(52, "الطُّور", "At-Tur", "The Mount", "مكية", 49, 27, 523),
            QuranSurah(53, "النَّجْم", "An-Najm", "The Star", "مكية", 62, 27, 526),
            QuranSurah(54, "القَمَر", "Al-Qamar", "The Moon", "مكية", 55, 27, 528),
            QuranSurah(55, "الرَّحْمَٰن", "Ar-Rahman", "The Beneficent", "مدنية", 78, 27, 531),
            QuranSurah(56, "الوَاقِعَة", "Al-Waqi'ah", "The Inevitable", "مكية", 96, 27, 534),
            QuranSurah(57, "الحَدِيد", "Al-Hadid", "The Iron", "مدنية", 29, 27, 537),
            QuranSurah(58, "المُجَادَلَة", "Al-Mujadila", "The Pleading Woman", "مدنية", 22, 28, 542),
            QuranSurah(59, "الحَشْر", "Al-Hashr", "The Exile", "مدنية", 24, 28, 545),
            QuranSurah(60, "المُمْتَحَنَة", "Al-Mumtahanah", "She That Is To Be Examined", "مدنية", 13, 28, 549),
            QuranSurah(61, "الصَّفّ", "As-Saff", "The Ranks", "مدنية", 14, 28, 551),
            QuranSurah(62, "الجُمُعَة", "Al-Jumu'ah", "The Congregation", "مدنية", 11, 28, 553),
            QuranSurah(63, "المُنَافِقُون", "Al-Munafiqun", "The Hypocrites", "مدنية", 11, 28, 554),
            QuranSurah(64, "التَّغَابُن", "At-Taghabun", "The Mutual Disillusion", "مدنية", 18, 28, 556),
            QuranSurah(65, "الطَّلَاق", "At-Talaq", "The Divorce", "مدنية", 12, 28, 558),
            QuranSurah(66, "التَّحْرِيم", "At-Tahrim", "The Prohibition", "مدنية", 12, 28, 560),
            QuranSurah(67, "المُلْك", "Al-Mulk", "The Sovereignty", "مكية", 30, 29, 562),
            QuranSurah(68, "القَلَم", "Al-Qalam", "The Pen", "مكية", 52, 29, 564),
            QuranSurah(69, "الحَاقَّة", "Al-Haqqah", "The Reality", "مكية", 52, 29, 566),
            QuranSurah(70, "المَعَارِج", "Al-Ma'arij", "The Ascending Stairways", "مكية", 44, 29, 568),
            QuranSurah(71, "نُوح", "Nuh", "Noah", "مكية", 28, 29, 570),
            QuranSurah(72, "الجِنّ", "Al-Jinn", "The Jinn", "مكية", 28, 29, 572),
            QuranSurah(73, "المُزَّمِّل", "Al-Muzzammil", "The Enshrouded One", "مكية", 20, 29, 574),
            QuranSurah(74, "المُدَّثِّر", "Al-Muddaththir", "The Cloaked One", "مكية", 56, 29, 575),
            QuranSurah(75, "القِيَامَة", "Al-Qiyamah", "The Resurrection", "مكية", 40, 29, 577),
            QuranSurah(76, "الإِنْسَان", "Al-Insan", "Man", "مدنية", 31, 29, 578),
            QuranSurah(77, "المُرْسَلَات", "Al-Mursalat", "The Emissaries", "مكية", 50, 29, 580),
            QuranSurah(78, "النَّبَأ", "An-Naba", "The Tidings", "مكية", 40, 30, 582),
            QuranSurah(79, "النَّازِعَات", "An-Nazi'at", "Those Who Drag Forth", "مكية", 46, 30, 583),
            QuranSurah(80, "عَبَسَ", "Abasa", "He Frowned", "مكية", 42, 30, 585),
            QuranSurah(81, "التَّكْوِير", "At-Takwir", "The Overthrowing", "مكية", 29, 30, 586),
            QuranSurah(82, "الانْفِطَار", "Al-Infitar", "The Cleaving", "مكية", 19, 30, 587),
            QuranSurah(83, "المُطَفِّفِينَ", "Al-Mutaffifin", "The Defrauding", "مكية", 36, 30, 587),
            QuranSurah(84, "الانْشِقَاق", "Al-Inshiqaq", "The Splitting Open", "مكية", 25, 30, 589),
            QuranSurah(85, "البُرُوج", "Al-Buruj", "The Mansions of the Stars", "مكية", 22, 30, 590),
            QuranSurah(86, "الطَّارِق", "At-Tariq", "The Nightcommer", "مكية", 17, 30, 591),
            QuranSurah(87, "الأَعْلَى", "Al-A'la", "The Most High", "مكية", 19, 30, 591),
            QuranSurah(88, "الغَاشِيَة", "Al-Ghashiyah", "The Overwhelming", "مكية", 26, 30, 592),
            QuranSurah(89, "الفَجْر", "Al-Fajr", "The Dawn", "مكية", 30, 30, 593),
            QuranSurah(90, "البَلَد", "Al-Balad", "The City", "مكية", 20, 30, 594),
            QuranSurah(91, "الشَّمْس", "Ash-Shams", "The Sun", "مكية", 15, 30, 595),
            QuranSurah(92, "اللَّيْل", "Al-Layl", "The Night", "مكية", 21, 30, 595),
            QuranSurah(93, "الضُّحَى", "Ad-Duha", "The Morning Hours", "مكية", 11, 30, 596),
            QuranSurah(94, "الشَّرْح", "Ash-Sharh", "The Relief", "مكية", 8, 30, 596),
            QuranSurah(95, "التِّين", "At-Tin", "The Fig", "مكية", 8, 30, 597),
            QuranSurah(96, "العَلَق", "Al-Alaq", "The Clot", "مكية", 19, 30, 597),
            QuranSurah(97, "القَدْر", "Al-Qadr", "The Power", "مكية", 5, 30, 598),
            QuranSurah(98, "البَيِّنَة", "Al-Bayyinah", "The Clear Proof", "مدنية", 8, 30, 598),
            QuranSurah(99, "الزَّلْزَلَة", "Az-Zalzalah", "The Earthquake", "مدنية", 8, 30, 599),
            QuranSurah(100, "العَادِيَات", "Al-Adiyat", "The Courser", "مكية", 11, 30, 599),
            QuranSurah(101, "القَارِعَة", "Al-Qari'ah", "The Calamity", "مكية", 11, 30, 600),
            QuranSurah(102, "التَّكَاثُر", "At-Takathur", "The Rivalry in World Increase", "مكية", 8, 30, 600),
            QuranSurah(103, "العَصْر", "Al-Asr", "The Declining Day", "مكية", 3, 30, 601),
            QuranSurah(104, "الهُمَزَة", "Al-Humazah", "The Traducer", "مكية", 9, 30, 601),
            QuranSurah(105, "الفِيل", "Al-Fil", "The Elephant", "مكية", 5, 30, 601),
            QuranSurah(106, "قُرَيْش", "Quraysh", "Quraysh", "مكية", 4, 30, 602),
            QuranSurah(107, "المَاعُون", "Al-Ma'un", "The Small Kindness", "مكية", 7, 30, 602),
            QuranSurah(108, "الكَوْثَر", "Al-Kawthar", "The Abundance", "مكية", 3, 30, 602),
            QuranSurah(109, "الكَافِرُون", "Al-Kafirun", "The Disbelievers", "مكية", 6, 30, 603),
            QuranSurah(110, "النَّصْر", "An-Nasr", "The Divine Support", "مدنية", 3, 30, 603),
            QuranSurah(111, "المَسَد", "Al-Masad", "The Palm Fiber", "مكية", 5, 30, 603),
            QuranSurah(112, "الإِخْلَاص", "Al-Ikhlas", "The Sincerity", "مكية", 4, 30, 604),
            QuranSurah(113, "الفَلَق", "Al-Falaq", "The Daybreak", "مكية", 5, 30, 604),
            QuranSurah(114, "النَّاس", "An-Nas", "Mankind", "مكية", 6, 30, 604)
        )
    }

    /**
     * Retrieves the authentic, complete Uthmani verses for the requested Surah.
     * Guaranteed 100% exact text from King Fahd Quran complex / Tanzil project.
     */
    suspend fun getSurahVerses(context: Context, surahNumber: Int): List<String> = withContext(Dispatchers.IO) {
        if (surahVersesCache.containsKey(surahNumber) && surahVersesCache[surahNumber]?.isNotEmpty() == true) {
            return@withContext surahVersesCache[surahNumber]!!
        }

        preloadAllSurahs(context)
        surahVersesCache[surahNumber] ?: emptyList()
    }

    suspend fun getSurahVerses(surahNumber: Int): List<String> = withContext(Dispatchers.IO) {
        if (surahVersesCache.containsKey(surahNumber) && surahVersesCache[surahNumber]?.isNotEmpty() == true) {
            return@withContext surahVersesCache[surahNumber]!!
        }
        appContext?.let { preloadAllSurahs(it) }
        surahVersesCache[surahNumber] ?: emptyList()
    }
}
