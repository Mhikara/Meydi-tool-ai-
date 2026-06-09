package com.example.db

import kotlinx.coroutines.flow.Flow

class DraftRepository(private val draftDao: DraftDao) {

    fun getDraftsByTypeAndEmail(type: String, email: String): Flow<List<ProjectDraft>> {
        return draftDao.getDraftsByTypeAndEmail(type, email)
    }

    suspend fun getLatestDraftByTypeAndEmail(type: String, email: String): ProjectDraft? {
        return draftDao.getLatestDraftByTypeAndEmail(type, email)
    }

    suspend fun saveDraft(draft: ProjectDraft) {
        draftDao.insertDraft(draft)
    }

    suspend fun updateDraft(id: Int, prompt: String, code: String) {
        draftDao.updateDraft(id, prompt, code)
    }
}
