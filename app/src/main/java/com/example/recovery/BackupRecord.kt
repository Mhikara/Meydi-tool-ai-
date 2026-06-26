package com.example.recovery

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_records")
data class BackupRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val filePath: String,
    val checksum: String,
    val description: String,
    val isValid: Boolean
)
