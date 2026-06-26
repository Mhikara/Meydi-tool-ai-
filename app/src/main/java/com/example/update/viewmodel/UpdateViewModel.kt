package com.example.update.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.update.model.AppConfigEntity
import com.example.update.model.ChangelogEntity
import com.example.update.repository.UpdateRepository
import com.example.update.repository.SyncResult
import com.example.utils.AppUpdateInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository(application.applicationContext)

    // UI States
    private val _apkUpdateState = MutableStateFlow<ApkUpdateState>(ApkUpdateState.Idle)
    val apkUpdateState: StateFlow<ApkUpdateState> = _apkUpdateState.asStateFlow()

    private val _otaSyncState = MutableStateFlow<OtaSyncState>(OtaSyncState.Idle)
    val otaSyncState: StateFlow<OtaSyncState> = _otaSyncState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadStatusText = MutableStateFlow("")
    val downloadStatusText: StateFlow<String> = _downloadStatusText.asStateFlow()

    private val _changelogsHistory = MutableStateFlow<List<ChangelogEntity>>(emptyList())
    val changelogsHistory: StateFlow<List<ChangelogEntity>> = _changelogsHistory.asStateFlow()

    private val _configsCache = MutableStateFlow<List<AppConfigEntity>>(emptyList())
    val configsCache: StateFlow<List<AppConfigEntity>> = _configsCache.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Background checker job
    private var backgroundJob: Job? = null
    private val prefs = application.getSharedPreferences("meydiai_update_manager", Context.MODE_PRIVATE)

    init {
        // Load initial cached data
        viewModelScope.launch {
            repository.allConfigs.collect {
                _configsCache.value = it
            }
        }
        _isOfflineSimulated.value = prefs.getBoolean("sim_offline_mode", false)
        loadChangelogHistory()
        syncOtaConfigs()
        startBackgroundUpdateTimer()
    }

    // --- LOGIC CHECKS ---
    fun setOfflineSimulation(active: Boolean) {
        _isOfflineSimulated.value = active
        prefs.edit().putBoolean("sim_offline_mode", active).apply()
        // If coming back online, auto-trigger a sync!
        if (!active) {
            syncOtaConfigs()
        }
    }

    fun loadChangelogHistory() {
        viewModelScope.launch {
            try {
                val list = repository.getChangelogsHistory(_searchQuery.value)
                _changelogsHistory.value = list
            } catch (e: Exception) {
                // Ignore empty lists
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadChangelogHistory()
    }

    fun triggerManualCheck() {
        viewModelScope.launch {
            _apkUpdateState.value = ApkUpdateState.Checking
            try {
                val info = repository.checkForApkUpdate(forceRefresh = true)
                if (info != null) {
                    _apkUpdateState.value = ApkUpdateState.UpdateAvailable(info)
                } else {
                    _apkUpdateState.value = ApkUpdateState.UpToDate
                }
            } catch (e: Exception) {
                _apkUpdateState.value = ApkUpdateState.Error(e.localizedMessage ?: "Gagal memproses data update server.")
            }
        }
    }

    fun syncOtaConfigs() {
        viewModelScope.launch {
            _otaSyncState.value = OtaSyncState.Syncing
            try {
                when (val result = repository.syncOtaConfig()) {
                    is SyncResult.Success -> {
                        _otaSyncState.value = OtaSyncState.Success(result.configs, result.updatedCount)
                    }
                    is SyncResult.OfflineFallback -> {
                        _otaSyncState.value = OtaSyncState.OfflineFallback(result.cachedConfigs)
                    }
                    is SyncResult.RollbackTriggered -> {
                        _otaSyncState.value = OtaSyncState.RollbackTriggered(result.revertedConfigs, result.alertMessage)
                    }
                }
            } catch (e: Exception) {
                _otaSyncState.value = OtaSyncState.Error(e.localizedMessage ?: "Koneksi terganggu.")
            }
        }
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncOtaConfigs()
            try {
                val info = repository.checkForApkUpdate(forceRefresh = true)
                if (info != null) {
                    _apkUpdateState.value = ApkUpdateState.UpdateAvailable(info)
                } else {
                    _apkUpdateState.value = ApkUpdateState.UpToDate
                }
            } catch (e: Exception) {
                // Supress background pull errors to keep UI smooth
            }
            loadChangelogHistory()
            delay(500)
            _isRefreshing.value = false
        }
    }

    // --- SIMULATE DOWNLOAD PROGRESS ---
    fun startSimulatedDownload(downloadUrl: String) {
        viewModelScope.launch {
            _downloadProgress.value = 0.0f
            _downloadStatusText.value = "Mengamankan handshake dengan server CDN..."
            delay(1200)

            _downloadStatusText.value = "Mempersiapkan media storage dan memverifikasi package..."
            delay(1000)

            val totalSteps = 100
            for (i in 1..totalSteps) {
                delay(40) // Simulate download speeds
                _downloadProgress.value = i.toFloat() / totalSteps
                _downloadStatusText.value = "Mengunduh APK baru... ${i}% (${"%.1f".format(i * 0.15)} MB / 15.0 MB)"
            }

            _downloadStatusText.value = "Selesai mengunduh! Memverifikasi integritas file SHA-256..."
            delay(1200)

            _downloadStatusText.value = "Pemasangan paket siap! Silakan konfirmasi penginstalan di layar sistem."
            delay(1000)
            _downloadProgress.value = null
        }
    }

    fun clearDownloadState() {
        _downloadProgress.value = null
        _downloadStatusText.value = ""
    }

    // --- PERIODIC BACKGROUND TIMER ---
    private fun startBackgroundUpdateTimer() {
        backgroundJob?.cancel()
        backgroundJob = viewModelScope.launch {
            while (true) {
                delay(60000) // Run checks every 60 seconds
                if (!repository.isNetworkAvailable()) continue
                
                try {
                    val info = repository.checkForApkUpdate(forceRefresh = false)
                    if (info != null && _apkUpdateState.value !is ApkUpdateState.UpdateAvailable) {
                        _apkUpdateState.value = ApkUpdateState.UpdateAvailable(info)
                    }
                } catch (e: Exception) {
                    // Suppress periodic background errors
                }
            }
        }
    }

    // --- RECONNECT SYNC TRIGGERED BY UI SENSOR ---
    fun notifyNetworkReconnected() {
        if (!prefs.getBoolean("sim_offline_mode", false)) {
            syncOtaConfigs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        backgroundJob?.cancel()
    }
}

// Sealed states for APK Update
sealed class ApkUpdateState {
    object Idle : ApkUpdateState()
    object Checking : ApkUpdateState()
    data class UpdateAvailable(val info: AppUpdateInfo) : ApkUpdateState()
    object UpToDate : ApkUpdateState()
    data class Error(val message: String) : ApkUpdateState()
}

// Sealed states for OTA configuration syncing
sealed class OtaSyncState {
    object Idle : OtaSyncState()
    object Syncing : OtaSyncState()
    data class Success(val configs: List<AppConfigEntity>, val updatedCount: Int) : OtaSyncState()
    data class OfflineFallback(val cachedConfigs: List<AppConfigEntity>) : OtaSyncState()
    data class RollbackTriggered(val revertedConfigs: List<AppConfigEntity>, val message: String) : OtaSyncState()
    data class Error(val message: String) : OtaSyncState()
}
