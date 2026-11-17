package com.example.radio

import com.example.data.model.RadioCategory
import com.example.data.model.RadioStation

object RadioCatalog {

    val stations: List<RadioStation> by lazy {
        listOf(
            // ==========================================
            // قسم 1: إذاعات فرنسا وأوروبا (France & Europe Live)
            // ==========================================
            RadioStation(
                id = "fr_cherie_fm",
                titleAr = "Chérie FM (فرنسا - باريس)",
                reciterOrDesc = "أشهر المحطات الفرنسية الرومانسية وأغاني البوب الفرنسية والعالمية",
                streamUrl = "https://cdnradio.streamakaci.com/cheriefm1.mp3",
                backupUrl = "https://cdnradio.streamakaci.com/cheriefm1.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "radio"
            ),
            RadioStation(
                id = "fr_europe1",
                titleAr = "Europe 1 (فرنسا - باريس المباشر)",
                reciterOrDesc = "أشهر المحطات الإخبارية والثقافية والحوارية المباشرة في فرنسا",
                streamUrl = "https://europe1.lmn.fm/europe1.mp3",
                backupUrl = "https://stream.radiofrance.fr/franceinter/franceinter.m3u8",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "radio"
            ),
            RadioStation(
                id = "fr_rfm_classics",
                titleAr = "RFM Pop-Rock & Classics (فرنسا)",
                reciterOrDesc = "أرقى روائع البوب والروك والكلاسيكيات الفرنسية والعالمية",
                streamUrl = "https://stream.rfm.fr/rfm-wr10.aac",
                backupUrl = "https://stream.radiofrance.fr/fip/fip.m3u8",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "music_note"
            ),
            RadioStation(
                id = "fr_skyrock",
                titleAr = "Skyrock Paris (Rap & R'n'B)",
                reciterOrDesc = "الإذاعة الفرنسية الأشهر للراب والهيب هوب وبرامج الشباب المباشرة",
                streamUrl = "https://icecast.skyrock.net/s/natio_mp3_128k",
                backupUrl = "https://icecast.skyrock.net/s/natio_mp3_128k",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "headphones"
            ),
            RadioStation(
                id = "fr_france_inter",
                titleAr = "France Inter (إذاعة فرنسا العامة)",
                reciterOrDesc = "الإذاعة الوطنية الرسمية لفرنسا - برامج ثقافية وأخبار وحوارات",
                streamUrl = "https://icecast.radiofrance.fr/franceinter-midfi.mp3",
                backupUrl = "https://icecast.radiofrance.fr/franceinter-midfi.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "mic"
            ),
            RadioStation(
                id = "fr_france_info",
                titleAr = "France Info (أخبار فرنسا والعالم 24/7)",
                reciterOrDesc = "أخبار متواصلة باللغة الفرنسية على مدار الساعة وتغطيات حية",
                streamUrl = "https://icecast.radiofrance.fr/franceinfo-midfi.mp3",
                backupUrl = "https://icecast.radiofrance.fr/franceinfo-midfi.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "public"
            ),
            RadioStation(
                id = "fr_rtl",
                titleAr = "RTL France (راديو باريس)",
                reciterOrDesc = "إذاعة RTL الفرنسية العريقة - أخبار، ترفيه، وتحليلات يومية",
                streamUrl = "https://live.m6radio.quortex.io/webM89Hc99XApzgfhXNX8ASN5/grouprtl/national/short/audio-64000/index.m3u8",
                backupUrl = "https://europe1.lmn.fm/europe1.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "radio"
            ),
            RadioStation(
                id = "fr_rfi_monde",
                titleAr = "RFI Monde (إذاعة فرنسا الدولية)",
                reciterOrDesc = "بث مباشر عالمي من باريس بأخبار العالم والبرامج الدولية الثقافية",
                streamUrl = "https://rfimonde64k.ice.infomaniak.ch/rfimonde-64.mp3",
                backupUrl = "https://rfimonde64k.ice.infomaniak.ch/rfimonde-64.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 64,
                country = "France",
                iconName = "public"
            ),
            RadioStation(
                id = "fr_fun_radio",
                titleAr = "Fun Radio France (Le Son Dance)",
                reciterOrDesc = "محطة موسيقى الدانس والمهرجانات الأوروبية الحيوية",
                streamUrl = "https://icecast.funradio.fr/fun-1-44-128",
                backupUrl = "https://icecast.funradio.fr/fun-1-44-128",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "celebration"
            ),
            RadioStation(
                id = "fr_radio_fg",
                titleAr = "Radio FG Paris (Deep & Electronic)",
                reciterOrDesc = "أرقى معزوفات الموسيقى الإلكترونية والهاوس من باريس",
                streamUrl = "https://radiofg.impek.com/fg",
                backupUrl = "https://radiofg.impek.com/fg",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "headphones"
            ),
            RadioStation(
                id = "fr_fip",
                titleAr = "FIP Radio France (جاز ومنوعات عالمية)",
                reciterOrDesc = "موسيقى بدون انقطاع - جاز، بلوز، ريجي، ومنوعات فرنسية راقية",
                streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
                backupUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
                category = RadioCategory.FRANCE_GLOBAL,
                bitrateKbps = 128,
                country = "France",
                iconName = "spa"
            ),

            // ==========================================
            // قسم 2: إذاعات تركيا (Turkish Live Radios)
            // ==========================================
            RadioStation(
                id = "tr_kral_fm",
                titleAr = "Kral FM Türkiye (ملك الموسيقى التركية)",
                reciterOrDesc = "أشهر إذاعة في تركيا - روائع الموسيقى الشعبية والأرابيسك التركي الأصيل",
                streamUrl = "https://dygedge2.radyotvonline.net/kralfm/playlist.m3u8",
                backupUrl = "https://dygedge2.radyotvonline.net/kralfm/playlist.m3u8",
                category = RadioCategory.TURKISH_RADIO,
                bitrateKbps = 128,
                country = "Turkey",
                iconName = "music_note"
            ),
            RadioStation(
                id = "tr_power_fm",
                titleAr = "Power Pop Türkiye (إسطنبول)",
                reciterOrDesc = "إذاعة الموسيقى العالمية والأغاني الإنجليزية والتركية الأكثر استماعاً",
                streamUrl = "https://listen.powerapp.com.tr/powerpop/128/chunks.m3u8",
                backupUrl = "https://listen.powerapp.com.tr/powerpop/128/chunks.m3u8",
                category = RadioCategory.TURKISH_RADIO,
                bitrateKbps = 128,
                country = "Turkey",
                iconName = "celebration"
            ),
            RadioStation(
                id = "tr_joy_turk",
                titleAr = "Joy Türk FM (إسطنبول)",
                reciterOrDesc = "بث الإذاعة التركية الأشهر لأرقى الأغاني التركية",
                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/JOY_TURK_SC",
                backupUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/JOY_TURK_SC",
                category = RadioCategory.TURKISH_RADIO,
                bitrateKbps = 128,
                country = "Turkey",
                iconName = "radio"
            ),
            RadioStation(
                id = "tr_alem_fm",
                titleAr = "Alem FM (عالم الموسيقى التركية)",
                reciterOrDesc = "أول إذاعة خاصة في تركيا ومن الأكثر شعبية",
                streamUrl = "https://turkmedya.radyotvonline.net/alemfmaac",
                backupUrl = "https://turkmedya.radyotvonline.net/alemfmaac",
                category = RadioCategory.TURKISH_RADIO,
                bitrateKbps = 128,
                country = "Turkey",
                iconName = "music_note"
            ),
            RadioStation(
                id = "tr_damarturk",
                titleAr = "DamarTürk FM (الطرب التركي الأصيل)",
                reciterOrDesc = "أجمل الأغاني الشعبية والشجية باللغة التركية",
                streamUrl = "https://live.radyositesihazir.com:10997/stream?type=http",
                backupUrl = "https://live.radyositesihazir.com:10997/stream",
                category = RadioCategory.TURKISH_RADIO,
                bitrateKbps = 128,
                country = "Turkey",
                iconName = "favorite"
            ),

            // ==========================================
            // قسم 3: إذاعات بريطانيا وأمريكا (UK & USA Live)
            // ==========================================
            RadioStation(
                id = "uk_bbc_world",
                titleAr = "BBC World Service (لندن)",
                reciterOrDesc = "الخدمة العالمية لهيئة الإذاعة البريطانية - أخبار وتحليلات دولية فورية",
                streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
                backupUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
                category = RadioCategory.UK_USA,
                bitrateKbps = 96,
                country = "United Kingdom",
                iconName = "public"
            ),
            RadioStation(
                id = "uk_bbc_radio1",
                titleAr = "BBC Radio 1 (المملكة المتحدة)",
                reciterOrDesc = "المحطة الأولى لأحدث الأغاني العالمية والهيتس والبرامج الشبابية",
                streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
                backupUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United Kingdom",
                iconName = "celebration"
            ),
            RadioStation(
                id = "uk_bbc_radio2",
                titleAr = "BBC Radio 2 (بريطانيا)",
                reciterOrDesc = "المحطة الأكثر استماعاً في المملكة المتحدة - موسيقى منوعة وبرامج ثقافية",
                streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_two",
                backupUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_two",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United Kingdom",
                iconName = "music_note"
            ),
            RadioStation(
                id = "uk_bbc_radio4",
                titleAr = "BBC Radio 4 (أخبار وحوارات لندن)",
                reciterOrDesc = "المحطة الرائدة في الأخبار الذكية، الروايات، والمسرحيات الإذاعية",
                streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm",
                backupUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United Kingdom",
                iconName = "menu_book"
            ),
            RadioStation(
                id = "uk_capital_fm",
                titleAr = "Capital FM London (The UK's No.1 Hit Music)",
                reciterOrDesc = "بث مباشر من العاصمة لندن لأحدث الأغاني الإنجليزية والعالمية",
                streamUrl = "https://media-ice.musicradio.com/CapitalMP3",
                backupUrl = "https://media-ice.musicradio.com/CapitalMP3",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United Kingdom",
                iconName = "radio"
            ),
            RadioStation(
                id = "uk_heart_80s",
                titleAr = "Heart 80s UK (لندن)",
                reciterOrDesc = "موسيقى الثمانينات والبوب البريطاني الكلاسيكي مباشرة من لندن",
                streamUrl = "https://media-ssl.musicradio.com/Heart80sMP3",
                backupUrl = "https://media-ssl.musicradio.com/CapitalUK",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United Kingdom",
                iconName = "headphones"
            ),
            RadioStation(
                id = "us_npr_news",
                titleAr = "NPR 24-Hour News (أمريكا - واشنطن)",
                reciterOrDesc = "الإذاعة الوطنية الأمريكية العامة - تحليلات معمقة وأخبار حصرية",
                streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
                backupUrl = "https://npr-ice.streamguys1.com/live.mp3",
                category = RadioCategory.UK_USA,
                bitrateKbps = 96,
                country = "United States",
                iconName = "public"
            ),
            RadioStation(
                id = "us_wnyc_ny",
                titleAr = "WNYC 93.9 FM (نيويورك)",
                reciterOrDesc = "الإذاعة العامة لمدينة نيويورك - صوت العاصمة المالية والثقافية",
                streamUrl = "https://fm939.wnyc.org/wnycfm",
                backupUrl = "https://fm939.wnyc.org/wnycfm",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United States",
                iconName = "location_city"
            ),
            RadioStation(
                id = "us_181fm_awesome80s",
                titleAr = "181.FM Awesome 80's (الولايات المتحدة)",
                reciterOrDesc = "روائع وذكريات موسيقى الثمانينات الذهبية في أمريكا",
                streamUrl = "https://listen.181fm.com/181-awesome80s_128k.mp3",
                backupUrl = "https://listen.181fm.com/181-awesome80s_128k.mp3",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United States",
                iconName = "music_note"
            ),
            RadioStation(
                id = "us_kexp_seattle",
                titleAr = "KEXP 90.3 FM (سياتل - أمريكا)",
                reciterOrDesc = "الإذاعة الرائدة عالمياً في الموسيقى المستقلة واكتشاف المواهب",
                streamUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
                backupUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
                category = RadioCategory.UK_USA,
                bitrateKbps = 128,
                country = "United States",
                iconName = "radio"
            ),

            // ==========================================
            // قسم 4: إذاعات مصر والخليج والوطن العربي (Arabic Stations)
            // ==========================================
            RadioStation(
                id = "ar_nogoum_fm",
                titleAr = "راديو نجوم إف إم 100.6 (Nogoum FM مصر)",
                reciterOrDesc = "المحطة الأولى في مصر والعالم العربي - أغاني وبرامج حصرية",
                streamUrl = "https://stream.zeno.fm/qb1zvsykm98uv",
                backupUrl = "https://stream.zeno.fm/qb1zvsykm98uv",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "radio"
            ),
            RadioStation(
                id = "ar_radio_masr",
                titleAr = "راديو مصر 88.7 FM (القاهرة)",
                reciterOrDesc = "إذاعة إخبارية وتثقيفية شاملة من ماسبيرو القاهرة",
                streamUrl = "https://stream.zeno.fm/zsgrfxg71s8uv",
                backupUrl = "https://stream.zeno.fm/zsgrfxg71s8uv",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "public"
            ),
            RadioStation(
                id = "ar_mega_fm",
                titleAr = "شعبي وميجا إف إم (Sha3by 95 FM مصر)",
                reciterOrDesc = "أحدث الأغاني الشبابية والمنوعات المصرية",
                streamUrl = "https://radio95.radioca.st/stream",
                backupUrl = "https://stream.zeno.fm/qb1zvsykm98uv",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "celebration"
            ),
            RadioStation(
                id = "ar_mbc_fm",
                titleAr = "إم بي سي إف إم (MBC FM السعودية والخليج)",
                reciterOrDesc = "أشهر إذاعة للأغاني الخليجية والعربية الحديثة",
                streamUrl = "https://radio-mbcfm.mbc.net/radio-mbcfm_1.m3u8",
                backupUrl = "https://alifalifjobs.com/radio/8000/AlifAlifLive.mp3",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "music_note"
            ),
            RadioStation(
                id = "ar_skynews_arabia",
                titleAr = "راديو سكاي نيوز عربية (الإمارات)",
                reciterOrDesc = "البث الإذاعي الإخباري المباشر من أبو ظبي ودبي",
                streamUrl = "https://stream.skynewsarabia.com/hls/sna.m3u8",
                backupUrl = "https://stream.skynewsarabia.com/hls/sna.m3u8",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 128,
                country = "United Arab Emirates",
                iconName = "radio"
            ),
            RadioStation(
                id = "ar_kuwait_fm",
                titleAr = "إذاعة دولة الكويت العامة (الكويت)",
                reciterOrDesc = "البث الإذاعي الرسمي المباشر من دولة الكويت",
                streamUrl = "https://kwtkrdota.cdn.mangomolo.com/k1rdo/k1rdo.stream_aac/chunklist.m3u8",
                backupUrl = "https://stream.zeno.fm/v7n499m8ckhvv",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Kuwait",
                iconName = "celebration"
            ),
            RadioStation(
                id = "ar_sawt_khaleej",
                titleAr = "راديو قطر وصوت الخليج (قطر)",
                reciterOrDesc = "البث المباشر لإذاعة قطر وروائع الفنون الخليجية من الدوحة",
                streamUrl = "https://live.kwikmotion.com/qmcqatarradiolive/qatarradio/playlist.m3u8",
                backupUrl = "https://stream.zeno.fm/0r0xa792kwzuv",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Qatar",
                iconName = "music_note"
            ),
            RadioStation(
                id = "ar_mosaique_fm",
                titleAr = "موزاييك إف إم (تونس Mosaique FM)",
                reciterOrDesc = "الإذاعة الخاصة الأولى في تونس - منوعات وأخبار وموسيقى مغاربية",
                streamUrl = "https://radio.mosaiquefm.net/mosalive",
                backupUrl = "https://radio.mosaiquefm.net/mosalive",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 128,
                country = "Tunisia",
                iconName = "radio"
            ),
            RadioStation(
                id = "ar_hit_radio_maroc",
                titleAr = "هيت راديو المغرب (HIT RADIO Morocco)",
                reciterOrDesc = "الإذاعة المغربية الأشهر للموسيقى وبرامج الشباب والمنوعات",
                streamUrl = "https://hitradio-maroc.ice.infomaniak.ch/hitradio-maroc-128.mp3",
                backupUrl = "https://radiomars.ice.infomaniak.ch/radiomars-128.mp3",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 128,
                country = "Morocco",
                iconName = "public"
            ),
            RadioStation(
                id = "ar_algerie_chaine1",
                titleAr = "الإذاعة الوطنية الجزائرية (القناة الأولى)",
                reciterOrDesc = "البث الإذاعي الوطني الرسمي من الجزائر العاصمة",
                streamUrl = "https://webradio1.tda.dz:8001/Chaine1_64K.mp3",
                backupUrl = "https://webradio3.tda.dz:8001/ElBahdja_64K.mp3",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Algeria",
                iconName = "radio"
            ),
            RadioStation(
                id = "ar_algerie_chaine3",
                titleAr = "الإذاعة الجزائرية القناة 3 (ناطقة بالفرنسية)",
                reciterOrDesc = "الإذاعة الجزائرية الناطقة بالفرنسية للثقافة والموسيقى والشباب",
                streamUrl = "https://webradio1.tda.dz:8001/Chaine3_64K.mp3",
                backupUrl = "https://webradio1.tda.dz:8001/Jeunesse_64K.mp3",
                category = RadioCategory.ARABIC_RADIO,
                bitrateKbps = 64,
                country = "Algeria",
                iconName = "headphones"
            ),

            // ==========================================
            // قسم 5: إذاعات القرآن الكريم ومشاهير القراء (Holy Quran)
            // ==========================================
            RadioStation(
                id = "quran_cairo",
                titleAr = "إذاعة القرآن الكريم - القاهرة",
                reciterOrDesc = "البث الرسمي المباشر من القاهرة - تلاوات وبرامج نادرة",
                streamUrl = "https://stream.radiojar.com/8s5u5tpdtwzuv",
                backupUrl = "https://backup.qurango.net/radio/abdulbasit_abdulsamad_mojawwad",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "mosque"
            ),
            RadioStation(
                id = "quran_makkah",
                titleAr = "إذاعة القرآن الكريم - مكة المكرمة",
                reciterOrDesc = "تلاوات الحرم المكي وترتيل خاشع متواصل 24 ساعة",
                streamUrl = "https://radio.mp3islam.com/listen/quran_radio/radio.mp3",
                backupUrl = "https://l3.itworkscdn.net/smcquranlive/quranradiolive/icecast.audio",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "kaaba"
            ),
            RadioStation(
                id = "reciter_abdulbasit",
                titleAr = "الشيخ عبد الباسط عبد الصمد",
                reciterOrDesc = "المصحف المجود وتلاوات نادرة بجودة صوتية فائقة",
                streamUrl = "https://backup.qurango.net/radio/abdulbasit_abdulsamad_mojawwad",
                backupUrl = "https://radio.mp3islam.com/listen/abdulbasit/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_minshawi",
                titleAr = "الشيخ محمد صديق المنشاوي",
                reciterOrDesc = "المصحف المرتل برواية حفص عن عاصم بصوت باكي خاشع",
                streamUrl = "https://backup.qurango.net/radio/mohammed_siddiq_alminshawi",
                backupUrl = "https://radio.mp3islam.com/listen/minshawi/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_hussary",
                titleAr = "الشيخ محمود خليل الحصري",
                reciterOrDesc = "شيخ المقارئ المصرية وإتقان أحكام التجويد والترتيل",
                streamUrl = "https://backup.qurango.net/radio/mahmoud_khalil_alhussary",
                backupUrl = "https://backup.qurango.net/radio/mahmoud_khalil_alhussary_mojawwad",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_alafasi",
                titleAr = "الشيخ مشاري راشد العفاسي",
                reciterOrDesc = "قراءات عذبة هادئة وأناشيد وأذكار مختارة",
                streamUrl = "https://backup.qurango.net/radio/mishary_alafasi",
                backupUrl = "https://radio.mp3islam.com/listen/mishary/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Kuwait",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_muaiqly",
                titleAr = "الشيخ ماهر المعيقلي",
                reciterOrDesc = "إمام الحرم المكي الشريف وتلاوة ندية محبوبة",
                streamUrl = "https://backup.qurango.net/radio/maher",
                backupUrl = "https://radio.mp3islam.com/listen/maher/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_dosari",
                titleAr = "الشيخ ياسر الدوسري",
                reciterOrDesc = "تلاوات الحرم المكي وتراويح رمضان المؤثرة",
                streamUrl = "https://backup.qurango.net/radio/yasser_aldosari",
                backupUrl = "https://radio.mp3islam.com/listen/yaser/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_ghamdi",
                titleAr = "الشيخ سعد الغامدي",
                reciterOrDesc = "المصحف المرتل بصوت عذب وشجي يريح القلوب",
                streamUrl = "https://backup.qurango.net/radio/saad_alghamdi",
                backupUrl = "https://backup.qurango.net/radio/saad_alghamdi",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_shatri",
                titleAr = "الشيخ أبو بكر الشاطري",
                reciterOrDesc = "المصحف المرتل برواية حفص عن عاصم بنقاء رائع",
                streamUrl = "https://backup.qurango.net/radio/shaik_abu_bakr_al_shatri",
                backupUrl = "https://backup.qurango.net/radio/shaik_abu_bakr_al_shatri",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_qatami",
                titleAr = "الشيخ ناصر القطامي",
                reciterOrDesc = "نبرات قرآنية حزينة وخاشعة تأسر الوجدان",
                streamUrl = "https://backup.qurango.net/radio/nasser_alqatami",
                backupUrl = "https://backup.qurango.net/radio/nasser_alqatami",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_abbad",
                titleAr = "الشيخ فارس عباد",
                reciterOrDesc = "تلاوة يمنية شجية ورائعة تسكن بها النفس",
                streamUrl = "https://backup.qurango.net/radio/fares_abbad",
                backupUrl = "https://backup.qurango.net/radio/fares_abbad",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Yemen",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_sudais",
                titleAr = "الشيخ عبد الرحمن السديس",
                reciterOrDesc = "إمام وخطيب المسجد الحرام وتلاوات الحرم المكي المؤثرة",
                streamUrl = "https://backup.qurango.net/radio/abdulrahman_alsudaes",
                backupUrl = "https://radio.mp3islam.com/listen/sudais/radio.mp3",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_shuraim",
                titleAr = "الشيخ سعود الشريم",
                reciterOrDesc = "تلاوات الحرم المكي الخاشعة برواية حفص عن عاصم",
                streamUrl = "https://radio.mp3islam.com/listen/alshuraim/radio.mp3",
                backupUrl = "https://backup.qurango.net/radio/abdulrahman_alsudaes",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_ayyoub",
                titleAr = "الشيخ محمد أيوب",
                reciterOrDesc = "إمام المسجد النبوي الشريف وتلاوة حجازية أصيلة",
                streamUrl = "https://backup.qurango.net/radio/mohammed_ayyub",
                backupUrl = "https://backup.qurango.net/radio/ayyoub2",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_aljalil",
                titleAr = "الشيخ خالد الجليل",
                reciterOrDesc = "تلاوات مبكية ونادرة تهز القلوب والوجدان",
                streamUrl = "https://backup.qurango.net/radio/khalid_aljileel",
                backupUrl = "https://backup.qurango.net/radio/khalid_aljileel",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "reciter_idrees_abkar",
                titleAr = "الشيخ إدريس أبكر",
                reciterOrDesc = "تلاوات وأدعية قنوت مبكية بجودة عالية",
                streamUrl = "https://backup.qurango.net/radio/idrees_abkr",
                backupUrl = "https://backup.qurango.net/radio/idrees_abkr",
                category = RadioCategory.QURAN,
                bitrateKbps = 64,
                country = "Saudi Arabia",
                iconName = "person"
            ),
            RadioStation(
                id = "quran_tafseer",
                titleAr = "إذاعة تفسير القرآن الكريم",
                reciterOrDesc = "تفسير آيات القرآن الكريم مع كبار المفسرين والعلماء",
                streamUrl = "https://backup.qurango.net/radio/tafseer",
                backupUrl = "https://backup.qurango.net/radio/mukhtasartafsir",
                category = RadioCategory.QURAN,
                bitrateKbps = 48,
                country = "العالم الإسلامي",
                iconName = "menu_book"
            ),

            // ==========================================
            // قسم 6: أذكار ورقيا شرعية
            // ==========================================
            RadioStation(
                id = "athkar_sabah_masa",
                titleAr = "إذاعة أذكار الصباح والمساء",
                reciterOrDesc = "أذكار الصباح والمساء متكررة بأصوات خاشعة لبركة اليوم والليلة",
                streamUrl = "https://backup.qurango.net/radio/athkar_sabah",
                backupUrl = "https://backup.qurango.net/radio/athkar_masa",
                category = RadioCategory.ATHKAR,
                bitrateKbps = 64,
                country = "العالم الإسلامي",
                iconName = "wb_sunny"
            ),
            RadioStation(
                id = "athkar_roqiah",
                titleAr = "إذاعة الرقية الشرعية الشاملة",
                reciterOrDesc = "آيات الشفاء والسكينة والتحصين من العين والحسد والمس",
                streamUrl = "https://backup.qurango.net/radio/roqiah",
                backupUrl = "https://backup.qurango.net/radio/roqiah",
                category = RadioCategory.ATHKAR,
                bitrateKbps = 64,
                country = "العالم الإسلامي",
                iconName = "health_and_safety"
            ),
            RadioStation(
                id = "athkar_baqarah",
                titleAr = "إذاعة سورة البقرة المستمرة",
                reciterOrDesc = "سورة البقرة كاملة مكررة للبركة وحفظ المنازل من الشياطين",
                streamUrl = "https://backup.qurango.net/radio/albaqarah",
                backupUrl = "https://backup.qurango.net/radio/albaqarah",
                category = RadioCategory.ATHKAR,
                bitrateKbps = 64,
                country = "العالم الإسلامي",
                iconName = "shield"
            ),
            RadioStation(
                id = "athkar_prophets",
                titleAr = "إذاعة السيرة النبوية العطرة",
                reciterOrDesc = "في ظلال السيرة النبوية العطرة • 400 حلقة عن حياة المصطفى ﷺ",
                streamUrl = "https://backup.qurango.net/radio/fi_zilal_alsiyra",
                backupUrl = "https://backup.qurango.net/radio/almukhtasar_fi_alsiyra",
                category = RadioCategory.ATHKAR,
                bitrateKbps = 48,
                country = "العالم الإسلامي",
                iconName = "history_edu"
            ),

            // ==========================================
            // قسم 7: طرب أصيل والزمن الجميل
            // ==========================================
            RadioStation(
                id = "tarab_om_kalthoum",
                titleAr = "إذاعة كوكب الشرق أم كلثوم (24/7)",
                reciterOrDesc = "سهرات وروائع سيدة الغناء العربي والقصائد التاريخية الخالدة",
                streamUrl = "https://stream.zeno.fm/zsgrfxg71s8uv",
                backupUrl = "https://stream.zeno.fm/zsgrfxg71s8uv",
                category = RadioCategory.TARAB,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "mic"
            ),
            RadioStation(
                id = "tarab_abdel_halim",
                titleAr = "إذاعة العندليب عبد الحليم حافظ",
                reciterOrDesc = "أغاني وقصائد وألحان العندليب الأسمر عبد الحليم حافظ الخالدة",
                streamUrl = "https://stream.zeno.fm/na3vpvn10qruv",
                backupUrl = "https://stream.zeno.fm/ctmapun6ot5vv",
                category = RadioCategory.TARAB,
                bitrateKbps = 64,
                country = "Egypt",
                iconName = "music_note"
            ),
            RadioStation(
                id = "tarab_fairouz_morning",
                titleAr = "إذاعة جارة القمر فيروز (Fairouz Classics)",
                reciterOrDesc = "أجمل أغاني الصباح وفيروزيات دافئة وألحان الرحابنة لبداية يوم رايق",
                streamUrl = "https://stream.zeno.fm/9vrahqqfuuhvv",
                backupUrl = "https://stream.zeno.fm/xkhnk4vee18uv",
                category = RadioCategory.TARAB,
                bitrateKbps = 64,
                country = "Lebanon",
                iconName = "wb_sunny"
            ),
            RadioStation(
                id = "tarab_warda_aljazairia",
                titleAr = "إذاعة الرحبانيات والذكريات الجميلة",
                reciterOrDesc = "روائع الطرب الأصيل واللحن العذب من الزمن الجميل",
                streamUrl = "https://stream.zeno.fm/4g8xck48kfhvv",
                backupUrl = "https://stream.zeno.fm/ctmapun6ot5vv",
                category = RadioCategory.TARAB,
                bitrateKbps = 64,
                country = "Lebanon",
                iconName = "music_note"
            ),
            RadioStation(
                id = "tarab_zaman_aljameel",
                titleAr = "إذاعة طربيات زمان ونغم أصيل",
                reciterOrDesc = "فريد الأطرش، محمد عبد الوهاب، نجاة الصغيرة، وصباح فخري",
                streamUrl = "https://stream.zeno.fm/na3vpvn10qruv",
                backupUrl = "https://stream.zeno.fm/na3vpvn10qruv",
                category = RadioCategory.TARAB,
                bitrateKbps = 64,
                country = "العالم العربي",
                iconName = "mic"
            ),

            // ==========================================
            // قسم 8: منوعات وهدوء عالمي
            // ==========================================
            RadioStation(
                id = "music_arabic_chillout",
                titleAr = "راديو الهدوء والروقان العربي (Arabic Chillout)",
                reciterOrDesc = "معزوفات عود وقانون وموسيقى هادئة تريح الأعصاب",
                streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
                backupUrl = "https://stream.zeno.fm/0r0xa792kwzuv",
                category = RadioCategory.MUSIC_RELAX,
                bitrateKbps = 64,
                country = "العالم",
                iconName = "spa"
            ),
            RadioStation(
                id = "music_arabic_lounge",
                titleAr = "راديو الصالون والمود الهادئ (Arabic Lounge)",
                reciterOrDesc = "ألحان شرقية وعصرية ممزوجة بجلسات هادئة تحفز التركيز",
                streamUrl = "https://stream.zeno.fm/0r0xa792kwzuv",
                backupUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
                category = RadioCategory.MUSIC_RELAX,
                bitrateKbps = 64,
                country = "العالم",
                iconName = "headphones"
            )
        )
    }

    fun getByCategory(category: RadioCategory): List<RadioStation> {
        return stations.filter { it.category == category }
    }
}
