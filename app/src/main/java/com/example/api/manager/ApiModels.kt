package com.example.api.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class ApiEndpoint(
    val id: String,
    val name: String,
    val url: String,
    var isHealthy: Boolean = true,
    var priority: Int = 0,
    var latency: Long = -1
)

enum class ApiKeyStatus {
    VALID, INVALID, EXPIRED, RATE_LIMITED, UNKNOWN
}

data class ApiKeyModel(
    val id: String,
    val key: String,
    val provider: String,
    var status: ApiKeyStatus = ApiKeyStatus.UNKNOWN,
    var usageCount: Int = 0,
    val expiryDate: Long? = null
)

class ApiSecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_api_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSecretKey(id: String, key: String) {
        sharedPrefs.edit().putString(id, key).apply()
    }

    fun getSecretKey(id: String): String? {
        return sharedPrefs.getString(id, null)
    }
}
