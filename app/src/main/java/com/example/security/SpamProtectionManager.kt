package com.example.security

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Model untuk melacak aktivitas request pengguna
data class RequestTracker(
    val timestamps: MutableList<Long> = mutableListOf(),
    var isBlocked: Boolean = false,
    var blockUntil: Long = 0L,
    var violationsCount: Int = 0
)

// Model untuk data cache request
data class CacheEntry(
    val data: String,
    val expiresAt: Long
)

// Representasi log audit keamanan sederhana
data class SecurityLog(
    val timestamp: Long,
    val event: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val details: String
)

class SpamProtectionManager {

    companion object {
        @Volatile
        private var instance: SpamProtectionManager? = null

        fun getInstance(): SpamProtectionManager {
            return instance ?: synchronized(this) {
                instance ?: SpamProtectionManager().also { instance = it }
            }
        }
    }

    // Konfigurasi dinamis (Bisa diperbarui via database / remote config)
    var maxRequestsPerWindow = 5          // Maksimal request dalam jendela waktu
    var windowSizeMs = 10000L             // Jendela waktu dalam milidetik (10 detik)
    var baseCooldownMs = 30000L           // Waktu cooldown dasar jika terdeteksi spam (30 detik)
    var maxViolationsBeforeBlock = 3      // Pelanggaran sebelum diblokir sementara
    var blockDurationMs = 900000L         // Durasi blokir sementara (15 menit)
    var duplicateWindowMs = 2000L         // Toleransi pengiriman data ganda (2 detik)

    // Penyimpanan data pelacakan aktivitas di memori secara thread-safe
    private val trackers = ConcurrentHashMap<String, RequestTracker>()
    private val requestCache = ConcurrentHashMap<String, CacheEntry>()
    private val activeRequests = ConcurrentHashMap<String, Long>() // Untuk pencegahan duplicate request ganda
    private val auditLogs = Collections.synchronizedList(mutableListOf<SecurityLog>())
    
    private val mutex = Mutex()

    init {
        logEvent("SpamProtectionManager diinisialisasi", "INFO", "Sistem anti-spam siap mengamankan aplikasi.")
    }

    /**
     * Memeriksa apakah request saat ini diperbolehkan (Rate Limiting + Spam Detection).
     * Mengembalikan objek Pair(Boolean, String) -> Pair(IsAllowed, Alasan/Pesan Cooldown)
     */
    suspend fun checkRequestPermission(userId: String, actionKey: String): Pair<Boolean, String> = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val key = "$userId:$actionKey"
        
        val tracker = trackers.getOrPut(key) { RequestTracker() }

        // 1. Cek apakah sedang diblokir sementara
        if (tracker.isBlocked) {
            if (currentTime < tracker.blockUntil) {
                val remainingSec = ((tracker.blockUntil - currentTime) / 1000).toInt()
                logEvent("Akses Diblokir Sementara", "CRITICAL", "User $userId diblokir saat mencoba aksi '$actionKey'. Sisa waktu: $remainingSec detik.")
                return Pair(false, "Akses Anda diblokir sementara karena terindikasi melakukan spam berat. Coba lagi dalam $remainingSec detik.")
            } else {
                // Blokir berakhir
                tracker.isBlocked = false
                tracker.violationsCount = 0
                logEvent("Blokir Sementara Berakhir", "INFO", "Blokir sementara untuk user $userId telah berakhir.")
            }
        }

        // 2. Bersihkan timestamp di luar jendela waktu (sliding window)
        tracker.timestamps.removeAll { currentTime - it > windowSizeMs }

        // 3. Deteksi Spam / Melampaui Batas
        if (tracker.timestamps.size >= maxRequestsPerWindow) {
            tracker.violationsCount++
            val multiplier = tracker.violationsCount
            val cooldownDuration = baseCooldownMs * multiplier
            tracker.blockUntil = currentTime + cooldownDuration
            
            if (tracker.violationsCount >= maxViolationsBeforeBlock) {
                tracker.isBlocked = true
                tracker.blockUntil = currentTime + blockDurationMs
                logEvent("User Diblokir", "CRITICAL", "User $userId diblokir selama ${blockDurationMs / 60000} menit setelah melanggar rate-limit sebanyak ${tracker.violationsCount} kali.")
                return Pair(false, "Sistem keamanan mendeteksi spam berulang! Akun Anda diblokir sementara selama 15 menit.")
            } else {
                logEvent("Batas Rate-Limit Terlampaui", "WARNING", "User $userId melampaui batas rate-limit aksi '$actionKey'. Diberikan cooldown $cooldownDuration ms.")
                return Pair(false, "Terlalu banyak permintaan! Keamanan anti-spam aktif. Silakan tunggu ${(cooldownDuration / 1000).toInt()} detik.")
            }
        }

        // Request diizinkan, catat timestamp baru
        tracker.timestamps.add(currentTime)
        return Pair(true, "Akses diizinkan.")
    }

    /**
     * Mencegah pengiriman request ganda yang sama dalam waktu singkat (Duplicate Request Prevention).
     */
    fun registerActiveRequest(payload: String): Boolean {
        val hash = generateHash(payload)
        val currentTime = System.currentTimeMillis()
        
        val lastTime = activeRequests[hash]
        if (lastTime != null && (currentTime - lastTime) < duplicateWindowMs) {
            logEvent("Pencegahan Request Ganda", "WARNING", "Mencegah pengiriman ulang data yang sama dalam waktu singkat. Hash: $hash")
            return false
        }
        
        activeRequests[hash] = currentTime
        return true
    }

    /**
     * Menandai request yang terdaftar sebagai selesai diproses
     */
    fun unregisterActiveRequest(payload: String) {
        val hash = generateHash(payload)
        activeRequests.remove(hash)
    }

    /**
     * Fungsi pembantu untuk menghasilkan hash payload sebagai penanda keunikan request
     */
    private fun generateHash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    /**
     * Menyimpan hasil respon data ke cache lokal sederhana
     */
    fun saveToCache(key: String, data: String, ttlMs: Long = 60000L) {
        val expiresAt = System.currentTimeMillis() + ttlMs
        requestCache[key] = CacheEntry(data, expiresAt)
    }

    /**
     * Mengambil data dari cache lokal jika belum kedaluwarsa
     */
    fun getFromCache(key: String): String? {
        val entry = requestCache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            requestCache.remove(key) // Hapus cache kedaluwarsa
            return null
        }
        return entry.data
    }

    /**
     * Mencatat aktivitas keamanan (Security Audit Logs)
     */
    fun logEvent(event: String, severity: String, details: String) {
        val timestamp = System.currentTimeMillis()
        val log = SecurityLog(timestamp, event, severity, details)
        auditLogs.add(0, log) // Tambahkan ke paling atas agar log terbaru di urutan pertama
        
        // Batasi log audit maksimal 100 entri
        if (auditLogs.size > 100) {
            auditLogs.removeAt(auditLogs.size - 1)
        }

        // Tampilkan ke logcat dengan mematuhi prinsip sanitasi keamanan (tidak membocorkan rahasia)
        when (severity) {
            "INFO" -> Log.i("MeydiAISecurity", "[$severity] $event: $details")
            "WARNING" -> Log.w("MeydiAISecurity", "[$severity] $event: $details")
            "CRITICAL" -> Log.e("MeydiAISecurity", "[$severity] $event: $details")
        }
    }

    /**
     * Mengambil daftar log keamanan
     */
    fun getAuditLogs(): List<SecurityLog> {
        return auditLogs.toList()
    }

    /**
     * Menghapus seluruh data rate-limiting dan log (misalnya saat logout)
     */
    fun reset() {
        trackers.clear()
        requestCache.clear()
        activeRequests.clear()
        auditLogs.clear()
        logEvent("SpamProtectionManager di-reset", "INFO", "Seluruh data anti-spam dan log keamanan dibersihkan.")
    }
}
