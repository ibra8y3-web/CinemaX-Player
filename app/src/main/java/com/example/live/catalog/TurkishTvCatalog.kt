package com.example.live.catalog

import com.example.data.model.LiveStreamChannel

object TurkishTvCatalog {

    fun getChannels(): List<LiveStreamChannel> {
        val raw = listOf(
            Triple("tr_0_4u_tv_720p", "4U TV (720p) 🇹🇷", "https://hls.4utv.live/hls/stream.m3u8"),
            Triple("tr_1_a2tv_1080p", "A2TV (1080p) 🇹🇷", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv.m3u8"),
            Triple("tr_2_aksu_tv_720p", "Aksu TV (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/playlist.m3u8"),
            Triple("tr_3_alanya_posta_tv_1080p", "Alanya Posta TV (1080p) 🇹🇷", "https://api-tv3.yayin.com.tr/postatv/postatv/playlist.m3u8"),
            Triple("tr_4_altas_tv_1080p", "Altas TV (1080p) 🇹🇷", "https://edge1.socialsmart.tv/altastv/bant1/playlist.m3u8"),
            Triple("tr_5_anadolu_net_tv_1080p", "Anadolu Net TV (1080p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/playlist.m3u8"),
            Triple("tr_6_atv_turkiye_1080p", "ATV (Turkiye) (1080p) 🇹🇷", "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_1080p.m3u8"),
            Triple("tr_7_atv_alanya_1080p", "ATV Alanya (1080p) 🇹🇷", "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/playlist.m3u8"),
            Triple("tr_8_bir_tv_720p", "Bir TV (720p) 🇹🇷", "https://edge.taksimbilisim.com/birtv/bant1/playlist.m3u8"),
            Triple("tr_9_bloomberg_ht_720p", "Bloomberg HT (720p) 🇹🇷", "https://mumbai-edge.smartplaytv.in/BloombergHT/index.m3u8"),
            Triple("tr_10_brtv_turkiye_720p", "BRTV (Turkiye) (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/playlist.m3u8"),
            Triple("tr_11_cay_tv_720p", "Cay TV (720p) 🇹🇷", "https://edge1.socialsmart.tv/caytv/bant1/playlist.m3u8"),
            Triple("tr_12_ciftci_tv_720p", "Ciftci TV (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/playlist.m3u8"),
            Triple("tr_13_cnbc_e", "CNBC-e 🇹🇷", "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8"),
            Triple("tr_14_deniz_postasi_tv_720p", "Deniz Postasi TV (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/playlist.m3u8"),
            Triple("tr_15_dream_turk_720p", "Dream Turk (720p) 🇹🇷", "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/playlist.m3u8"),
            Triple("tr_16_edessa_tv_1080p", "Edessa TV (1080p) 🇹🇷", "https://canli.edessatv.com/hls/stream.m3u8"),
            Triple("tr_17_ertv_1080p", "ERTV (1080p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/playlist.m3u8"),
            Triple("tr_18_erzurum_web_tv_720p", "Erzurum Web TV (720p) 🇹🇷", "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/iptvdelisi.m3u8"),
            Triple("tr_19_es_tv_720p", "ES TV (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_estv/estv/playlist.m3u8"),
            Triple("tr_20_etv_kayseri_720p", "ETV Kayseri (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_etv/etv/playlist.m3u8"),
            Triple("tr_21_etv_manisa_1080p", "ETV Manisa (1080p) 🇹🇷", "https://edge1.socialsmart.tv/manisaetv/bant1/playlist.m3u8"),
            Triple("tr_22_euro_d_720p", "Euro D (720p) 🇹🇷", "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/playlist.m3u8"),
            Triple("tr_23_fortuna_tv_720p", "Fortuna TV (720p) 🇹🇷", "https://edge1.socialsmart.tv/ftvturk/bant1/playlist.m3u8"),
            Triple("tr_24_icel_tv_720p", "Icel TV (720p) 🇹🇷", "https://edge1.socialsmart.tv/iceltv/bant1/playlist.m3u8"),
            Triple("tr_25_ilke_tv_1080p", "Ilke TV (1080p) 🇹🇷", "https://stream.ilketv.com.tr/hls/ilkecanli.m3u8"),
            Triple("tr_26_kanal_3_1080p", "Kanal 3 (1080p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/playlist.m3u8"),
            Triple("tr_27_kanal_7_avrupa_720p", "Kanal 7 Avrupa (720p) 🇹🇷", "https://livetv.radyotvonline.net/kanal7live/kanal7avr/playlist.m3u8"),
            Triple("tr_28_kanal_12_720p", "Kanal 12 (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/playlist.m3u8"),
            Triple("tr_29_kanal_15_720p", "Kanal 15 (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/playlist.m3u8"),
            Triple("tr_30_kanal_23_1080p", "Kanal 23 (1080p) 🇹🇷", "https://cdn-kanal23.yayin.com.tr/kanal23/index.m3u8"),
            Triple("tr_31_kanal_26_720p", "Kanal 26 (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/playlist.m3u8"),
            Triple("tr_32_kanal_32_720p", "Kanal 32 (720p) 🇹🇷", "https://edge1.socialsmart.tv/kanal32/bant1/playlist.m3u8"),
            Triple("tr_33_kanal_33_720p", "Kanal 33 (720p) 🇹🇷", "https://edge1.socialsmart.tv/kanal33/bant1/playlist.m3u8"),
            Triple("tr_34_kanal_58_720p", "Kanal 58 (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/playlist.m3u8"),
            Triple("tr_35_kanal_d_turkiye_1080p", "Kanal D (Turkiye) (1080p) 🇹🇷", "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3"),
            Triple("tr_36_kanal_firat_720p", "Kanal Firat (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/playlist.m3u8"),
            Triple("tr_37_kanal_v_720p", "Kanal V (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/playlist.m3u8"),
            Triple("tr_38_kay_tv_576p", "Kay TV (576p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/playlist.m3u8"),
            Triple("tr_39_kent_turk_1080p", "Kent Turk (1080p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/playlist.m3u8"),
            Triple("tr_40_kn_music_tv", "KN Music TV 🇹🇷", "https://cdn4.yayin.com.tr/kntv/tracks-v1a1/mono.m3u8"),
            Triple("tr_41_kocaeli_tv_1080p", "Kocaeli TV (1080p) 🇹🇷", "https://edge.taksimbilisim.com/kocaelitv/bant1/playlist.m3u8"),
            Triple("tr_42_konya_olay_tv_720p", "Konya Olay TV (720p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/playlist.m3u8"),
            Triple("tr_43_kral_pop_tv_720p", "Kral Pop TV (720p) 🇹🇷", "https://dogus-live.daioncdn.net/kralpoptv/playlist.m3u8"),
            Triple("tr_44_krt_1080p", "KRT (1080p) 🇹🇷", "https://raw.githubusercontent.com/yaskar661-lgtm/stream/refs/heads/main/filbox/krthd.m3u8"),
            Triple("tr_45_line_tv_720p", "Line TV (720p) 🇹🇷", "https://edge1.socialsmart.tv/linetv/bant1/playlist.m3u8"),
            Triple("tr_46_luys_tv_1080p", "Luys TV (1080p) 🇹🇷", "https://b01c02nl.mediatriple.net/videoonlylive/mtpayqrfkgirxelive/broadcast_5e91c5ac96898.smil/playlist.m3u8"),
            Triple("tr_47_mavikaradeniz_tv_1080p", "MaviKaradeniz TV (1080p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/playlist.m3u8"),
            Triple("tr_48_mercan_tv_576p", "Mercan TV (576p) 🇹🇷", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/playlist.m3u8"),
            Triple("tr_49_mturk_tv_1080p", "MTurk TV (1080p) 🇹🇷", "https://edge1.socialsmart.tv/muglaturk/bant1/playlist.m3u8")
        )
        val backups = listOf("https://hls.4utv.live/hls/stream.m3u8")
        return raw.map { (id, title, url) ->
            LiveStreamChannel(
                id = id,
                title = title,
                titleAr = title,
                url = url,
                category = "قنوات تركية (Turkey TV)",
                iconName = "live_tv",
                description = "بث مباشر حقيقي ومفحوص للقنوات التركية (دراما، أخبار، أطفال، رياضة وموسيقى)",
                backupUrls = backups
            )
        }
    }
}
