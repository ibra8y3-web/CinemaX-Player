package com.example.live

import com.example.data.model.LiveStreamChannel
import com.example.live.catalog.AmericasAndAfricaCatalog
import com.example.live.catalog.ArabicNewsAndTvCatalog
import com.example.live.catalog.AsianTvCatalog
import com.example.live.catalog.EntertainmentSportsKidsCatalog
import com.example.live.catalog.EuropeanTvCatalog
import com.example.live.catalog.FrenchTvCatalog
import com.example.live.catalog.IslamicAndQuranCatalog
import com.example.live.catalog.MoviesAndCinemaCatalog
import com.example.live.catalog.SportsChannelsCatalog
import com.example.live.catalog.TurkishTvCatalog

/**
 * دليل القنوات والبث المباشر العالمي الشامل والواقعي 100%.
 * يغطي القنوات الرياضية والكورة، الأفلام والسينما، العربية، الفرنسية، التركية، الآسيوية، الأوروبية، الأمريكية، الأفريقية، والأطفال.
 */
object LiveChannelsCatalog {

    fun buildCatalog(): List<LiveStreamChannel> {
        val list = ArrayList<LiveStreamChannel>(2000)

        // 1. باقة قنوات الرياضة والكورة والبث المباشر (beIN Sports, OnTime, الكأس, دبي, أبوظبي, السعودية)
        list.addAll(SportsChannelsCatalog.getChannels())

        // 2. باقة قنوات الأفلام والسينما (روتانا سينما, كلاسيك, كوميدي, هوليوود, بوليوود)
        list.addAll(MoviesAndCinemaCatalog.getChannels())

        // 3. قنوات عربية وأخبار
        list.addAll(ArabicNewsAndTvCatalog.getChannels())

        // 4. إسلاميات وقرآن
        list.addAll(IslamicAndQuranCatalog.getChannels())

        // 5. قنوات فرنسية وأوروبية
        list.addAll(FrenchTvCatalog.getChannels())

        // 6. قنوات تركية
        list.addAll(TurkishTvCatalog.getChannels())

        // 7. قنوات أوروبية
        list.addAll(EuropeanTvCatalog.getChannels())

        // 8. قنوات آسيوية
        list.addAll(AsianTvCatalog.getChannels())

        // 9. قنوات أمريكية وأفريقية
        list.addAll(AmericasAndAfricaCatalog.getChannels())

        // 10. أطفال ووثائقيات
        list.addAll(EntertainmentSportsKidsCatalog.getChannels())

        return list.distinctBy { it.id }
    }
}
