package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiGenerator
import com.example.api.MicrostockMetadata
import com.example.db.AppDatabase
import com.example.db.DraftRepository
import com.example.db.ProjectDraft
import com.example.utils.FirebaseManager
import com.example.utils.NetworkMonitor
import com.example.utils.BackupLogger
import com.example.utils.Cryptographer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AutoSaveStatus {
    SAVING,          // "Menyimpan..."
    SAVED,           // "Tersimpan"
    FAILED,          // "Gagal menyimpan"
    PENDING_SYNC     // "Menunggu sinkronisasi"
}

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DraftRepository
    private var currentDraftId: Int? = null
    private var currentUserEmail: String = "guest"
    private val networkMonitor = NetworkMonitor(application)

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _codeContent = MutableStateFlow("")
    val codeContent: StateFlow<String> = _codeContent.asStateFlow()

    private val _selectedTemplateId = MutableStateFlow<String?>(null)
    val selectedTemplateId: StateFlow<String?> = _selectedTemplateId.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long?>(null)
    val lastSavedTime: StateFlow<Long?> = _lastSavedTime.asStateFlow()

    // Status Auto Save
    private val _autoSaveStatus = MutableStateFlow<AutoSaveStatus>(AutoSaveStatus.SAVED)
    val autoSaveStatus: StateFlow<AutoSaveStatus> = _autoSaveStatus.asStateFlow()

    // Backward compatibility mapper for UI code that checks isBackingUp
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    // Conflict & Auto Recovery States
    private val _recoveryAvailable = MutableStateFlow(false)
    val recoveryAvailable: StateFlow<Boolean> = _recoveryAvailable.asStateFlow()

    private val _localDraftToRecover = MutableStateFlow<ProjectDraft?>(null)
    val localDraftToRecover: StateFlow<ProjectDraft?> = _localDraftToRecover.asStateFlow()

    private val _cloudDraftToRecover = MutableStateFlow<ProjectDraft?>(null)
    val cloudDraftToRecover: StateFlow<ProjectDraft?> = _cloudDraftToRecover.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    private val _microstockMetadataMap = MutableStateFlow<Map<String, MicrostockMetadata>>(emptyMap())
    val microstockMetadataMap: StateFlow<Map<String, MicrostockMetadata>> = _microstockMetadataMap.asStateFlow()

    private val _microstockLoadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val microstockLoadingMap: StateFlow<Map<String, Boolean>> = _microstockLoadingMap.asStateFlow()

    private var autoSaveJob: Job? = null
    private var isInitialized = false

    // Local in-memory caches to avoid redundant db / network writes
    private var cachedPrompt = ""
    private var cachedCode = ""
    private var cachedTemplateId: String? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DraftRepository(database.draftDao())

        // Listen for internet connection recovery to trigger automatic background synchronization
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && currentUserEmail != "guest" && currentUserEmail.isNotBlank()) {
                    performBackgroundSync()
                }
            }
        }

        // Map autoSaveStatus to legacy isBackingUp to preserve compatibility with existing layout files
        viewModelScope.launch {
            autoSaveStatus.collect { status ->
                _isBackingUp.value = (status == AutoSaveStatus.SAVING)
            }
        }
    }

    fun loadLatestDraft(type: String, email: String, defaultCode: String, defaultPrompt: String = "") {
        currentUserEmail = email
        if (isInitialized) return
        isInitialized = true
        
        viewModelScope.launch {
            BackupLogger.log("INFO", "Menginisialisasi draf proyek ($type) untuk $email...")
            _autoSaveStatus.value = AutoSaveStatus.SAVING
            val localDraft = repository.getLatestDraftByTypeAndEmail(type, email)
            val cloudDraft = fetchLatestFromCloud(type, email)
            
            if (localDraft != null && cloudDraft != null) {
                val localTime = localDraft.timestamp
                val cloudTime = cloudDraft.timestamp
                
                // If timestamps are exactly the same, they are in sync
                if (localTime == cloudTime) {
                    applyDraft(localDraft)
                    _autoSaveStatus.value = AutoSaveStatus.SAVED
                    BackupLogger.log("SUCCESS", "Draf lokal dan cloud sinkron. Draf berhasil diterapkan.")
                } else {
                    // Conflict detected: Local and cloud versions have different edit timestamps!
                    // Show a recovery decision dialog to the user
                    _localDraftToRecover.value = localDraft
                    _cloudDraftToRecover.value = cloudDraft
                    _recoveryAvailable.value = true
                    _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                    BackupLogger.log("SYNC", "Konflik data terdeteksi! Lokal: ${localTime} vs Cloud: ${cloudTime}. Menunggu keputusan pemulihan dari pengguna.")
                }
            } else if (localDraft != null) {
                // Local exists, cloud is empty or inaccessible
                applyDraft(localDraft)
                BackupLogger.log("INFO", "Draf lokal ditemukan. Memeriksa koneksi untuk backup ke Cloud...")
                if (networkMonitor.isCurrentlyConnected()) {
                    val success = pushToCloud(localDraft)
                    if (success) {
                        repository.updateDraftExtended(
                            localDraft.id, localDraft.promptInput, localDraft.codeContent,
                            localDraft.selectedTemplateId, localDraft.timestamp, true, System.currentTimeMillis()
                        )
                        _autoSaveStatus.value = AutoSaveStatus.SAVED
                        BackupLogger.log("SUCCESS", "Berhasil mencadangkan draf lokal ke Cloud Firestore.")
                    } else {
                        _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                        BackupLogger.log("ERROR", "Gagal mengunggah draf ke Cloud. Menunggu sinkronisasi offline.")
                    }
                } else {
                    _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                    BackupLogger.log("INFO", "Perangkat offline. Draf lokal aman di SQLite perangkat.")
                }
            } else if (cloudDraft != null) {
                // Cloud exists, local is empty (e.g. app freshly installed or cleared data)
                // Save locally first to be offline-secure, then apply
                BackupLogger.log("INFO", "Draf lokal kosong, tetapi draf Cloud ditemukan. Menjalankan Auto Restore...")
                applyDraft(cloudDraft)
                repository.saveDraft(cloudDraft.copy(id = 0, isSynced = true, lastCloudTimestamp = cloudDraft.timestamp))
                val savedLocal = repository.getLatestDraftByTypeAndEmail(type, email)
                if (savedLocal != null) {
                    currentDraftId = savedLocal.id
                }
                _autoSaveStatus.value = AutoSaveStatus.SAVED
                BackupLogger.log("SUCCESS", "Auto Restore sukses! Draf dari Cloud berhasil diunduh dan disimpan ke lokal.")
            } else {
                // Brand new project
                _codeContent.value = defaultCode
                _promptInput.value = defaultPrompt
                _autoSaveStatus.value = AutoSaveStatus.SAVED
                BackupLogger.log("INFO", "Membuat sesi draf baru yang bersih.")
            }
        }
    }

    private fun applyDraft(draft: ProjectDraft) {
        currentDraftId = draft.id
        _promptInput.value = draft.promptInput
        _codeContent.value = draft.codeContent
        _selectedTemplateId.value = draft.selectedTemplateId
        _lastSavedTime.value = draft.timestamp
        
        cachedPrompt = draft.promptInput
        cachedCode = draft.codeContent
        cachedTemplateId = draft.selectedTemplateId
    }

    fun loadTemplateDirectly(prompt: String, code: String, templateId: String?, type: String, email: String) {
        _promptInput.value = prompt
        _codeContent.value = code
        _selectedTemplateId.value = templateId
        
        cachedPrompt = prompt
        cachedCode = code
        cachedTemplateId = templateId
        
        isInitialized = true
        viewModelScope.launch {
            _autoSaveStatus.value = AutoSaveStatus.SAVING
            val draft = repository.getLatestDraftByTypeAndEmail(type, email)
            val timestamp = System.currentTimeMillis()
            
            if (draft != null) {
                currentDraftId = draft.id
                repository.updateDraftExtended(draft.id, prompt, code, templateId, timestamp, false, 0L)
            } else {
                val newDraft = ProjectDraft(
                    type = type,
                    projectTitle = "Draft $type",
                    promptInput = prompt,
                    codeContent = code,
                    selectedTemplateId = templateId,
                    userEmail = email,
                    timestamp = timestamp,
                    isSynced = false
                )
                repository.saveDraft(newDraft)
                val savedDraft = repository.getLatestDraftByTypeAndEmail(type, email)
                if (savedDraft != null) {
                    currentDraftId = savedDraft.id
                }
            }
            _lastSavedTime.value = timestamp
            
            // Sync immediately
            if (networkMonitor.isCurrentlyConnected()) {
                val currentDraft = repository.getLatestDraftByTypeAndEmail(type, email)
                if (currentDraft != null) {
                    val success = pushToCloud(currentDraft)
                    if (success) {
                        repository.updateDraftExtended(
                            currentDraft.id, prompt, code, templateId, timestamp, true, System.currentTimeMillis()
                        )
                        _autoSaveStatus.value = AutoSaveStatus.SAVED
                    } else {
                        _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                    }
                }
            } else {
                _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
            }
            _saveEvent.tryEmit(Unit)
        }
    }

    fun updatePromptInput(newPrompt: String, type: String, email: String) {
        if (_promptInput.value == newPrompt) return // Avoid redundant triggers
        _promptInput.value = newPrompt
        scheduleAutoSave(type, email)
    }

    fun updateCodeContent(newCode: String, type: String, email: String) {
        if (_codeContent.value == newCode) return // Avoid redundant triggers
        _codeContent.value = newCode
        scheduleAutoSave(type, email)
    }

    fun updateSelectedTemplate(templateId: String?, type: String, email: String) {
        if (_selectedTemplateId.value == templateId) return
        _selectedTemplateId.value = templateId
        scheduleAutoSave(type, email)
    }

    private fun scheduleAutoSave(type: String, email: String) {
        autoSaveJob?.cancel()
        _autoSaveStatus.value = AutoSaveStatus.SAVING
        autoSaveJob = viewModelScope.launch {
            delay(1000) // 1 second debounce for optimal performance
            performSave(type, email)
        }
    }

    private fun validateData(prompt: String, code: String): Boolean {
        // Validation: Empty prompt and code simultaneously is invalid
        if (prompt.trim().isEmpty() && code.trim().isEmpty()) {
            return false
        }
        return true
    }

    private suspend fun performSave(type: String, email: String) {
        val prompt = _promptInput.value
        val code = _codeContent.value
        val templateId = _selectedTemplateId.value

        // Check if cached version is identical to avoid redundant writes
        if (prompt == cachedPrompt && code == cachedCode && templateId == cachedTemplateId) {
            _autoSaveStatus.value = AutoSaveStatus.SAVED
            return
        }

        // Validate data before committing
        if (!validateData(prompt, code)) {
            _autoSaveStatus.value = AutoSaveStatus.FAILED
            BackupLogger.log("ERROR", "Gagal menyimpan draf: Data kosong atau tidak valid.")
            return
        }

        _autoSaveStatus.value = AutoSaveStatus.SAVING
        val timestamp = System.currentTimeMillis()

        try {
            BackupLogger.log("INFO", "Menyimpan draf secara lokal di database Room...")
            // Step 1: Save locally in Room database (Crash & Offline safe)
            if (currentDraftId != null) {
                repository.updateDraftExtended(currentDraftId!!, prompt, code, templateId, timestamp, false, 0L)
            } else {
                val newDraft = ProjectDraft(
                    type = type,
                    projectTitle = "Draft $type",
                    promptInput = prompt,
                    codeContent = code,
                    selectedTemplateId = templateId,
                    userEmail = email,
                    timestamp = timestamp,
                    isSynced = false
                )
                repository.saveDraft(newDraft)
                val savedDraft = repository.getLatestDraftByTypeAndEmail(type, email)
                if (savedDraft != null) {
                    currentDraftId = savedDraft.id
                }
            }

            // Update local memory cache
            cachedPrompt = prompt
            cachedCode = code
            cachedTemplateId = templateId
            _lastSavedTime.value = timestamp
            BackupLogger.log("SUCCESS", "Draf berhasil disimpan di penyimpanan lokal (SQLite).")

            // Step 2: Sync to Cloud database if network is active
            if (networkMonitor.isCurrentlyConnected()) {
                BackupLogger.log("INFO", "Koneksi internet aktif. Memulai sinkronisasi cloud...")
                val latestLocal = if (currentDraftId != null) {
                    repository.getLatestDraftByTypeAndEmail(type, email)
                } else null
                
                if (latestLocal != null) {
                    val cloudSuccess = pushToCloud(latestLocal)
                    if (cloudSuccess) {
                        repository.updateDraftExtended(
                            latestLocal.id, prompt, code, templateId, timestamp, true, System.currentTimeMillis()
                        )
                        _autoSaveStatus.value = AutoSaveStatus.SAVED
                        BackupLogger.log("SUCCESS", "Sinkronisasi Cloud Berhasil! Enkripsi AES aktif.")
                    } else {
                        _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                        BackupLogger.log("ERROR", "Gagal menghubungi Firestore. Draf ditandai untuk sinkronisasi ulang.")
                    }
                } else {
                    _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                }
            } else {
                _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                BackupLogger.log("INFO", "Perangkat offline. Transmisi ke cloud ditunda.")
            }
            
            _saveEvent.tryEmit(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _autoSaveStatus.value = AutoSaveStatus.FAILED
            BackupLogger.log("ERROR", "Terjadi kegagalan I/O sistem: ${e.localizedMessage}")
        }
    }

    // Force manual save (L2)
    fun forceSaveCurrent(type: String, email: String) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave(type, email)
        }
    }

    // Background Sync trigger when recovering connectivity
    private suspend fun performBackgroundSync() {
        val unsynced = repository.getUnsyncedDrafts(currentUserEmail)
        if (unsynced.isNotEmpty()) {
            BackupLogger.log("SYNC", "Koneksi kembali terdeteksi! Memulai Background Auto Sync untuk ${unsynced.size} draf...")
            _autoSaveStatus.value = AutoSaveStatus.SAVING
            var hasFailures = false
            unsynced.forEach { draft ->
                val success = pushToCloud(draft)
                if (success) {
                    repository.updateDraftExtended(
                        id = draft.id,
                        prompt = draft.promptInput,
                        code = draft.codeContent,
                        templateId = draft.selectedTemplateId,
                        timestamp = draft.timestamp,
                        isSynced = true,
                        lastCloudTimestamp = System.currentTimeMillis()
                    )
                    BackupLogger.log("SUCCESS", "Otomatis sinkronisasi draf ${draft.type} ke Cloud sukses.")
                } else {
                    hasFailures = true
                    BackupLogger.log("ERROR", "Sinkronisasi otomatis draf ${draft.type} tertunda.")
                }
            }
            _autoSaveStatus.value = if (hasFailures) AutoSaveStatus.PENDING_SYNC else AutoSaveStatus.SAVED
        }
    }

    private suspend fun pushToCloud(draft: ProjectDraft): Boolean {
        val firestore = FirebaseManager.firestore ?: return false
        val email = draft.userEmail
        if (email.isBlank() || email == "guest") return false
        
        return try {
            val docId = "${email.replace(".", "_")}_${draft.type}"
            val encryptedPrompt = com.example.utils.Cryptographer.encrypt(draft.promptInput)
            val encryptedCode = com.example.utils.Cryptographer.encrypt(draft.codeContent)
            
            val data = mapOf(
                "type" to draft.type,
                "projectTitle" to draft.projectTitle,
                "promptInput" to encryptedPrompt,
                "codeContent" to encryptedCode,
                "selectedTemplateId" to draft.selectedTemplateId,
                "userEmail" to draft.userEmail,
                "timestamp" to draft.timestamp,
                "isEncrypted" to true
            )
            firestore.collection("project_drafts").document(docId).set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchLatestFromCloud(type: String, email: String): ProjectDraft? {
        val firestore = FirebaseManager.firestore ?: return null
        if (email.isBlank() || email == "guest") return null
        return try {
            val docId = "${email.replace(".", "_")}_${type}"
            val doc = firestore.collection("project_drafts").document(docId).get().await()
            if (doc.exists()) {
                val rawPrompt = doc.getString("promptInput") ?: ""
                val rawCode = doc.getString("codeContent") ?: ""
                val isEncrypted = doc.getBoolean("isEncrypted") ?: false
                
                val decryptedPrompt = if (isEncrypted) com.example.utils.Cryptographer.decrypt(rawPrompt) else rawPrompt
                val decryptedCode = if (isEncrypted) com.example.utils.Cryptographer.decrypt(rawCode) else rawCode

                ProjectDraft(
                    id = 0,
                    type = doc.getString("type") ?: type,
                    projectTitle = doc.getString("projectTitle") ?: "Draft $type",
                    promptInput = decryptedPrompt,
                    codeContent = decryptedCode,
                    selectedTemplateId = doc.getString("selectedTemplateId"),
                    userEmail = doc.getString("userEmail") ?: email,
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    isSynced = true,
                    lastCloudTimestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Resolve Conflict - Option 1: Use local version
    fun resolveConflictUseLocal(type: String, email: String) {
        val draft = _localDraftToRecover.value ?: return
        viewModelScope.launch {
            BackupLogger.log("SYNC", "Resolusi Konflik: Memilih menggunakan versi draf Lokal.")
            applyDraft(draft)
            _recoveryAvailable.value = false
            _localDraftToRecover.value = null
            _cloudDraftToRecover.value = null
            
            _autoSaveStatus.value = AutoSaveStatus.SAVING
            val success = pushToCloud(draft)
            if (success) {
                repository.updateDraftExtended(
                    draft.id, draft.promptInput, draft.codeContent,
                    draft.selectedTemplateId, draft.timestamp, true, System.currentTimeMillis()
                )
                _autoSaveStatus.value = AutoSaveStatus.SAVED
                BackupLogger.log("SUCCESS", "Pilihan draf lokal sukses diterapkan dan di-sinkronkan ke Cloud.")
            } else {
                _autoSaveStatus.value = AutoSaveStatus.PENDING_SYNC
                BackupLogger.log("INFO", "Draf lokal diterapkan. Backup cloud tertunda karena masalah koneksi.")
            }
        }
    }

    // Resolve Conflict - Option 2: Use cloud version
    fun resolveConflictUseCloud(type: String, email: String) {
        val draft = _cloudDraftToRecover.value ?: return
        viewModelScope.launch {
            BackupLogger.log("SYNC", "Resolusi Konflik: Memilih menggunakan versi draf Cloud.")
            _recoveryAvailable.value = false
            _localDraftToRecover.value = null
            _cloudDraftToRecover.value = null
            
            applyDraft(draft)
            if (currentDraftId != null) {
                repository.updateDraftExtended(
                    currentDraftId!!, draft.promptInput, draft.codeContent,
                    draft.selectedTemplateId, draft.timestamp, true, draft.timestamp
                )
            } else {
                repository.saveDraft(draft.copy(id = 0, isSynced = true, lastCloudTimestamp = draft.timestamp))
                val savedLocal = repository.getLatestDraftByTypeAndEmail(type, email)
                if (savedLocal != null) {
                    currentDraftId = savedLocal.id
                }
            }
            _autoSaveStatus.value = AutoSaveStatus.SAVED
            BackupLogger.log("SUCCESS", "Pilihan draf cloud berhasil diunduh, disimpan di SQLite, dan diterapkan di editor.")
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
                userEmail = email,
                timestamp = System.currentTimeMillis(),
                isSynced = false
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
