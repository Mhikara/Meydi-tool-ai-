package com.example.activity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_history")
data class ActivityHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val name: String,
    val description: String,
    val timestamp: Long,
    val userId: String,
    val deviceId: String,
    val ipAddress: String,
    val isSuccess: Boolean,
    val additionalDetails: String
)
