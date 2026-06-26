package com.example.recovery

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BackupRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BackupRecord)

    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    suspend fun getAllBackupRecords(): List<BackupRecord>

    @Query("SELECT * FROM backup_records WHERE isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestValidBackup(): BackupRecord?

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteBackupRecord(id: Int)
}
