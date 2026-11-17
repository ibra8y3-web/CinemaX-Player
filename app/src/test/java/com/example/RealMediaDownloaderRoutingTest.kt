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
