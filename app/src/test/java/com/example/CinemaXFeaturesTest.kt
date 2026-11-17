package com.example

import com.example.ai.AiEnhanceMode
import com.example.ai.AiVideoCategorizer
import com.example.data.model.AdhanAlarmType
import com.example.data.model.AdhanLogEntry
import com.example.live.LiveChannelsCatalog
import com.example.music.MusicPlayerManager
import com.example.player.NovaPlayerManager
import com.example.prayer.AdhanLogStore
import com.example.prayer.AdhanVoicesCatalog
import com.example.radio.RadioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CinemaXFeaturesTest {

    @Test
    fun `cinema hdr and video filters contain at least 25 modes`() {
        assertTrue(
            "Expected at least 25 video filters, found ${AiEnhanceMode.entries.size}",
            AiEnhanceMode.entries.size >= 25
        )
        val distinctArTitles = AiEnhanceMode.entries.map { it.titleAr }.toSet()
        assertEquals(AiEnhanceMode.entries.size, distinctArTitles.size)
    }

    @Test
    fun `repeat modes expose full cycling states for audio and video`() {
        assertEquals(
            listOf(
                MusicPlayerManager.AudioRepeatMode.OFF,
                MusicPlayerManager.AudioRepeatMode.ALL,
                MusicPlayerManager.AudioRepeatMode.ONE
            ),
            MusicPlayerManager.AudioRepeatMode.entries
        )
        assertEquals(
            listOf(
                NovaPlayerManager.VideoRepeatMode.OFF,
                NovaPlayerManager.VideoRepeatMode.ONE,
                NovaPlayerManager.VideoRepeatMode.ALL
            ),
            NovaPlayerManager.VideoRepeatMode.entries
        )
    }

    @Test
    fun `ai video categorizer classifies key media categories accurately`() {
        assertEquals(
            "قرآن وتلاوات",
            AiVideoCategorizer.categorize("سورة البقرة بصوت المنشاوي", "Download", 1800_000L, 1920, 1080)
        )
        assertEquals(
            "شورتس وريلز",
            AiVideoCategorizer.categorize("clip_01", "Camera", 35_000L, 1080, 1920)
        )
        assertEquals(
            "أفلام وسينما",
            AiVideoCategorizer.categorize("Documentary_4K", "Movies", 3600_000L, 3840, 2160)
        )
    }

    @Test
    fun `live channels catalog has hundreds of distinct non-youtube verified streams`() {
        val catalog = LiveChannelsCatalog.buildCatalog()
        assertTrue("Expected >= 900 verified live channels, got ${catalog.size}", catalog.size >= 900)

        val uniqueUrls = catalog.map { it.url }.toSet()
        assertEquals("Catalog primary URLs should be distinct", catalog.size, uniqueUrls.size)

        catalog.forEach { channel ->
            assertTrue("Channel ${channel.id} must use valid direct stream scheme", channel.url.startsWith("https://"))
            assertFalse("YouTube web URLs not allowed in Live TV catalog: ${channel.url}", channel.url.contains("youtube.com"))
            assertFalse("Invalid template slug in URL: ${channel.url}", channel.url.contains("\$slug"))
        }
    }

    @Test
    fun `adhan voices catalog has 10 full authentic recordings and migrates legacy ids`() {
        val voices = AdhanVoicesCatalog.voices
        assertEquals(10, voices.size)
        voices.forEach { voice ->
            assertTrue("Voice ${voice.id} should be a full Adhan (>= 120s)", voice.durationSec >= 120)
            assertNotEquals("Voice ${voice.id} must point to a real raw resource", 0, voice.rawResId)
            assertTrue(voice.muazzinName.isNotBlank())
        }

        // Backward-compatibility ID migration for previously saved prefs
        assertEquals("adhan_surayhi", AdhanVoicesCatalog.getVoiceById("adhan_dosari").id)
        assertEquals("adhan_fajr_hijaz", AdhanVoicesCatalog.getVoiceById("adhan_hussary").id)
        assertEquals("adhan_sham_khashia", AdhanVoicesCatalog.getVoiceById("adhan_minshawi").id)
    }

    @Test
    fun `adhan log store serializes and enforces pre post timings and tasbih cycle logic`() {
        val basePrayerMs = 1_800_000_000_000L
        assertEquals(
            basePrayerMs - 600_000L,
            AdhanLogStore.computeOffsetTriggerMillis(basePrayerMs, 10, isBefore = true)
        )
        assertEquals(
            basePrayerMs + 900_000L,
            AdhanLogStore.computeOffsetTriggerMillis(basePrayerMs, 15, isBefore = false)
        )

        val (count32, done32) = AdhanLogStore.nextTasbihCount(31, 33)
        assertEquals(32, count32)
        assertFalse(done32)

        val (count33, done33) = AdhanLogStore.nextTasbihCount(32, 33)
        assertEquals(33, count33)
        assertTrue(done33)

        val (openCount, openDone) = AdhanLogStore.nextTasbihCount(150, 0)
        assertEquals(151, openCount)
        assertFalse(openDone)

        val entries = listOf(
            AdhanLogEntry("1", 100L, "2026-11-15 12:00:00", "الظهر", "القاهرة", "الشيخ علي ملا", AdhanAlarmType.PRE_ADHAN, "قبل 10د"),
            AdhanLogEntry("2", 200L, "2026-11-15 12:10:00", "الظهر", "القاهرة", "الشيخ علي ملا", AdhanAlarmType.ADHAN, "تم الأذان"),
            AdhanLogEntry("3", 300L, "2026-11-15 12:20:00", "الظهر", "القاهرة", "الشيخ علي ملا", AdhanAlarmType.POST_ADHAN, "تسبيح وإقامة")
        )
        val encoded = AdhanLogStore.encodeLogs(entries)
        val decoded = AdhanLogStore.decodeLogs(encoded)
        assertEquals(entries.size, decoded.size)
        assertEquals(AdhanAlarmType.PRE_ADHAN, decoded[0].eventType)
        assertEquals(AdhanAlarmType.ADHAN, decoded[1].eventType)
        assertEquals(AdhanAlarmType.POST_ADHAN, decoded[2].eventType)
    }

    @Test
    fun `radio catalog contains valid https streams without deprecated dead hosts`() {
        val stations = RadioCatalog.stations
        assertTrue(stations.size >= 40)
        stations.forEach { station ->
            assertTrue("Station ${station.id} must use HTTPS", station.streamUrl.startsWith("https://"))
            assertFalse("Dead NRJ proxy found in ${station.id}", station.streamUrl.contains("streaming.nrjaudio.fm"))
            assertFalse("Dead alanbiya endpoint found in ${station.id}", station.streamUrl.endsWith("/alanbiya"))
        }
    }
}
