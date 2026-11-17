package com.example.live.catalog

import com.example.data.model.LiveStreamChannel

object FrenchTvCatalog {

    fun getChannels(): List<LiveStreamChannel> {
        val raw = listOf(
            Triple("fr_0_france_24_fran_ais_hd_24", "France 24 Français HD (فرنسا 24 🇫🇷)", "https://static.france24.com/live/F24_FR_HI_HLS/live_tv.m3u8"),
            Triple("fr_1_6ter_720p", "6ter (720p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/6ter_fr/live.livx/playlist.m3u8"),
            Triple("fr_2_20_minutes_tv_1080p", "20 Minutes TV (1080p) 🇫🇷", "https://live-20minutestv.digiteka.com/1961167769/index.m3u8"),
            Triple("fr_3_alpe_d_huez_tv_720p", "Alpe d'Huez TV (720p) 🇫🇷", "https://edge13.vedge.infomaniak.com/livecast/ik:adhtv/manifest.m3u8"),
            Triple("fr_4_antenne_reunion_720p", "Antenne Reunion (720p) 🇫🇷", "https://live-antenne-reunion.zeop.tv/live/c3eds/antreunihd/hls_fta/antreunihd.m3u8?location=ZEOP01"),
            Triple("fr_5_brionnais_tv_720p", "Brionnais TV (720p) 🇫🇷", "https://stream2.mandarine.media/brionnaistv/brionnaistv/playlist.m3u8"),
            Triple("fr_6_caillou", "Caillou 🇫🇷", "https://do7nccdsswstc.cloudfront.net/v1/manifest/3722c60a815c199d9c0ef36c5b73da68a62b09d1/cc-1aso0bc668saa/a5233c83-f772-4a81-959a-45ec7877ef61/5.m3u8"),
            Triple("fr_7_cannes_lerins_tv_1080p", "Cannes Lerins TV (1080p) 🇫🇷", "https://vdo2.pro-fhi.net:3628/live/uppodsfqlive.m3u8"),
            Triple("fr_8_comedy_central_france_720p", "Comedy Central (France) (720p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/comedy_central/live.livx/playlist.m3u8"),
            Triple("fr_9_dbm_tv_1080p", "DBM TV (1080p) 🇫🇷", "https://dbmtv.vedge.infomaniak.com/livecast/dbmtv/playlist.m3u8"),
            Triple("fr_10_fashiontv_paris_l_original", "FashionTV Paris L'Original (1080p) 🇫🇷", "https://edge-fast3.evrideo.tv/bfdbb576-83f7-11f0-9f89-0200170e3e04_1000028043_HLS/manifest.m3u8"),
            Triple("fr_11_france_2_1080p", "France 2 (1080p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/france_2/live.livx/playlist.m3u8"),
            Triple("fr_12_france_3_1080p", "France 3 (1080p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/france_3/live.livx/playlist.m3u8"),
            Triple("fr_13_france_5_1080p", "France 5 (1080p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/france_5/live.livx/playlist.m3u8"),
            Triple("fr_14_frequence_novelas_1080p", "Frequence Novelas (1080p) 🇫🇷", "https://d20oth1416y9a1.cloudfront.net/playlist/amg00711-zylofast-frequencenovelas-rakutenus/playlist.m3u8"),
            Triple("fr_15_generations_tv_576p", "Generations TV (576p) 🇫🇷", "https://event.vedge.infomaniak.com/livecast/ik:generation-tv/manifest.m3u8"),
            Triple("fr_16_handicap_tv", "HANDICAP TV 🇫🇷", "https://srv.webtvmanager.fr:3697/stream/play.m3u8"),
            Triple("fr_17_lcp_477p", "LCP (477p) 🇫🇷", "https://raw.githubusercontent.com/Paradise-91/ParaTV/main/streams/publicsenat/publicsenat-dm.m3u8"),
            Triple("fr_18_le_figaro_tv_ile_de_france", "Le Figaro TV Ile-de-France (1080p) 🇫🇷", "https://figarotv-live.freecaster.com/live/freecaster/figarotv.m3u8"),
            Triple("fr_19_mtv_france_720p", "MTV (France) (720p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/mtv/live.livx/playlist.m3u8"),
            Triple("fr_20_novo19_720p", "NOVO19 (720p) 🇫🇷", "https://r.eu.cdn-redge.media/livehls/oeu/mt/live/novo_19/live.livx/playlist.m3u8"),
            Triple("fr_21_p2m_tv_720p", "P2M TV (720p) 🇫🇷", "https://vdo2.pro-fhi.net:3207/stream/play.m3u8"),
            Triple("fr_22_puissance_tv", "Puissance TV 🇫🇷", "https://event.vedge.infomaniak.com/livecast/ik:puissancetelevision/manifest.m3u8"),
            Triple("fr_23_qwest_tv_jazz_beyond_1080p", "Qwest TV Jazz & Beyond (1080p) 🇫🇷", "https://samsungau-qwestjazz-samsungtv-zw2jc.amagi.tv/playlist/samsungau-qwestjazz-samsungtv/playlist.m3u8"),
            Triple("fr_24_rmc_decouverte_1080p", "RMC Decouverte (1080p) 🇫🇷", "https://d16zzycxcd0m0r.cloudfront.net/v1/master/3722c60a815c199d9c0ef36c5b73da68a62b09d1/cc-hixvx5kymecr9/RMC_Decouverte_FR.m3u8"),
            Triple("fr_25_rmc_life_1080p", "RMC Life (1080p) 🇫🇷", "https://d3dcdjv6dx07iz.cloudfront.net/v1/master/3722c60a815c199d9c0ef36c5b73da68a62b09d1/cc-eaaww2dyp3iih/RMC_Life_FR.m3u8")
        )
        val backups = listOf("https://static.france24.com/live/F24_FR_HI_HLS/live_tv.m3u8")
        return raw.map { (id, title, url) ->
            LiveStreamChannel(
                id = id,
                title = title,
                titleAr = title,
                url = url,
                category = "قنوات فرنسية (France TV 🇫🇷)",
                iconName = "tv",
                description = "بث مباشر حقيقي للقنوات الفرنسية الرسمية (أخبار، ثقافة، رياضة وترفيه)",
                backupUrls = backups
            )
        }
    }
}
