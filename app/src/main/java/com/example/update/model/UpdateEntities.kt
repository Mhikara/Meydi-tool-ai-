package com.example.update.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "changelog_history")
data class ChangelogEntity(
    @PrimaryKey val version: String,
    val releaseDate: Long,
    val featuresList: String, // Comma-separated or bullet list
    val bugFixesList: String,
    val perfGainsList: String,
    val securityList: String,
    val sizeBytes: Long,
    val isForceUpdate: Boolean,
    val downloadUrl: String
)

@Entity(tableName = "app_config_cache")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String,
    val updatedAt: Long,
    val versionHash: String,
    val category: String
)
