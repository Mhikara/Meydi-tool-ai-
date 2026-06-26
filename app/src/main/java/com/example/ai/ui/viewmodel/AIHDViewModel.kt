package com.example.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.domain.model.AIConfig
import com.example.ai.domain.model.AIProcessingJob
import com.example.ai.domain.model.ProcessingStatus
import com.example.ai.domain.model.ProcessingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AIHDViewModel : ViewModel() {

    private val _jobs = MutableStateFlow<List<AIProcessingJob>>(emptyList())
    val jobs: StateFlow<List<AIProcessingJob>> = _jobs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _stats = MutableStateFlow(AIStats())
    val stats: StateFlow<AIStats> = _stats.asStateFlow()

    fun addJob(sourceUri: android.net.Uri, type: ProcessingType, config: AIConfig) {
        val newJob = AIProcessingJob(
            id = UUID.randomUUID().toString(),
            sourceUri = sourceUri,
            type = type,
            config = config,
            status = ProcessingStatus.QUEUED
        )
        _jobs.value = _jobs.value + newJob
        processJob(newJob)
    }

    private fun processJob(job: AIProcessingJob) {
        viewModelScope.launch {
            _isProcessing.value = true
            // Update status to PROCESSING
            updateJobStatus(job.id, ProcessingStatus.PROCESSING, 0.1f)
            
            // Simulate AI Processing
            kotlinx.coroutines.delay(2000)
            updateJobStatus(job.id, ProcessingStatus.PROCESSING, 0.5f)
            kotlinx.coroutines.delay(2000)
            
            updateJobStatus(job.id, ProcessingStatus.COMPLETED, 1.0f)
            _isProcessing.value = _jobs.value.any { it.status == ProcessingStatus.PROCESSING }
            
            // Update stats
            _stats.value = _stats.value.copy(
                totalProcessed = _stats.value.totalProcessed + 1,
                totalUpscaled = if (job.config.upscaleFactor > 1) _stats.value.totalUpscaled + 1 else _stats.value.totalUpscaled
            )
        }
    }

    private fun updateJobStatus(id: String, status: ProcessingStatus, progress: Float) {
        _jobs.value = _jobs.value.map {
            if (it.id == id) it.copy(status = status, progress = progress) else it
        }
    }
}

data class AIStats(
    val totalProcessed: Int = 0,
    val totalUpscaled: Int = 0,
    val totalSpaceSaved: String = "0 MB",
    val gpuStatus: String = "Active (Adreno 650)"
)
