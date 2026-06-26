package com.example.logging

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_logs")
data class SystemLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: String,
    val timestamp: Long,
    val severity: String, // INFO, DEBUG, WARN, ERROR, CRITICAL
    val module: String,
    val functionName: String,
    val message: String,
    val deviceInfo: String,
    val androidVersion: String,
    val appVersion: String,
    val stackTrace: String?,
    val userId: String?,
    val sessionId: String?
)
