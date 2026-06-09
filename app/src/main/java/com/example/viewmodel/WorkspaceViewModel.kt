package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.DraftRepository
import com.example.db.ProjectDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DraftRepository
    private var currentDraftId: Int? = null

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _codeContent = MutableStateFlow("")
    val codeContent: StateFlow<String> = _codeContent.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long?>(null)
    val lastSavedTime: StateFlow<Long?> = _lastSavedTime.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private var autoSaveJob: Job? = null
    private var isInitialized = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DraftRepository(database.draftDao())
    }

    fun loadLatestDraft(type: String, email: String, defaultCode: String) {
        if (isInitialized) return
        isInitialized = true
        
        viewModelScope.launch {
            val draft = repository.getLatestDraftByTypeAndEmail(type, email)
            if (draft != null) {
                currentDraftId = draft.id
                _promptInput.value = draft.promptInput
                _codeContent.value = draft.codeContent
                _lastSavedTime.value = draft.timestamp
            } else {
                _codeContent.value = defaultCode
            }
        }
    }

    fun updatePromptInput(newPrompt: String, type: String, email: String) {
        _promptInput.value = newPrompt
        scheduleAutoSave(type, email)
    }

    fun updateCodeContent(newCode: String, type: String, email: String) {
        _codeContent.value = newCode
        scheduleAutoSave(type, email)
    }

    private fun scheduleAutoSave(type: String, email: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500) // Debounce auto-save for 1.5 seconds
            performSave(type, email)
        }
    }

    private suspend fun performSave(type: String, email: String) {
        _isBackingUp.value = true
        delay(500) // Simulate cloud auto-backup sync delay
        val prompt = _promptInput.value
        val code = _codeContent.value
        
        if (currentDraftId != null) {
            repository.updateDraft(currentDraftId!!, prompt, code)
        } else {
            val newDraft = ProjectDraft(
                type = type,
                projectTitle = "Draft $$type",
                promptInput = prompt,
                codeContent = code,
                userEmail = email
            )
            repository.saveDraft(newDraft)
        }
        _lastSavedTime.value = System.currentTimeMillis()
        _isBackingUp.value = false
    }
    
    // Force manual save
    fun forceSaveCurrent(type: String, email: String) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave(type, email)
        }
    }
}
