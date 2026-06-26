package com.example.security

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException

// Exception kustom jika request diblokir oleh sistem anti-spam lokal
class RateLimitExceededException(message: String) : IOException(message)
class DuplicateRequestException(message: String) : IOException(message)

/**
 * RequestInterceptor mensimulasikan interseptor jaringan (seperti pada OkHttp Interceptor)
 * namun dirancang agar kompatibel dengan HttpURLConnection bawaan proyek ini.
 * Interseptor ini melakukan integrasi utuh antara Proteksi API Key dan Sistem Anti-Spam.
 */
class RequestInterceptor(private val context: Context) {

    private val apiKeyManager = ApiKeyManager(context)
    private val spamProtectionManager = SpamProtectionManager.getInstance()

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    /**
     * Memproses request secara aman (Intercept & Execute).
     * Melakukan pembungkusan keamanan pra-request, penanganan tanda tangan, caching,
     * auto-retry, dan pencatatan audit log.
     * 
     * @param url URL tujuan request
     * @param method Metode HTTP (GET, POST, dll)
     * @param payload Body request (kosongkan jika GET)
     * @param networkAction Aksi pengeksekusian request jaringan yang mengembalikan String (Response)
     */
    fun executeSecureRequest(
        userId: String,
        url: String,
        method: String,
        payload: String = "",
        networkAction: () -> String
    ): String = runBlocking {

        // 1. Integrasi Anti Spam: Pemeriksaan Rate Limit Lokal sebelum mengirim request ke luar
        val (isAllowed, rateLimitMsg) = spamProtectionManager.checkRequestPermission(userId, method)
        if (!isAllowed) {
            throw RateLimitExceededException(rateLimitMsg)
        }

        // 2. Integrasi Anti Spam: Deteksi Request Ganda (Duplicate Request Detection)
        val cachePayloadKey = "$method|$url|$payload"
        if (method == "POST" || method == "PUT") {
            val isUnique = spamProtectionManager.registerActiveRequest(cachePayloadKey)
            if (!isUnique) {
                throw DuplicateRequestException("Gagal: Request ganda terdeteksi. Silakan tunggu sebentar.")
            }
        }

        // 3. Integrasi Anti Spam: Caching Lokal untuk metode GET
        if (method == "GET") {
            val cachedResponse = spamProtectionManager.getFromCache(cachePayloadKey)
            if (cachedResponse != null) {
                spamProtectionManager.logEvent(
                    "Hit Cache Lokal", 
                    "INFO", 
                    "Mengembalikan data cache untuk menghemat sumber daya API."
                )
                return@runBlocking cachedResponse
            }
        }

        // 4. Integrasi Proteksi API Key: Validasi keabsahan API Key sebelum digunakan
        if (!apiKeyManager.isApiKeyValid()) {
            spamProtectionManager.logEvent(
                "Pemuatan API Key Otomatis", 
                "INFO", 
                "API Key lokal tidak valid/kedaluwarsa. Melakukan pembaharuan otomatis..."
            )
            val fetchSuccess = apiKeyManager.fetchApiKeyFromBackend()
            if (!fetchSuccess) {
                throw IOException("Keamanan API Gagal: Tidak dapat mengambil kunci otorisasi yang valid.")
            }
        }

        // 5. Integrasi Proteksi API Key: Sematkan Header Signature Kriptografis (Anti Replay & Tamper-proof)
        val timestamp = System.currentTimeMillis()
        val nonce = apiKeyManager.generateSecureNonce()
        val signature = apiKeyManager.generateRequestSignature(timestamp, nonce, payload)

        val secureHeaders = mapOf(
            "X-API-Key" to (apiKeyManager.getLocalApiKey()?.take(8) + "..." ?: ""), // Kirimkan penanda kunci tersensor
            "X-Signature" to signature,
            "X-Timestamp" to timestamp.toString(),
            "X-Nonce" to nonce,
            "User-Agent" to "Meydi-OS-Android-ClientSecure"
        )

        // Tampilkan audit log bahwa request telah ditandatangani secara kriptografis
        spamProtectionManager.logEvent(
            "Request Ditandatangani", 
            "INFO", 
            "Request ke $url ditandatangani (Signature: ${signature.take(12)}...)"
        )

        // 6. Sistem Auto Retry dengan Exponential Backoff
        var attempt = 0
        var lastException: Exception? = null
        var responseString = ""

        while (attempt < MAX_RETRIES) {
            try {
                attempt++
                if (attempt > 1) {
                    val backoffDelay = INITIAL_RETRY_DELAY_MS * (attempt - 1)
                    spamProtectionManager.logEvent(
                        "Auto Retry Jaringan", 
                        "WARNING", 
                        "Koneksi gagal. Melakukan percobaan ulang ke-$attempt setelah $backoffDelay milidetik..."
                    )
                    delay(backoffDelay)
                }

                // Eksekusi aksi jaringan sesungguhnya
                responseString = networkAction()
                
                // Jika sukses, keluar dari loop retry
                break
            } catch (e: Exception) {
                lastException = e
                spamProtectionManager.logEvent(
                    "Percobaan Request Gagal", 
                    "WARNING", 
                    "Percobaan ke-$attempt gagal: ${e.localizedMessage}"
                )
            }
        }

        // Lepas registrasi request aktif setelah selesai diproses
        if (method == "POST" || method == "PUT") {
            spamProtectionManager.unregisterActiveRequest(cachePayloadKey)
        }

        // Jika semua percobaan retry gagal, lemparkan error asli yang telah di-sanitize
        if (responseString.isEmpty() && lastException != null) {
            val sanitizedMsg = NetworkSecurity.sanitizeNetworkErrorMessage(lastException)
            spamProtectionManager.logEvent(
                "Koneksi Jaringan Gagal", 
                "CRITICAL", 
                "Seluruh percobaan retry gagal. Deskripsi kesalahan: ${lastException.localizedMessage}"
            )
            throw IOException(sanitizedMsg, lastException)
        }

        // 7. Simpan ke Cache Lokal jika metode GET sukses
        if (method == "GET" && responseString.isNotEmpty()) {
            spamProtectionManager.saveToCache(cachePayloadKey, responseString, 30000L) // Cache bertahan 30 detik
        }

        return@runBlocking responseString
    }
}
