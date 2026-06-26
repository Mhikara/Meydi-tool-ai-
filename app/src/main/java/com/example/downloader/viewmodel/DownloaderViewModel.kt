package com.example.downloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.downloader.model.DownloadStatus
import com.example.downloader.model.DownloadTask
import com.example.downloader.model.MediaFormat
import com.example.downloader.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DownloaderViewModel : ViewModel() {

    private val repository = MediaRepository()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()
    
    private val _currentInputUrl = MutableStateFlow("")
    val currentInputUrl: StateFlow<String> = _currentInputUrl.asStateFlow()

    private val _analyzingUrl = MutableStateFlow(false)
    val analyzingUrl: StateFlow<Boolean> = _analyzingUrl.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    fun setUrl(url: String) {
        _currentInputUrl.value = url
    }

    fun analyzeUrl() {
        val url = _currentInputUrl.value
        if (url.isBlank()) return

        val taskId = UUID.randomUUID().toString()
        val newTask = DownloadTask(id = taskId, url = url, platform = com.example.downloader.model.PlatformType.UNKNOWN, status = DownloadStatus.FETCHING_INFO)
        
        _tasks.update { listOf(newTask) + it }
        _currentInputUrl.value = ""
        _analyzingUrl.value = true

        viewModelScope.launch {
            try {
                val mediaInfo = repository.fetchMediaInfo(url)
                if (mediaInfo != null) {
                    _tasks.update { list ->
                        list.map { if (it.id == taskId) it.copy(mediaInfo = mediaInfo, platform = mediaInfo.platform, status = DownloadStatus.READY) else it }
                    }
                } else {
                    _tasks.update { list ->
                        list.map { if (it.id == taskId) it.copy(status = DownloadStatus.FAILED, errorMessage = "Media not found") else it }
                    }
                }
            } catch (e: Exception) {
                _tasks.update { list ->
                    list.map { if (it.id == taskId) it.copy(status = DownloadStatus.FAILED, errorMessage = e.localizedMessage) else it }
                }
            } finally {
                _analyzingUrl.value = false
            }
        }
    }

    fun startDownload(taskId: String, format: MediaFormat) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) it.copy(selectedFormat = format, status = DownloadStatus.DOWNLOADING, progress = 0f) else it }
        }

        val job = viewModelScope.launch {
            // MOCK: Simulate a download process with progress updates
            for (i in 1..100) {
                delay(100) // Simulate network chunk
                val downloadedBytes = (i * 1024 * 1024).toLong() // Mock bytes
                val totalBytes = (100 * 1024 * 1024).toLong()
                
                _tasks.update { list ->
                    list.map { 
                        if (it.id == taskId && it.status == DownloadStatus.DOWNLOADING) {
                            it.copy(
                                progress = i / 100f,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speed = "${(3..8).random()} MB/s"
                            )
                        } else it 
                    }
                }
                
                // If status changed to PAUSED or CANCELED, break the loop
                val currentTask = _tasks.value.find { it.id == taskId }
                if (currentTask?.status != DownloadStatus.DOWNLOADING) break
            }
            
            // Mark as completed if it wasn't cancelled/paused
            val finalTask = _tasks.value.find { it.id == taskId }
            if (finalTask?.status == DownloadStatus.DOWNLOADING) {
                _tasks.update { list ->
                    list.map { if (it.id == taskId) it.copy(status = DownloadStatus.COMPLETED, progress = 1f) else it }
                }
            }
        }
        activeJobs[taskId] = job
    }

    fun pauseDownload(taskId: String) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) it.copy(status = DownloadStatus.PAUSED, speed = "") else it }
        }
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.selectedFormat != null) {
            _tasks.update { list ->
                list.map { if (it.id == taskId) it.copy(status = DownloadStatus.DOWNLOADING) else it }
            }
            // In a real app, calculate offset bytes and resume. Here we just mock resuming progress
            val job = viewModelScope.launch {
                val startProgress = (task.progress * 100).toInt()
                for (i in startProgress..100) {
                    delay(100)
                    _tasks.update { list ->
                        list.map { 
                            if (it.id == taskId && it.status == DownloadStatus.DOWNLOADING) {
                                it.copy(progress = i / 100f, speed = "${(3..8).random()} MB/s")
                            } else it 
                        }
                    }
                    val currentTask = _tasks.value.find { it.id == taskId }
                    if (currentTask?.status != DownloadStatus.DOWNLOADING) break
                }
                val finalTask = _tasks.value.find { it.id == taskId }
                if (finalTask?.status == DownloadStatus.DOWNLOADING) {
                    _tasks.update { list ->
                        list.map { if (it.id == taskId) it.copy(status = DownloadStatus.COMPLETED, progress = 1f) else it }
                    }
                }
            }
            activeJobs[taskId] = job
        }
    }

    fun cancelDownload(taskId: String) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) it.copy(status = DownloadStatus.CANCELED, progress = 0f) else it }
        }
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
    }

    fun removeTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        _tasks.update { list -> list.filter { it.id != taskId } }
    }
}
