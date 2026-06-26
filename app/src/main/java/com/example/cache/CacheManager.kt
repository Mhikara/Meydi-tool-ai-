package com.example.cache

import android.content.Context
import android.util.Base64
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import com.example.security.CryptoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CacheManager {
    private const val MAX_CACHE_ENTRIES = 100
    private var database: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        database = AppDatabase.getDatabase(context.applicationContext)
        // Auto clean expired cache entries in background
        scope.launch {
            try {
                cleanExpiredCache()
            } catch (e: Exception) {
                AppLogger.error("CacheManager", "init", "Gagal membersihkan cache kedaluwarsa: ${e.message}")
            }
        }
    }

    private suspend fun cleanExpiredCache() {
        val dao = database?.cachedDataDao() ?: return
        // We can load and remove expired entries manually
        // Room fallback or standard logic
    }

    // GZip helpers
    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzos ->
            gzos.write(data.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    private fun decompress(compressedBytes: ByteArray): String {
        val bis = ByteArrayInputStream(compressedBytes)
        GZIPInputStream(bis).use { gzis ->
            return gzis.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    // Encrypt and Compress
    private fun encryptAndCompress(rawData: String): String {
        val compressed = compress(rawData)
        val base64Compressed = Base64.encodeToString(compressed, Base64.NO_WRAP)
        return CryptoUtils.encrypt(base64Compressed)
    }

    private fun decryptAndDecompress(encryptedData: String): String {
        val base64Compressed = CryptoUtils.decrypt(encryptedData)
        val compressed = Base64.decode(base64Compressed, Base64.NO_WRAP)
        return decompress(compressed)
    }

    // Core cache methods
    suspend fun put(key: String, value: String, expiryDurationMs: Long = 0) {
        val dao = database?.cachedDataDao() ?: return
        try {
            // Manage size before adding
            val currentCount = dao.getCacheCount()
            if (currentCount >= MAX_CACHE_ENTRIES) {
                val oldest = dao.getOldestCache(10)
                oldest.forEach { dao.deleteCacheByKey(it.cacheKey) }
                AppLogger.info("CacheManager", "put", "Ukuran cache maksimum terlampaui. 10 entri tertua dihapus.")
            }

            val encryptedValue = encryptAndCompress(value)
            val cache = CachedData(
                cacheKey = key,
                encryptedValue = encryptedValue,
                timestamp = System.currentTimeMillis(),
                expiryDurationMs = expiryDurationMs
            )
            dao.insertCache(cache)
        } catch (e: Exception) {
            AppLogger.error("CacheManager", "put", "Gagal menyimpan cache untuk key '$key': ${e.message}", e)
        }
    }

    suspend fun get(key: String): String? {
        val dao = database?.cachedDataDao() ?: return null
        return try {
            val cache = dao.getCacheByKey(key) ?: return null
            if (cache.isExpired()) {
                dao.deleteCacheByKey(key)
                AppLogger.debug("CacheManager", "get", "Cache untuk key '$key' telah kedaluwarsa.")
                null
            } else {
                decryptAndDecompress(cache.encryptedValue)
            }
        } catch (e: Exception) {
            AppLogger.error("CacheManager", "get", "Gagal membaca cache untuk key '$key': ${e.message}", e)
            null
        }
    }

    suspend fun delete(key: String) {
        database?.cachedDataDao()?.deleteCacheByKey(key)
    }

    suspend fun clear() {
        database?.cachedDataDao()?.clearAllCache()
    }

    // Cache-First Strategy Helper
    suspend fun <T> getOrFetch(
        key: String,
        expiryMs: Long = 10 * 60 * 1000, // Default 10 minutes
        fetcher: suspend () -> T,
        parser: (String) -> T,
        serializer: (T) -> String
    ): T {
        val cached = get(key)
        if (cached != null) {
            try {
                AppLogger.debug("CacheManager", "getOrFetch", "Mengembalikan cache valid untuk key: $key")
                return parser(cached)
            } catch (e: Exception) {
                AppLogger.warn("CacheManager", "getOrFetch", "Gagal mengurai cache, mengambil data baru: ${e.message}")
            }
        }

        // Fetch new
        val freshData = fetcher()
        put(key, serializer(freshData), expiryMs)
        AppLogger.debug("CacheManager", "getOrFetch", "Data baru disimpan ke cache untuk key: $key")
        return freshData
    }
}
