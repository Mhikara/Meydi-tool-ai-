package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadQueueDao {
    @Query("SELECT * FROM download_queue ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<DownloadQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DownloadQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DownloadQueueEntity>)

    @Query("DELETE FROM download_queue WHERE id = :id")
    suspend fun deleteItemById(id: String)
    
    @Query("DELETE FROM download_queue")
    suspend fun clearAll()
}
