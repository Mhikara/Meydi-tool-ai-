package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.ui.PromptTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class VideoJobState {
    object Idle : VideoJobState()
    object Submitting : VideoJobState()
    data class InQueue(val statusMessage: String) : VideoJobState()
    data class Processing(val logs: List<String>, val progressMessage: String) : VideoJobState()
    data class Completed(val videoUrl: String) : VideoJobState()
    data class Error(val message: String) : VideoJobState()
}

object FalVideoGenerator {
    private const val TAG = "FalVideoGenerator"
    private const val FAL_QUEUE_URL = "https://queue.fal.run/fal-ai/minimax-video" 

    private fun getApiKey(): String {
        return ApiKeyRegistry.getFalKey()
    }

    fun renderVideo(
        templateData: PromptTemplate,
        userPrompt: String,
        resolution: String = "1080p",
        frameRate: String = "30fps",
        aspectRatio: String = "16:9"
    ): Flow<VideoJobState> = flow {
        emit(VideoJobState.Submitting)
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                emit(VideoJobState.Error("FAL_KEY is missing in configuration"))
                return@flow
            }

            val combinedPrompt = """
                Template: ${templateData.title}
                Template Style/Prompt: ${templateData.prompt}
                User Instruction: $userPrompt
                Preferred Aspect Ratio: $aspectRatio
                Preferred Resolution: $resolution
                Preferred Frame Rate: $frameRate
            """.trimIndent()

            val url = URL(FAL_QUEUE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.readTimeout = 30_000 
            connection.connectTimeout = 30_000
            connection.setRequestProperty("Authorization", "Key ${apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("prompt", combinedPrompt)
                put("aspect_ratio", aspectRatio)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                emit(VideoJobState.Error("Failed to submit job ($responseCode): $errorStream"))
                return@flow
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val submitJson = JSONObject(response)
            
            val statusUrl = submitJson.optString("status_url")
            val responseUrl = submitJson.optString("response_url")
            
            if (statusUrl.isEmpty() || responseUrl.isEmpty()) {
                emit(VideoJobState.Error("Invalid response format from Fal queue API."))
                return@flow
            }

            // Polling Loop
            var isCompleted = false
            var errorMsg: String? = null
            
            while (!isCompleted) {
                delay(3000) // Poll every 3 seconds
                
                val statusConn = URL(statusUrl).openConnection() as HttpURLConnection
                statusConn.requestMethod = "GET"
                statusConn.setRequestProperty("Authorization", "Key ${apiKey}")
                
                if (statusConn.responseCode == 200) {
                    val statusText = statusConn.inputStream.bufferedReader().use { it.readText() }
                    val statusObj = JSONObject(statusText)
                    val statusStr = statusObj.optString("status", "")
                    
                    if (statusStr == "IN_PROGRESS") {
                        val logsArr = statusObj.optJSONArray("logs")
                        val logList = mutableListOf<String>()
                        if (logsArr != null) {
                            for (i in 0 until logsArr.length()) {
                                val logObj = logsArr.optJSONObject(i)
                                if (logObj != null) {
                                    val msg = logObj.optString("message", "")
                                    if (msg.isNotEmpty()) logList.add(msg)
                                } else {
                                    val logStr = logsArr.optString(i, "")
                                    if (logStr.isNotEmpty()) logList.add(logStr)
                                }
                            }
                        }
                        val progressMsg = "Rendering: ${logList.lastOrNull() ?: "Generating video..."}"
                        emit(VideoJobState.Processing(logList, progressMsg))
                    } else if (statusStr == "IN_QUEUE") {
                        val pos = statusObj.optInt("queue_position", 0)
                        emit(VideoJobState.InQueue("Waiting in queue... Position: $pos"))
                    } else if (statusStr == "COMPLETED") {
                        isCompleted = true
                    } else if (statusStr == "ERROR" || statusStr == "FAILED") {
                        errorMsg = "Job failed or encountered an error"
                        isCompleted = true
                    } else {
                        emit(VideoJobState.Processing(emptyList(), "Status: $statusStr"))
                    }
                } else {
                    emit(VideoJobState.Error("Error checking status: HTTP ${statusConn.responseCode}"))
                    return@flow
                }
            }

            if (errorMsg != null) {
                emit(VideoJobState.Error(errorMsg))
                return@flow
            }
            
            // Job completed, fetch the result
            val resultConn = URL(responseUrl).openConnection() as HttpURLConnection
            resultConn.requestMethod = "GET"
            resultConn.setRequestProperty("Authorization", "Key ${apiKey}")
            
            if (resultConn.responseCode == 200) {
                val resultText = resultConn.inputStream.bufferedReader().use { it.readText() }
                val resultObj = JSONObject(resultText)
                
                val videoUrl = if (resultObj.has("video")) {
                    resultObj.getJSONObject("video").optString("url")
                } else if (resultObj.has("url")) {
                    resultObj.optString("url")
                } else {
                    null
                }
                
                if (videoUrl != null) {
                    emit(VideoJobState.Completed(videoUrl))
                } else {
                    emit(VideoJobState.Error("Failed to parse video URL from completion response"))
                }
            } else {
                emit(VideoJobState.Error("Failed to fetch job result: HTTP ${resultConn.responseCode}"))
            }

        } catch (e: Exception) {
            emit(VideoJobState.Error("Failed to call Fal API: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
