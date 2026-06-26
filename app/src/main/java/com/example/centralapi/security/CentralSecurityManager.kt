package com.example.centralapi.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.centralapi.domain.UserRole

class CentralSecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "central_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_APP_API_KEY = "app_api_key"
        private const val KEY_USER_TOKEN = "user_jwt_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
    }

    // Default configuration from Secrets panel / Env
    init {
        // If app_api_key is empty, we can seed it with a placeholder or default value
        if (getAppApiKey().isNullOrEmpty()) {
            // Seed safely from BuildConfig if available
            try {
                val defaultKey = com.example.api.ApiKeyRegistry.getFirebaseApiKey()
                if (defaultKey.isNotEmpty()) {
                    saveAppApiKey(defaultKey)
                } else {
                    saveAppApiKey("MEYDIAI_DEFAULT_SECURE_API_KEY_2026")
                }
            } catch (e: Exception) {
                saveAppApiKey("MEYDIAI_DEFAULT_SECURE_API_KEY_2026")
            }
        }
    }

    fun saveAppApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_APP_API_KEY, apiKey).apply()
    }

    fun getAppApiKey(): String? {
        return securePrefs.getString(KEY_APP_API_KEY, null)
    }

    fun saveUserToken(token: String?) {
        securePrefs.edit().putString(KEY_USER_TOKEN, token).apply()
    }

    fun getUserToken(): String? {
        return securePrefs.getString(KEY_USER_TOKEN, null)
    }

    fun saveUserRole(role: UserRole) {
        securePrefs.edit().putString(KEY_USER_ROLE, role.name).apply()
    }

    fun getUserRole(): UserRole {
        val roleStr = securePrefs.getString(KEY_USER_ROLE, UserRole.USER.name)
        return try {
            UserRole.valueOf(roleStr ?: UserRole.USER.name)
        } catch (e: Exception) {
            UserRole.USER
        }
    }

    fun saveUserEmail(email: String?) {
        securePrefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return securePrefs.getString(KEY_USER_EMAIL, null)
    }

    fun clearAll() {
        securePrefs.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_EMAIL)
            .apply()
    }
}
