package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_drafts")
data class ProjectDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // e.g. "CANVAS", "REMOTION"
    val projectTitle: String,
    val promptInput: String,
    val codeContent: String,
    val selectedTemplateId: String? = null,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val lastCloudTimestamp: Long = 0L
)
