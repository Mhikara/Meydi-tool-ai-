package com.example.admin.model

import com.example.auth.model.UserRole

data class AuditLog(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetUid: String? = null,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String? = null,
    val deviceName: String? = null
)

data class SystemStats(
    val totalUsers: Int = 0,
    val onlineUsers: Int = 0,
    val premiumUsers: Int = 0,
    val totalRevenue: Double = 0.0,
    val serverStatus: String = "Online",
    val cpuUsage: Int = 0,
    val ramUsage: Int = 0,
    val activeApis: Int = 0
)
