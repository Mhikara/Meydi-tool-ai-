package com.example.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_cache")
data class CachedData(
    @PrimaryKey val cacheKey: String,
    val encryptedValue: String, // Encrypted & compressed
    val timestamp: Long,
    val expiryDurationMs: Long // Duration in ms, 0 means no expiration
) {
    fun isExpired(): Boolean {
        if (expiryDurationMs <= 0) return false
        return System.currentTimeMillis() - timestamp > expiryDurationMs
    }
}
