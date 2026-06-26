package com.example.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val collectionName: String,
    val docId: String,
    val dataJson: String,
    val operationType: String, // CREATE, UPDATE, DELETE
    val timestamp: Long,
    val retryCount: Int = 0
)
