package com.example

import com.example.live.XtreamCodesImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamCodesImporterTest {

    @Test
    fun mapsAllStreamsToChannelUrlsWithCategoriesAndLogos() {
        val categories = """
            [
              {"category_id":"10","category_name":"News"},
              {"category_id":"20","category_name":"Sports"}
            ]
        """.trimIndent()
        val streams = """
            [
              {"stream_id":101,"name":"News HD","category_id":"10","stream_icon":"https://cdn.example/news.png","epg_channel_id":"news.id"},
              {"stream_id":202,"name":"Sports HD","category_id":"20","stream_icon":"https://cdn.example/sports.png","epg_channel_id":"sports.id"}
            ]
        """.trimIndent()

        val channels = XtreamCodesImporter.parseLiveChannels(
            server = "https://provider.example:8080",
            username = "demo user",
            password = "p@ss",
            categoriesJson = categories,
            streamsJson = streams,
            outputFormat = "m3u8"
        )

        assertEquals(2, channels.size)
        assertEquals("News HD", channels[0].title)
        assertEquals("News", channels[0].category)
        assertEquals("https://cdn.example/news.png", channels[0].logoUrl)
        assertEquals("news.id", channels[0].tvgId)
        assertEquals(
            "https://provider.example:8080/live/demo%20user/p%40ss/101.m3u8",
            channels[0].url
        )
        assertEquals("Sports", channels[1].category)
    }

    @Test
    fun removesDuplicateStreamUrls() {
        val channels = XtreamCodesImporter.parseLiveChannels(
            server = "http://provider.example:8080",
            username = "user",
            password = "pass",
            categoriesJson = "[]",
            streamsJson = """
                [
                  {"stream_id":1,"name":"Channel A"},
                  {"stream_id":1,"name":"Channel A duplicate"}
                ]
            """.trimIndent()
        )

        assertEquals(1, channels.size)
    }

    @Test
    fun acceptsServerUrlWithPlayerApiSuffix() {
        val channels = XtreamCodesImporter.parseLiveChannels(
            server = "https://provider.example/xtream/player_api.php",
            username = "user",
            password = "pass",
            categoriesJson = "[]",
            streamsJson = """[{"stream_id":9,"name":"One"}]"""
        )

        assertTrue(
            channels.single().url.startsWith("https://provider.example/xtream/live/user/pass/9.ts")
        )
    }

    @Test
    fun ignoresInvalidEntriesAndUsesDefaultCategory() {
        val channels = XtreamCodesImporter.parseLiveChannels(
            server = "http://provider.example",
            username = "user",
            password = "pass",
            categoriesJson = """[{"category_id":"1","category_name":"Sports"}]""",
            streamsJson = """
                [
                  {},
                  {"stream_id":7,"name":"A","category_id":"missing"}
                ]
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Xtream · بث مباشر", channels.single().category)
    }
}
