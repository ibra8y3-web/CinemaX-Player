package com.example

import com.example.live.M3uPlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistParserTest {

    @Test
    fun parsesEveryExtInfEntryAndKeepsGroupNames() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.one" tvg-name="News One" tvg-logo="https://cdn.example/logo.png" group-title="News",News One HD
            https://cdn.example/news/one.m3u8
            #EXTINF:-1 tvg-id="sport.two" group-title="Sports",Sports Two
            https://cdn.example/sport/two.m3u8
            #EXTINF:-1 group-title="Sports",Sports Three
            https://cdn.example/sport/three.m3u8
        """.trimIndent()

        val channels = M3uPlaylistParser.parse(content)

        assertEquals(3, channels.size)
        assertEquals("News One HD", channels[0].title)
        assertEquals("News", channels[0].category)
        assertEquals("news.one", channels[0].tvgId)
        assertEquals("https://cdn.example/logo.png", channels[0].logoUrl)
        assertEquals("Sports", channels[1].category)
        assertEquals("https://cdn.example/sport/three.m3u8", channels[2].url)
    }

    @Test
    fun handlesCommaInsideQuotedAttributesAndChannelNames() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://cdn.example/logo,a.png" group-title="News, World",Channel, International
            https://cdn.example/channel.m3u8
        """.trimIndent()

        val channel = M3uPlaylistParser.parse(content).single()
        assertEquals("Channel, International", channel.title)
        assertEquals("News, World", channel.category)
        assertEquals("https://cdn.example/logo,a.png", channel.logoUrl)
    }

    @Test
    fun removesDuplicateStreamUrls() {
        val content = """
            #EXTM3U
            #EXTINF:-1,First name
            https://cdn.example/live.m3u8
            #EXTINF:-1,Second name
            https://cdn.example/live.m3u8
        """.trimIndent()

        val channels = M3uPlaylistParser.parse(content)
        assertEquals(1, channels.size)
        assertEquals("First name", channels.single().title)
    }

    @Test
    fun supportsRelativeUrlsAgainstPlaylistAddress() {
        val raw = """
            #EXTM3U
            #EXTINF:-1,Relative Channel
            streams/relative.m3u8
            #EXTINF:-1,Absolute Channel
            https://cdn.example/live.m3u8
        """.trimIndent()

        val channels = M3uPlaylistParser.parse(raw, "https://lists.example.com/iptv/list.m3u")
        assertEquals(2, channels.size)
        assertEquals("https://lists.example.com/iptv/streams/relative.m3u8", channels[0].url)
        assertEquals("https://cdn.example/live.m3u8", channels[1].url)
    }

    @Test
    fun rawUrlLinesBecomeSeparateChannels() {
        val channels = M3uPlaylistParser.parse(
            "https://cdn.example/one.m3u8\nhttps://cdn.example/two.m3u8"
        )
        assertEquals(2, channels.size)
    }

    @Test
    fun hlsMediaManifestIsNotImportedAsAnIptvChannelList() {
        val content = """
            #EXTM3U
            #EXT-X-TARGETDURATION:10
            #EXTINF:10,
            segment001.ts
            #EXTINF:10,
            segment002.ts
        """.trimIndent()

        val error = runCatching {
            M3uPlaylistParser.parse(content, "https://cdn.example/video.m3u8")
        }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("HLS"))
    }
}
