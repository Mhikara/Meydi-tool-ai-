package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_jobs")
data class VideoJob(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateName: String,
    val prompt: String,
    val status: String,
    val videoUrl: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val resolution: String = "1080p",
    val frameRate: String = "30fps",
    val aspectRatio: String = "16:9",
    val renderTimeMs: Long = 0L
)
