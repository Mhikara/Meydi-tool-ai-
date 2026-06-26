package com.example.autoclipper.ai

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.delay

data class DetectedObjectResult(
    val boundingBox: Rect,
    val trackingId: Int?,
    val labels: List<String>
)

class ObjectDetectionEngine {
    
    /**
     * MOCK: Detects objects in a given bitmap.
     */
    suspend fun detectObjects(bitmap: Bitmap): List<DetectedObjectResult> {
        delay(1200) // Simulate processing time
        
        // Mock result: A bounding box in the center of the image
        val width = bitmap.width
        val height = bitmap.height
        
        val rect = Rect(
            (width * 0.2).toInt(),
            (height * 0.2).toInt(),
            (width * 0.8).toInt(),
            (height * 0.8).toInt()
        )
        
        return listOf(
            DetectedObjectResult(
                boundingBox = rect,
                trackingId = 1,
                labels = listOf("Object (Simulated)")
            )
        )
    }
}
