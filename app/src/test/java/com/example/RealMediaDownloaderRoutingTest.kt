package com.example

import com.example.media.RealMediaDownloader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RealMediaDownloaderRoutingTest {

    @Test
    fun facebookPageRoutesToBrowserCapture() = runBlocking {
        val result = RealMediaDownloader.verifyVideoBeforeDownload(
            "https://www.facebook.com/watch/?v=123456789"
        )

        assertTrue(
            "A Facebook page must not be accepted as a direct media file",
            result is RealMediaDownloader.VideoVerificationResult.SocialWebPage
        )
        result as RealMediaDownloader.VideoVerificationResult.SocialWebPage
        assertEquals("Facebook", result.platform)
        assertTrue(result.advice.contains("مباشر"))
    }

    @Test
    fun facebookShortShareUrlIsIdentifiedWithoutNetwork() = runBlocking {
        val result = RealMediaDownloader.verifyVideoBeforeDownload("https://fb.watch/AbCdEf12/")

        assertTrue(result is RealMediaDownloader.VideoVerificationResult.SocialWebPage)
        result as RealMediaDownloader.VideoVerificationResult.SocialWebPage
        assertEquals("Facebook", result.platform)
    }

    @Test
    fun youtubePageIsIdentifiedAsUnsupportedForDirectAppDownload() = runBlocking {
        val result = RealMediaDownloader.verifyVideoBeforeDownload(
            "https://youtu.be/dQw4w9WgXcQ"
        )

        assertTrue(result is RealMediaDownloader.VideoVerificationResult.SocialWebPage)
        result as RealMediaDownloader.VideoVerificationResult.SocialWebPage
        assertEquals("YouTube", result.platform)
        assertTrue(result.advice.contains("لا يستخرج CinemaX"))
    }

    @Test
    fun directFilesAreClassifiedByTheirRealFileExtension() {
        val pdf = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/docs/report.pdf")
        val archive = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/files/archive.zip")
        val audio = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/audio/track.mp3")
        val apk = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/apps/game.apk")
        val xapk = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/apps/game.xapk")
        val apks = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/apps/game.apks")

        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, pdf.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, archive.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, audio.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, apk.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, xapk.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, apks.route)
    }

    @Test
    fun pastedHlsAndDashLinksAreRoutedToUniversalWebsiteExtractor() = runBlocking {
        val hls = RealMediaDownloader.verifyVideoBeforeDownload(
            "https://cdn.example.net/live/index.m3u8"
        )
        val dash = RealMediaDownloader.verifyVideoBeforeDownload(
            "https://cdn.example.net/live/manifest.mpd"
        )

        assertTrue(hls is RealMediaDownloader.VideoVerificationResult.SocialWebPage)
        assertTrue(dash is RealMediaDownloader.VideoVerificationResult.SocialWebPage)
    }

    @Test
    fun streamingManifestsAreNotMistakenForSingleFiles() {
        val hls = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/live/index.m3u8")
        val dash = com.example.media.DownloadLinkAnalyzer.analyze("https://cdn.example.net/live/manifest.mpd")

        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.HLS_MANIFEST, hls.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DASH_MANIFEST, dash.route)
    }

    @Test
    fun providerMatchingDoesNotAcceptLookalikeDomains() {
        val genuine = com.example.media.DownloadLinkAnalyzer.analyze("https://m.facebook.com/watch/")
        val lookalike = com.example.media.DownloadLinkAnalyzer.analyze("https://facebook.com.attacker.example/watch/")

        assertEquals(com.example.media.DownloadLinkAnalyzer.Provider.FACEBOOK, genuine.provider)
        assertEquals(null, lookalike.provider)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.WEB_PAGE, lookalike.route)
    }

    @Test
    fun sharedTextAcceptsBareDomainsAndHostPortValues() {
        val links = com.example.media.DownloadLinkAnalyzer.extractUrls(
            "example.com/video.mp4 and www.cdn.example.net:8443/files/movie?token=abc"
        )

        assertEquals(2, links.size)
        assertEquals("https://example.com/video.mp4", links[0])
        assertEquals("https://www.cdn.example.net:8443/files/movie?token=abc", links[1])
    }

    @Test
    fun snifferRecognizesHlsAndDashManifestsForUniversalExtraction() {
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaManifestUrl(
                "https://cdn.example.net/live/master.m3u8?token=abc"
            )
        )
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaManifestUrl(
                "https://cdn.example.net/live/manifest?id=42",
                "application/dash+xml"
            )
        )
        assertFalse(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/live/master.m3u8?token=abc"
            )
        )
    }

    @Test
    fun youtubePageAdviceUsesUniversalExtractorWhenSupported() {
        val analysis = com.example.media.DownloadLinkAnalyzer.analyze("https://youtu.be/abc123")
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.SOCIAL_PAGE, analysis.route)
        assertTrue(analysis.advice.contains("محرك المواقع"))
        assertFalse(analysis.advice.contains("لا يستخرج CinemaX"))
    }

    @Test
    fun sharedTextCanContainSeveralLinks() {
        val links = com.example.media.DownloadLinkAnalyzer.extractUrls(
            "Download these: https://example.com/a.pdf and https://cdn.example.net/video.mp4"
        )

        assertEquals(2, links.size)
        assertTrue(links[0].endsWith("a.pdf"))
        assertTrue(links[1].endsWith("video.mp4"))
    }

    @Test
    fun webSnifferRecognizesOpaqueVideoAndAudioUrlsButRejectsHtml() {
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/resource?id=video-123",
                "video/mp4"
            )
        )
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/audio?id=track-123",
                "audio/mpeg"
            )
        )
        assertFalse(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/page?id=123",
                "text/html"
            )
        )
    }

    @Test
    fun snifferAcceptsVideoMimeButRejectsAudioMimeAndPlaylists() {
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/opaque?id=123",
                "video/mp4"
            )
        )
        assertTrue(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/opaque?id=123",
                "audio/mpeg"
            )
        )
        assertFalse(
            com.example.media.WebVideoSnifferHelper.isMediaStreamUrl(
                "https://cdn.example.net/live/manifest.m3u8",
                "video/mp2t"
            )
        )
    }

    @Test
    fun playingVideoElementIsRankedAboveIncidentalNetworkRequest() {
        val networkRequest = com.example.media.CapturedVideoSource(
            url = "https://video.example.net/ad.mp4",
            pageUrl = "https://www.facebook.com/reel/123",
            requestHeaders = emptyMap(),
            sourceKind = "network"
        )
        val activeVideo = com.example.media.CapturedVideoSource(
            url = "https://video.example.net/actual-reel.mp4",
            pageUrl = "https://www.facebook.com/reel/123",
            requestHeaders = emptyMap(),
            sourceKind = "active-video",
            mimeType = "video/mp4",
            width = 720,
            height = 1280,
            durationMs = 18_000L,
            currentTimeMs = 5_000L,
            isPlaying = true
        )

        assertTrue(activeVideo.confidenceScore > networkRequest.confidenceScore)
    }

    @Test
    fun nonHttpSchemesAreRejected() {
        val result = com.example.media.DownloadLinkAnalyzer.analyze("file:///sdcard/video.mp4")
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.INVALID, result.route)
    }

    @Test
    fun urlNormalizationPreservesExplicitSchemesAndUpgradesPlainHosts() {
        assertEquals(
            "https://www.facebook.com/watch/?v=123",
            RealMediaDownloader.normalizeUrl("www.facebook.com/watch/?v=123")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            RealMediaDownloader.normalizeUrl("https://www.youtube.com/watch?v=abc")
        )
        assertEquals(
            "fb://profile/123",
            RealMediaDownloader.normalizeUrl("fb://profile/123")
        )
    }
}
