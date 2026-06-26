package com.example.ai.domain.model

import android.net.Uri

data class AIProcessingJob(
    val id: String,
    val sourceUri: Uri,
    val targetUri: Uri? = null,
    val type: ProcessingType,
    val status: ProcessingStatus = ProcessingStatus.IDLE,
    val progress: Float = 0f,
    val config: AIConfig,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ProcessingType {
    IMAGE_HD, VIDEO_HD, SUPER_RESOLUTION, FACE_ENHANCEMENT, RESTORATION
}

enum class ProcessingStatus {
    IDLE, QUEUED, PROCESSING, COMPLETED, FAILED
}

data class AIConfig(
    val quality: QualityLevel = QualityLevel.AUTO,
    val upscaleFactor: Int = 1,
    val denoiseEnabled: Boolean = true,
    val sharpenEnabled: Boolean = true,
    val faceEnhanceEnabled: Boolean = false,
    val mode: ProcessingMode = ProcessingMode.HYBRID
)

enum class QualityLevel {
    LOW, MEDIUM, HIGH, ULTRA, AUTO
}

enum class ProcessingMode {
    OFFLINE, CLOUD, HYBRID
}
