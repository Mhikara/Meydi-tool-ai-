package com.example.downloader.repository

import com.example.downloader.model.MediaFormat
import com.example.downloader.model.MediaInfo
import com.example.downloader.model.PlatformType
import kotlinx.coroutines.delay

class MediaRepository {

    suspend fun fetchMediaInfo(url: String): MediaInfo? {
        // Simulate network delay for API call
        delay(1500)
        
        val platform = detectPlatform(url)
        
        // Mock data representation matching official API shapes
        return MediaInfo(
            title = "Extracted Video from ${platform.displayName}",
            thumbnailUrl = "https://via.placeholder.com/640x360.png?text=Thumbnail+${platform.displayName}",
            platform = platform,
            duration = "03:45",
            formats = listOf(
                MediaFormat("f1", "1080p", "mp4", "45 MB"),
                MediaFormat("f2", "720p", "mp4", "22 MB"),
                MediaFormat("f3", "480p", "mp4", "12 MB"),
                MediaFormat("a1", "Audio", "mp3", "4 MB", isAudioOnly = true)
            )
        )
    }

    private fun detectPlatform(url: String): PlatformType {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> PlatformType.YOUTUBE
            lowerUrl.contains("tiktok.com") -> PlatformType.TIKTOK
            lowerUrl.contains("instagram.com") -> PlatformType.INSTAGRAM
            lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") -> PlatformType.TWITTER
            lowerUrl.contains("facebook.com") || lowerUrl.contains("fb.watch") -> PlatformType.FACEBOOK
            lowerUrl.contains("pinterest.com") || lowerUrl.contains("pin.it") -> PlatformType.PINTEREST
            lowerUrl.contains("reddit.com") -> PlatformType.REDDIT
            else -> PlatformType.UNKNOWN
        }
    }
}
