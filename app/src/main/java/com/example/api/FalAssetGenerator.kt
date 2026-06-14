package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class AssetJobState {
    object Idle : AssetJobState()
    object Submitting : AssetJobState()
    data class InQueue(val statusMessage: String) : AssetJobState()
    data class Processing(val logs: List<String>, val progressMessage: String) : AssetJobState()
    data class Completed(val assetUrl: String) : AssetJobState()
    data class Error(val message: String) : AssetJobState()
}

object FalAssetGenerator {
    private const val TAG = "FalAssetGenerator"
    private const val FAL_QUEUE_URL = "https://queue.fal.run/fal-ai/flux/dev" 

    private fun getApiKey(): String {
        return ApiKeyRegistry.getFalKey()
    }

    fun generateAsset(
        prompt: String,
        imageSize: String = "landscape_16_9", // You can use standard sizes like "landscape_16_9", "portrait_4_3", "square"
        numImages: Int = 1
    ): Flow<AssetJobState> = flow {
        emit(AssetJobState.Submitting)
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                emit(AssetJobState.Error("FAL_KEY is missing in configuration"))
                return@flow
            }

            val url = URL(FAL_QUEUE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.readTimeout = 30_000 
            connection.connectTimeout = 30_000
            connection.setRequestProperty("Authorization", "Key ${apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("prompt", prompt)
                put("image_size", imageSize)
                put("num_images", numImages)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                emit(AssetJobState.Error("Failed to submit job ($responseCode): $errorStream"))
                return@flow
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val submitJson = JSONObject(response)
            
            val statusUrl = submitJson.optString("status_url")
            val responseUrl = submitJson.optString("response_url")
            
            if (statusUrl.isEmpty() || responseUrl.isEmpty()) {
                emit(AssetJobState.Error("Invalid response format from Fal queue API."))
                return@flow
            }

            // Polling Loop
            var isCompleted = false
            var errorMsg: String? = null
            
            while (!isCompleted) {
                delay(2000) // Poll every 2 seconds for images since they are faster
                
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
                        val progressMsg = "Generating: ${logList.lastOrNull() ?: "Applying brushstrokes..."}"
                        emit(AssetJobState.Processing(logList, progressMsg))
                    } else if (statusStr == "IN_QUEUE") {
                        val pos = statusObj.optInt("queue_position", 0)
                        emit(AssetJobState.InQueue("Waiting in queue... Position: $pos"))
                    } else if (statusStr == "COMPLETED") {
                        isCompleted = true
                    } else if (statusStr == "ERROR" || statusStr == "FAILED") {
                        errorMsg = "Job failed or encountered an error"
                        isCompleted = true
                    } else {
                        emit(AssetJobState.Processing(emptyList(), "Status: $statusStr"))
                    }
                } else {
                    emit(AssetJobState.Error("Error checking status: HTTP ${statusConn.responseCode}"))
                    return@flow
                }
            }

            if (errorMsg != null) {
                emit(AssetJobState.Error(errorMsg))
                return@flow
            }
            
            // Job completed, fetch the result
            val resultConn = URL(responseUrl).openConnection() as HttpURLConnection
            resultConn.requestMethod = "GET"
            resultConn.setRequestProperty("Authorization", "Key ${apiKey}")
            
            if (resultConn.responseCode == 200) {
                val resultText = resultConn.inputStream.bufferedReader().use { it.readText() }
                val resultObj = JSONObject(resultText)
                
                var assetUrl: String? = null
                
                // Flux models might return an array of images
                if (resultObj.has("images")) {
                    val imagesArr = resultObj.getJSONArray("images")
                    if (imagesArr.length() > 0) {
                        assetUrl = imagesArr.getJSONObject(0).optString("url")
                    }
                } else if (resultObj.has("url")) {
                    assetUrl = resultObj.optString("url")
                }
                
                if (assetUrl != null && assetUrl.isNotEmpty()) {
                    emit(AssetJobState.Completed(assetUrl))
                } else {
                    emit(AssetJobState.Error("Failed to parse asset URL from completion response"))
                }
            } else {
                emit(AssetJobState.Error("Failed to fetch job result: HTTP ${resultConn.responseCode}"))
            }

        } catch (e: Exception) {
            emit(AssetJobState.Error("Failed to call Fal API: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
