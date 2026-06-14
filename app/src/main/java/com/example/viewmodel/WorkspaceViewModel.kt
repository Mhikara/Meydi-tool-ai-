package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiGenerator
import com.example.api.MicrostockMetadata
import com.example.db.AppDatabase
import com.example.db.DraftRepository
import com.example.db.ProjectDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DraftRepository
    private var currentDraftId: Int? = null

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _codeContent = MutableStateFlow("")
    val codeContent: StateFlow<String> = _codeContent.asStateFlow()

    private val _selectedTemplateId = MutableStateFlow<String?>(null)
    val selectedTemplateId: StateFlow<String?> = _selectedTemplateId.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long?>(null)
    val lastSavedTime: StateFlow<Long?> = _lastSavedTime.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    private val _microstockMetadataMap = MutableStateFlow<Map<String, MicrostockMetadata>>(emptyMap())
    val microstockMetadataMap: StateFlow<Map<String, MicrostockMetadata>> = _microstockMetadataMap.asStateFlow()

    private val _microstockLoadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val microstockLoadingMap: StateFlow<Map<String, Boolean>> = _microstockLoadingMap.asStateFlow()

    private var autoSaveJob: Job? = null
    private var isInitialized = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DraftRepository(database.draftDao())
    }

    fun loadLatestDraft(type: String, email: String, defaultCode: String, defaultPrompt: String = "") {
        if (isInitialized) return
        isInitialized = true
        
        viewModelScope.launch {
            val draft = repository.getLatestDraftByTypeAndEmail(type, email)
            if (draft != null) {
                currentDraftId = draft.id
                _promptInput.value = draft.promptInput
                _codeContent.value = draft.codeContent
                _selectedTemplateId.value = draft.selectedTemplateId
                _lastSavedTime.value = draft.timestamp
            } else {
                _codeContent.value = defaultCode
                _promptInput.value = defaultPrompt
            }
        }
    }

    fun loadTemplateDirectly(prompt: String, code: String, templateId: String?, type: String, email: String) {
        _promptInput.value = prompt
        _codeContent.value = code
        _selectedTemplateId.value = templateId
        isInitialized = true
        viewModelScope.launch {
            val draft = repository.getLatestDraftByTypeAndEmail(type, email)
            if (draft != null) {
                currentDraftId = draft.id
                repository.updateDraft(draft.id, prompt, code, templateId)
            } else {
                val newDraft = ProjectDraft(
                    type = type,
                    projectTitle = "Draft $type",
                    promptInput = prompt,
                    codeContent = code,
                    selectedTemplateId = templateId,
                    userEmail = email
                )
                repository.saveDraft(newDraft)
                val savedDraft = repository.getLatestDraftByTypeAndEmail(type, email)
                if (savedDraft != null) {
                    currentDraftId = savedDraft.id
                }
            }
            _lastSavedTime.value = System.currentTimeMillis()
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

    fun updateSelectedTemplate(templateId: String?, type: String, email: String) {
        _selectedTemplateId.value = templateId
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
        val templateId = _selectedTemplateId.value
        
        if (currentDraftId != null) {
            repository.updateDraft(currentDraftId!!, prompt, code, templateId)
        } else {
            val newDraft = ProjectDraft(
                type = type,
                projectTitle = "Draft $$type",
                promptInput = prompt,
                codeContent = code,
                selectedTemplateId = templateId,
                userEmail = email
            )
            repository.saveDraft(newDraft)
        }
        _lastSavedTime.value = System.currentTimeMillis()
        _isBackingUp.value = false
        _saveEvent.tryEmit(Unit)
    }
    
    // Force manual save
    fun forceSaveCurrent(type: String, email: String) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave(type, email)
        }
    }

    fun analyzeTemplate(templateId: String, title: String, description: String) {
        viewModelScope.launch {
            _microstockLoadingMap.value = _microstockLoadingMap.value + (templateId to true)
            val result = GeminiGenerator.analyzeTemplateForMicrostock(title, description)
            _microstockLoadingMap.value = _microstockLoadingMap.value + (templateId to false)
            if (result != null) {
                _microstockMetadataMap.value = _microstockMetadataMap.value + (templateId to result)
            }
        }
    }

    fun getRemotionPresetsFlow(email: String) = repository.getDraftsByTypeAndEmail("REMOTION_PRESET", email)

    fun saveRemotionPreset(title: String, prompt: String, code: String, templateId: String?, email: String) {
        viewModelScope.launch {
            val newPreset = ProjectDraft(
                type = "REMOTION_PRESET",
                projectTitle = title,
                promptInput = prompt,
                codeContent = code,
                selectedTemplateId = templateId,
                userEmail = email
            )
            repository.saveDraft(newPreset)
        }
    }

    fun deleteRemotionPreset(id: Int) {
        viewModelScope.launch {
            repository.deleteDraft(id)
        }
    }
}
