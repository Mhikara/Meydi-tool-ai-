package com.example.centralapi.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

// Roles for access control
enum class UserRole {
    USER, PREMIUM, ADMIN, OWNER
}

// User model
data class UserProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val token: String? = null
)

// Dashboard data
data class DashboardStats(
    val totalUsers: Int,
    val activeSubscriptions: Int,
    val systemLoad: Double,
    val totalDownloads: Int
)

// Auto Update info
data class UpdateInfo(
    val latestVersion: String,
    val forceUpdate: Boolean,
    val downloadUrl: String,
    val changeLog: String
)

// Downloader task
data class DownloadTask(
    val id: String,
    val fileName: String,
    val url: String,
    val progress: Float,
    val status: String
)

// AI Request / Response
data class AiPromptRequest(val prompt: String, val maxTokens: Int = 500)
data class AiResponse(val result: String, val tokensUsed: Int)

// Sync Status
data class SyncState(val lastSyncTime: Long, val pendingChangesCount: Int, val isSyncing: Boolean)

// Feedback model
data class UserFeedback(val id: String, val rating: Int, val comment: String, val timestamp: Long)

// Activity Log Entity for Offline cache
@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val action: String,
    val timestamp: Long,
    val synced: Boolean
)

// Generic Cache Entity
@Entity(tableName = "offline_cache")
data class CachedResponse(
    @PrimaryKey val cacheKey: String,
    val jsonResponse: String,
    val timestamp: Long
)
