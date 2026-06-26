package com.example.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CachedData)

    @Query("SELECT * FROM offline_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCacheByKey(key: String): CachedData?

    @Query("DELETE FROM offline_cache WHERE cacheKey = :key")
    suspend fun deleteCacheByKey(key: String)

    @Query("DELETE FROM offline_cache")
    suspend fun clearAllCache()

    @Query("SELECT COUNT(*) FROM offline_cache")
    suspend fun getCacheCount(): Int

    @Query("SELECT * FROM offline_cache ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestCache(limit: Int): List<CachedData>
}
