package com.example.update.db

import androidx.room.*
import com.example.update.model.ChangelogEntity
import com.example.update.model.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {
    @Query("SELECT * FROM changelog_history ORDER BY releaseDate DESC")
    fun getAllChangelogsFlow(): Flow<List<ChangelogEntity>>

    @Query("SELECT * FROM changelog_history ORDER BY releaseDate DESC")
    suspend fun getAllChangelogs(): List<ChangelogEntity>

    @Query("SELECT * FROM changelog_history WHERE version = :version LIMIT 1")
    suspend fun getChangelogByVersion(version: String): ChangelogEntity?

    @Query("SELECT * FROM changelog_history WHERE version LIKE '%' || :query || '%' ORDER BY releaseDate DESC")
    suspend fun searchChangelogs(query: String): List<ChangelogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangelog(changelog: ChangelogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangelogs(changelogs: List<ChangelogEntity>)

    @Query("SELECT * FROM app_config_cache")
    fun getAllConfigsFlow(): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM app_config_cache")
    suspend fun getAllConfigs(): List<AppConfigEntity>

    @Query("SELECT * FROM app_config_cache WHERE configKey = :key LIMIT 1")
    suspend fun getConfigByKey(key: String): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<AppConfigEntity>)

    @Query("DELETE FROM app_config_cache WHERE configKey = :key")
    suspend fun deleteConfigByKey(key: String)

    @Query("DELETE FROM app_config_cache")
    suspend fun clearAllConfigs()
}
