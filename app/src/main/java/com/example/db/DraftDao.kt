package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM project_drafts WHERE type = :type AND userEmail = :email ORDER BY timestamp DESC")
    fun getDraftsByTypeAndEmail(type: String, email: String): Flow<List<ProjectDraft>>

    @Query("SELECT * FROM project_drafts WHERE type = :type AND userEmail = :email ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestDraftByTypeAndEmail(type: String, email: String): ProjectDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ProjectDraft)

    @Query("UPDATE project_drafts SET promptInput = :prompt, codeContent = :code, selectedTemplateId = :templateId, timestamp = :timestamp WHERE id = :id")
    suspend fun updateDraft(id: Int, prompt: String, code: String, templateId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE project_drafts SET promptInput = :prompt, codeContent = :code, selectedTemplateId = :templateId, timestamp = :timestamp, isSynced = :isSynced, lastCloudTimestamp = :lastCloudTimestamp WHERE id = :id")
    suspend fun updateDraftExtended(id: Int, prompt: String, code: String, templateId: String?, timestamp: Long, isSynced: Boolean, lastCloudTimestamp: Long)

    @Query("SELECT * FROM project_drafts WHERE isSynced = 0 AND userEmail = :email")
    suspend fun getUnsyncedDrafts(email: String): List<ProjectDraft>

    @Query("DELETE FROM project_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}
