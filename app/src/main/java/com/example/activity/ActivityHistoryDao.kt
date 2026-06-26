package com.example.activity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityHistory)

    @Query("SELECT * FROM activity_history ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityHistory>>

    @Query("SELECT * FROM activity_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getActivitiesByUserId(userId: String): Flow<List<ActivityHistory>>

    @Query("SELECT * FROM activity_history WHERE name = :name ORDER BY timestamp DESC")
    fun getActivitiesByName(name: String): Flow<List<ActivityHistory>>

    @Query("SELECT * FROM activity_history WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp DESC")
    fun getActivitiesByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ActivityHistory>>

    @Query("SELECT * FROM activity_history WHERE (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchActivities(query: String): Flow<List<ActivityHistory>>

    @Query("DELETE FROM activity_history")
    suspend fun clearAllActivities()
}
