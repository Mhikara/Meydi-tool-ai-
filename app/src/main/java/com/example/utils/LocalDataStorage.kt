package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.DownloadQueueItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lokal storage mandiri untuk manajemen antrean offline dan sinkronisasi data Meydi AI
 */
class LocalDataStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("meydi_local_storage", Context.MODE_PRIVATE)

    fun saveQueue(queue: List<DownloadQueueItem>) {
        val jsonArray = JSONArray()
        for (item in queue) {
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("url", item.url)
                put("mediaType", item.mediaType)
                put("platform", item.platform)
                put("timestamp", item.timestamp)
                put("status", item.status)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("download_queue_v2", jsonArray.toString()).apply()
    }

    fun loadQueue(): List<DownloadQueueItem> {
        val jsonStr = prefs.getString("download_queue_v2", null) ?: return emptyList()
        val list = mutableListOf<DownloadQueueItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                list.add(
                    DownloadQueueItem(
                        id = jsonObject.getString("id"),
                        url = jsonObject.getString("url"),
                        mediaType = jsonObject.getString("mediaType"),
                        platform = jsonObject.getString("platform"),
                        timestamp = jsonObject.getLong("timestamp"),
                        status = jsonObject.getString("status")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
    
    fun clearQueue() {
        prefs.edit().remove("download_queue_v2").apply()
    }
    
    // Additional local configurations
    fun savePreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    fun getPreference(key: String, default: String = ""): String {
        return prefs.getString(key, default) ?: default
    }
}
