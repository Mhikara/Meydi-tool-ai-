package com.example.security

import android.content.Context
import android.os.Build
import java.io.File

class SecurityManager(private val context: Context) {

    val apiKeyManager = ApiKeyManager(context)
    val spamProtectionManager = SpamProtectionManager.getInstance()

    init {
        auditAppSecurityStatus()
    }

    /**
     * Audit komprehensif terhadap kondisi keamanan aplikasi saat startup.
     */
    private fun auditAppSecurityStatus() {
        val isRooted = isDeviceRooted()
        val isEmulator = isRunningOnEmulator()
        
        spamProtectionManager.logEvent(
            "Security Audit Berjalan", 
            "INFO", 
            "Audit lingkungan selesai. Status Root: $isRooted, Status Emulator: $isEmulator"
        )
    }

    /**
     * Memeriksa apakah perangkat terindikasi di-root (Root Detection).
     * Mengecek keberadaan file biner superuser dan tags build sistem.
     */
    fun isDeviceRooted(): Boolean {
        // Cek tags build sistem
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // Cek lokasi file biner superuser (su) umum
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        // Cek eksekusi command 'su' secara runtime
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Memeriksa apakah aplikasi dijalankan pada Emulator (Emulator Detection).
     */
    fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.indexOf("sdk_google") != -1
                || Build.PRODUCT.indexOf("google_sdk") != -1
                || Build.PRODUCT.indexOf("sdk") != -1
                || Build.PRODUCT.indexOf("sdk_x86") != -1
                || Build.PRODUCT.indexOf("vbox86p") != -1
                || Build.BOARD.lowercase().contains("nox")
                || Build.BOOTLOADER.lowercase().contains("nox")
                || Build.HARDWARE.lowercase().contains("nox")
                || Build.PRODUCT.lowercase().contains("nox")
    }

    /**
     * Memvalidasi format payload request JSON/Teks sebelum dikirim
     */
    fun validateInputPayload(input: String): Boolean {
        // Mencegah potensi serangan input mencurigakan (XSS / SQL Injection sederhana)
        val suspiciousPatterns = listOf(
            "<script>", "</script>", "javascript:", "UNION SELECT", "OR '1'='1"
        )
        for (pattern in suspiciousPatterns) {
            if (input.contains(pattern, ignoreCase = true)) {
                spamProtectionManager.logEvent(
                    "Input Mencurigakan Terdeteksi", 
                    "CRITICAL", 
                    "Payload input diblokir karena mengandung pola mencurigakan: '$pattern'"
                )
                return false
            }
        }
        return true
    }

    /**
     * Melakukan Enkripsi data sensitif (seperti PIN, Catatan Rahasia, dll) menggunakan Android Keystore
     */
    fun encryptData(plainText: String): String {
        return CryptoUtils.encrypt(plainText)
    }

    /**
     * Melakukan Dekripsi data sensitif menggunakan Android Keystore
     */
    fun decryptData(encryptedBase64: String): String {
        return CryptoUtils.decrypt(encryptedBase64)
    }

    /**
     * Mengatur ulang konfigurasi perlindungan anti-spam secara berkala (Simulasi remote update)
     */
    fun updateSpamLimits(maxRequests: Int, windowMs: Long) {
        spamProtectionManager.maxRequestsPerWindow = maxRequests
        spamProtectionManager.windowSizeMs = windowMs
        spamProtectionManager.logEvent(
            "Konfigurasi Diperbarui", 
            "INFO", 
            "Batas rate-limit diperbarui: Maksimal $maxRequests request dalam ${windowMs / 1000} detik."
        )
    }
}
