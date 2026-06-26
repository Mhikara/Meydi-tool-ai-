package com.example.logging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SystemLog)

    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SystemLog>>

    @Query("SELECT * FROM system_logs WHERE severity = :severity ORDER BY timestamp DESC")
    fun getLogsBySeverity(severity: String): Flow<List<SystemLog>>

    @Query("SELECT * FROM system_logs WHERE message LIKE '%' || :query || '%' OR module LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<SystemLog>>

    @Query("DELETE FROM system_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteLogsOlderThan(olderThanTimestamp: Long)

    @Query("DELETE FROM system_logs")
    suspend fun clearAllLogs()
}
