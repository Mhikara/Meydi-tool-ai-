package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoJobDao {
    @Query("SELECT * FROM video_jobs ORDER BY timestamp DESC")
    fun getAllJobs(): Flow<List<VideoJob>>

    @Insert
    suspend fun insertJob(job: VideoJob): Long

    @Update
    suspend fun updateJob(job: VideoJob)

    @Query("SELECT * FROM video_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: Int): VideoJob?
    
    @Query("DELETE FROM video_jobs WHERE id = :id")
    suspend fun deleteJob(id: Int)

    @Query("DELETE FROM video_jobs")
    suspend fun deleteAllJobs()
}
