package com.example.centralapi.database

import android.content.Context
import androidx.room.*
import com.example.centralapi.domain.ActivityLog
import com.example.centralapi.domain.CachedResponse

@Dao
interface CentralCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResponse(cache: CachedResponse)

    @Query("SELECT * FROM offline_cache WHERE cacheKey = :key")
    suspend fun getResponse(key: String): CachedResponse?

    @Query("DELETE FROM offline_cache WHERE cacheKey = :key")
    suspend fun clearResponse(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logActivity(log: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    suspend fun getActivityLogs(): List<ActivityLog>

    @Query("SELECT * FROM activity_logs WHERE synced = 0")
    suspend fun getUnsyncedLogs(): List<ActivityLog>

    @Query("UPDATE activity_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<String>)
}

@Database(entities = [CachedResponse::class, ActivityLog::class], version = 1, exportSchema = false)
abstract class CentralCacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CentralCacheDao

    companion object {
        @Volatile
        private var INSTANCE: CentralCacheDatabase? = null

        fun getDatabase(context: Context): CentralCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CentralCacheDatabase::class.java,
                    "central_api_cache_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
