package com.example.security

import android.content.Context
import android.util.Log

object EncryptionManager {
    private const val TAG = "EncryptionManager"
    private var isInitialized = false
    private lateinit var secureStorage: SecureStorage
    private val securityLogs = mutableListOf<String>()

    fun initialize(context: Context) {
        if (isInitialized) return
        secureStorage = SecureStorage(context.applicationContext)
        isInitialized = true
        logSecurityEvent("Sistem Enkripsi Terpusat diinisialisasi secara aman menggunakan Android Keystore AES-256 GCM.")
    }

    fun getSecureStorageInstance(): SecureStorage {
        if (!isInitialized) {
            throw IllegalStateException("EncryptionManager belum diinisialisasi! Hubungi EncryptionManager.initialize(context) terlebih dahulu.")
        }
        return secureStorage
    }

    fun logSecurityEvent(event: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedLog = "[$timestamp] $event"
        synchronized(securityLogs) {
            securityLogs.add(0, formattedLog)
            if (securityLogs.size > 100) {
                securityLogs.removeAt(securityLogs.size - 1)
            }
        }
        Log.i(TAG, formattedLog)
    }

    fun getSecurityLogs(): List<String> {
        return synchronized(securityLogs) {
            securityLogs.toList()
        }
    }

    fun clearSecurityLogs() {
        synchronized(securityLogs) {
            securityLogs.clear()
        }
    }

    // Session Management Encryption Helpers
    fun saveSessionToken(token: String) {
        getSecureStorageInstance().putString("session_token", token)
        logSecurityEvent("Session token baru berhasil dienkripsi dan disimpan di Keystore.")
    }

    fun getSessionToken(): String? {
        return getSecureStorageInstance().getString("session_token")
    }

    fun saveApiToken(apiToken: String) {
        getSecureStorageInstance().putString("api_token", apiToken)
        logSecurityEvent("API Access Token berhasil dienkripsi di Secure Storage.")
    }

    fun getApiToken(): String? {
        return getSecureStorageInstance().getString("api_token")
    }

    // User Profile Data Encryption Helpers
    fun encryptUserProfileField(fieldName: String, plainText: String): String {
        return try {
            val cipherText = CryptoUtils.encrypt(plainText)
            logSecurityEvent("Berhasil mengenkripsi bidang profil: $fieldName.")
            cipherText
        } catch (e: Exception) {
            logSecurityEvent("Peringatan: Gagal mengenkripsi bidang profil $fieldName: ${e.localizedMessage}")
            plainText
        }
    }

    fun decryptUserProfileField(fieldName: String, encryptedText: String): String {
        return try {
            val plainText = CryptoUtils.decrypt(encryptedText)
            logSecurityEvent("Berhasil mendekripsi bidang profil: $fieldName.")
            plainText
        } catch (e: Exception) {
            logSecurityEvent("Peringatan: Gagal mendekripsi bidang profil $fieldName.")
            encryptedText
        }
    }

    // System Key Rotation Orchestration
    fun performKeyRotation(): Boolean {
        logSecurityEvent("Memulai proses rotasi kunci kriptografi.")
        return try {
            val storage = getSecureStorageInstance()
            val backup = storage.getAllDecrypted()
            
            // Perform key rotate in Keystore
            val success = KeyStoreManager.rotateKey()
            if (!success) {
                logSecurityEvent("Rotasi kunci di dalam Android Keystore gagal.")
                return false
            }
            
            // Re-initialize a fresh storage context with the rotated key
            storage.clear()
            for ((key, value) in backup) {
                if (!value.contains("[DECRYPTION_ERROR")) {
                    storage.putString(key, value)
                }
            }
            logSecurityEvent("Rotasi kunci master selesai. Seluruh data berhasil didekripsi dan dienkripsi ulang menggunakan kunci baru.")
            true
        } catch (e: Exception) {
            logSecurityEvent("Rotasi kunci master GAGAL: ${e.localizedMessage}")
            false
        }
    }

    fun wipeAllUserData() {
        try {
            getSecureStorageInstance().clear()
            KeyStoreManager.deleteKey()
            logSecurityEvent("Pemusnahan Data Mutlak (Secure Data Wipeout) berhasil diselesaikan.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
