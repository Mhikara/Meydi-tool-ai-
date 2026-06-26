package com.example.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConfigManager @Inject constructor(
    private val context: Context
) {
    fun initializeFirebase() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val config = loadConfig()
            if (config != null && isConfigValid(config)) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(config.apiKey)
                    .setApplicationId(config.applicationId)
                    .setProjectId(config.projectId)
                    .setStorageBucket(config.storageBucket ?: "")
                    .setGcmSenderId(config.gcmSenderId ?: "")
                    .setDatabaseUrl(config.databaseUrl ?: "")
                    .build()
                
                FirebaseApp.initializeApp(context, options)
            } else {
                // Handle error: Config invalid or missing
            }
        }
    }

    private fun loadConfig(): FirebaseConfig? {
        // Mengambil kredensial dari ApiKeyRegistry (bisa dari input dinamis UI atau BuildConfig bawaan)
        return try {
            val apiKey = com.example.api.ApiKeyRegistry.getFirebaseApiKey()
            val appId = com.example.api.ApiKeyRegistry.getFirebaseAppId()
            val projectId = com.example.api.ApiKeyRegistry.getFirebaseProjectId()

            if (apiKey.isEmpty() || appId.isEmpty() || projectId.isEmpty()) {
                null
            } else {
                FirebaseConfig(
                    apiKey = apiKey,
                    applicationId = appId,
                    projectId = projectId
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isConfigValid(config: FirebaseConfig): Boolean {
        return config.apiKey.isNotEmpty() && config.applicationId.isNotEmpty() && config.projectId.isNotEmpty()
    }
}
