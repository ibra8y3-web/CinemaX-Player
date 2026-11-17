package com.example

import com.example.media.RealMediaDownloader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, pdf.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, archive.route)
        assertEquals(com.example.media.DownloadLinkAnalyzer.Route.DIRECT_FILE, audio.route)
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
    fun sharedTextCanContainSeveralLinks() {
        val links = com.example.media.DownloadLinkAnalyzer.extractUrls(
            "Download these: https://example.com/a.pdf and https://cdn.example.net/video.mp4"
        )

        assertEquals(2, links.size)
        assertTrue(links[0].endsWith("a.pdf"))
        assertTrue(links[1].endsWith("video.mp4"))
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
