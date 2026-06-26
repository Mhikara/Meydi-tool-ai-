package com.example.db

import androidx.room.TypeConverter
import com.example.downloader.model.DownloadStatus
import com.example.downloader.model.PlatformType

class Converters {
    @TypeConverter
    fun fromPlatformType(value: PlatformType): String {
        return value.name
    }

    @TypeConverter
    fun toPlatformType(value: String): PlatformType {
        return PlatformType.valueOf(value)
    }

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: Exception) {
            DownloadStatus.IDLE
        }
    }
}
