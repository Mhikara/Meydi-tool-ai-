package com.example.downloader.model

import java.util.UUID

enum class DownloadStatus {
    IDLE, FETCHING_INFO, READY, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELED
}

enum class PlatformType(val displayName: String) {
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    TWITTER("X (Twitter)"),
    FACEBOOK("Facebook"),
    PINTEREST("Pinterest"),
    REDDIT("Reddit"),
    UNKNOWN("Universal")
}

data class MediaInfo(
    val title: String,
    val thumbnailUrl: String,
    val platform: PlatformType,
    val duration: String? = null,
    val formats: List<MediaFormat>
)

data class MediaFormat(
    val id: String,
    val resolution: String,
    val ext: String,
    val size: String,
    val isAudioOnly: Boolean = false
)

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val platform: PlatformType,
    val mediaInfo: MediaInfo? = null,
    val selectedFormat: MediaFormat? = null,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
