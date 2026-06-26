package com.example.security

import android.content.Context
import android.content.SharedPreferences

class SecureStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("meydi_secure_preferences_v2", Context.MODE_PRIVATE)

    fun putString(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
            return
        }
        try {
            val encryptedValue = CryptoUtils.encrypt(value)
            prefs.edit().putString(key, encryptedValue).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        val encryptedValue = prefs.getString(key, null) ?: return defaultValue
        return try {
            CryptoUtils.decrypt(encryptedValue)
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val valueStr = getString(key, null) ?: return defaultValue
        return valueStr.toBoolean()
    }

    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val valueStr = getString(key, null) ?: return defaultValue
        return valueStr.toIntOrNull() ?: defaultValue
    }

    fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val valueStr = getString(key, null) ?: return defaultValue
        return valueStr.toLongOrNull() ?: defaultValue
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
    
    fun getAllDecrypted(): Map<String, String> {
        val all = prefs.all
        val decryptedMap = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (value is String) {
                try {
                    decryptedMap[key] = CryptoUtils.decrypt(value)
                } catch (e: Exception) {
                    decryptedMap[key] = "[DECRYPTION_ERROR: Kunci Berbeda]"
                }
            }
        }
        return decryptedMap
    }
    
    fun getAllRaw(): Map<String, String> {
        val all = prefs.all
        val rawMap = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (value is String) {
                rawMap[key] = value
            }
        }
        return rawMap
    }
}
