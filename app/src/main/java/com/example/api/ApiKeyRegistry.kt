package com.example.api

import android.content.Context
import com.example.BuildConfig

object ApiKeyRegistry {
    private var appContext: Context? = null

    // Call this inside MainActivity or MeydiAiApp root to set application context securely
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getContext(): Context? = appContext

    private fun getPrefs() = appContext?.getSharedPreferences("meydi_api_keys_pref", Context.MODE_PRIVATE)

    private fun getBuildConfigString(fieldName: String): String {
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField(fieldName)
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun getGeminiKey(): String {
        val customKey = getPrefs()?.getString("gemini_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_GEMINI_API_KEY" || buildKey.length < 10) {
            ""
        } else {
            buildKey
        }
    }

    fun getFalKey(): String {
        val customKey = getPrefs()?.getString("fal_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.FAL_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_FAL_API_KEY") {
            ""
        } else {
            buildKey
        }
    }

    fun getAzbryKey(): String {
        val customKey = getPrefs()?.getString("azbry_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.AZBRY_API_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_AZBRY_API_KEY" || buildKey.startsWith("http")) {
            ""
        } else {
            buildKey
        }
    }

    fun getFirebaseApiKey(): String {
        val customKey = getPrefs()?.getString("firebase_api_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()
        val buildKey = getBuildConfigString("FIREBASE_API_KEY")
        return if (buildKey.isEmpty() || buildKey == "FIREBASE_API_KEY_DEFAULT_VALUE") "" else buildKey
    }

    fun getFirebaseAppId(): String {
        val customKey = getPrefs()?.getString("firebase_app_id", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()
        val buildKey = getBuildConfigString("FIREBASE_APP_ID")
        return if (buildKey.isEmpty() || buildKey == "FIREBASE_APP_ID_DEFAULT_VALUE") "" else buildKey
    }

    fun getFirebaseProjectId(): String {
        val customKey = getPrefs()?.getString("firebase_project_id", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()
        val buildKey = getBuildConfigString("FIREBASE_PROJECT_ID")
        return if (buildKey.isEmpty() || buildKey == "FIREBASE_PROJECT_ID_DEFAULT_VALUE") "" else buildKey
    }

    fun saveGeminiKey(key: String) {
        getPrefs()?.edit()?.putString("gemini_key", key.trim())?.apply()
    }

    fun saveFalKey(key: String) {
        getPrefs()?.edit()?.putString("fal_key", key.trim())?.apply()
    }

    fun saveAzbryKey(key: String) {
        getPrefs()?.edit()?.putString("azbry_key", key.trim())?.apply()
    }

    fun saveFirebaseApiKey(key: String) {
        getPrefs()?.edit()?.putString("firebase_api_key", key.trim())?.apply()
    }

    fun saveFirebaseAppId(id: String) {
        getPrefs()?.edit()?.putString("firebase_app_id", id.trim())?.apply()
    }

    fun saveFirebaseProjectId(id: String) {
        getPrefs()?.edit()?.putString("firebase_project_id", id.trim())?.apply()
    }

    fun hasCustomGeminiKey(): Boolean = !getPrefs()?.getString("gemini_key", "").isNullOrBlank()
    fun hasCustomFalKey(): Boolean = !getPrefs()?.getString("fal_key", "").isNullOrBlank()
    fun hasCustomAzbryKey(): Boolean = !getPrefs()?.getString("azbry_key", "").isNullOrBlank()
    fun hasCustomFirebase(): Boolean = !getPrefs()?.getString("firebase_api_key", "").isNullOrBlank()

    fun getSupabaseUrl(): String {
        val customUrl = getPrefs()?.getString("supabase_url", "") ?: ""
        if (customUrl.isNotBlank()) return customUrl.trim()
        val buildUrl = getBuildConfigString("SUPABASE_URL")
        return if (buildUrl.isEmpty() || buildUrl == "SUPABASE_URL_DEFAULT_VALUE") "https://your-supabase-project.supabase.co" else buildUrl
    }

    fun getSupabaseAnonKey(): String {
        val customKey = getPrefs()?.getString("supabase_anon_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()
        val buildKey = getBuildConfigString("SUPABASE_ANON_KEY")
        return if (buildKey.isEmpty() || buildKey == "SUPABASE_ANON_KEY_DEFAULT_VALUE") "" else buildKey
    }

    fun saveSupabaseUrl(url: String) {
        getPrefs()?.edit()?.putString("supabase_url", url.trim())?.apply()
    }

    fun saveSupabaseAnonKey(key: String) {
        getPrefs()?.edit()?.putString("supabase_anon_key", key.trim())?.apply()
    }

    fun hasCustomSupabase(): Boolean = !getPrefs()?.getString("supabase_url", "").isNullOrBlank() && !getPrefs()?.getString("supabase_anon_key", "").isNullOrBlank()
}
