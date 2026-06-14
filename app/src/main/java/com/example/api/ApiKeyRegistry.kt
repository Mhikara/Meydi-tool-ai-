package com.example.api

import android.content.Context
import com.example.BuildConfig

object ApiKeyRegistry {
    private var appContext: Context? = null

    // Call this inside MainActivity or MeydiAiApp root to set application context securely
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs() = appContext?.getSharedPreferences("meydi_api_keys_pref", Context.MODE_PRIVATE)

    fun getGeminiKey(): String {
        val customKey = getPrefs()?.getString("gemini_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_GEMINI_API_KEY" || buildKey.length < 10) {
            com.example.ui.CrossPlatformUtils.COMMON_API_KEY
        } else {
            buildKey
        }
    }

    fun getFalKey(): String {
        val customKey = getPrefs()?.getString("fal_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.FAL_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_FAL_API_KEY") {
            com.example.ui.CrossPlatformUtils.COMMON_API_KEY
        } else {
            buildKey
        }
    }

    fun getAzbryKey(): String {
        val customKey = getPrefs()?.getString("azbry_key", "") ?: ""
        if (customKey.isNotBlank()) return customKey.trim()

        val buildKey = try { BuildConfig.AZBRY_API_KEY } catch (e: Throwable) { "" }
        return if (buildKey.isEmpty() || buildKey == "MY_AZBRY_API_KEY" || buildKey.startsWith("http")) {
            com.example.ui.CrossPlatformUtils.COMMON_API_KEY
        } else {
            buildKey
        }
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

    fun hasCustomGeminiKey(): Boolean = !getPrefs()?.getString("gemini_key", "").isNullOrBlank()
    fun hasCustomFalKey(): Boolean = !getPrefs()?.getString("fal_key", "").isNullOrBlank()
    fun hasCustomAzbryKey(): Boolean = !getPrefs()?.getString("azbry_key", "").isNullOrBlank()
}
