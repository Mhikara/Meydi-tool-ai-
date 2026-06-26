package com.example.downloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.downloader.model.DownloadStatus
import com.example.downloader.model.PlatformType

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val platform: PlatformType,
    val filePath: String,
    val fileSize: Long,
    val status: DownloadStatus,
    val timestamp: Long = System.currentTimeMillis()
)
