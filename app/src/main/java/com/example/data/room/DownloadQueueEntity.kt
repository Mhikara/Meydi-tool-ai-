package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_queue")
data class DownloadQueueEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val mediaType: String,
    val platform: String,
    val timestamp: Long,
    val status: String
)
