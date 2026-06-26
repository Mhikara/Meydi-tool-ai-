package com.example.update.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import com.example.update.db.UpdateDao
import com.example.update.model.AppConfigEntity
import com.example.update.model.ChangelogEntity
import com.example.utils.AppUpdateInfo
import com.example.utils.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

class UpdateRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao: UpdateDao = db.updateDao()

    val allChangelogs: Flow<List<ChangelogEntity>> = dao.getAllChangelogsFlow()
    val allConfigs: Flow<List<AppConfigEntity>> = dao.getAllConfigsFlow()

    // Preferences for simulation and update caching
    private val prefs = context.getSharedPreferences("meydiai_update_manager", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_UPDATE_CHECK = "last_apk_update_check"
        private const val KEY_INTEGRITY_SALT = "MEYDI_INTEGRITY_SECURE_SALT_v1"
        private const val CACHE_COOLDOWN_MS = 15000L // 15 seconds cooldown to prevent spamming
    }

    // --- INTERNET UTILITY ---
    fun isNetworkAvailable(): Boolean {
        // Allow UI to simulate offline mode
        if (prefs.getBoolean("sim_offline_mode", false)) {
            return false
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // --- APK UPDATE ENGINE ---
    suspend fun checkForApkUpdate(forceRefresh: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersion = getLocalAppVersion()
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)

        // 1. Check debounce cooldown unless forced
        if (!forceRefresh && (now - lastCheck < CACHE_COOLDOWN_MS)) {
            AppLogger.info("UpdateRepository", "checkForApkUpdate", "Using cached check results (cooldown active).")
            return@withContext null
        }

        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, now).apply()

        // 2. Offline check
        if (!isNetworkAvailable()) {
            AppLogger.warn("UpdateRepository", "checkForApkUpdate", "No internet connection. Skipping remote APK update check.")
            throw Exception("Koneksi internet tidak tersedia atau simulasi offline aktif.")
        }

        // Simulate network latency
        delay(1000)

        // 3. Simulating remote fetch.
        // In real app, you would fetch Firestore document: `app_settings/update_config`
        val remoteVersion = prefs.getString("sim_remote_version", "1.2.0") ?: "1.2.0"
        val forceUpdate = prefs.getBoolean("sim_force_update", false)
        val changelog = prefs.getString("sim_remote_changelog", 
            "🚀 FITUR BARU:\n• AI Update Manager Module\n• Rollback & Integrity Engine\n• Incremental Cache Sync\n\n🐛 PERBAIKAN BUG:\n• Crash database saat offline\n• Sinkronisasi token Firebase Auth") 
            ?: ""
        val downloadSize = prefs.getString("sim_update_size", "14.8 MB") ?: "14.8 MB"
        val releaseDate = prefs.getString("sim_release_date", "25 Juni 2026") ?: "25 Juni 2026"
        val downloadUrl = "https://play.google.com/store/apps/details?id=com.example.meydiai"

        // Store into history if it's a new version
        val remoteChangelogEntity = ChangelogEntity(
            version = remoteVersion,
            releaseDate = now,
            featuresList = "OTA Update Manager, Real-time status sync, Incremental checks",
            bugFixesList = "Offline crash on DAO queries",
            perfGainsList = "Fast local cache loading (under 10ms)",
            securityList = "SHA-256 secure integrity validations",
            sizeBytes = 15000000L,
            isForceUpdate = forceUpdate,
            downloadUrl = downloadUrl
        )
        dao.insertChangelog(remoteChangelogEntity)

        // Version Comparison
        if (isVersionGreater(remoteVersion, currentVersion)) {
            return@withContext AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = remoteVersion,
                isForceUpdate = forceUpdate,
                changelog = "$changelog\n\n📦 Ukuran: $downloadSize\n📅 Tanggal: $releaseDate",
                downloadUrl = downloadUrl
            )
        }

        return@withContext null
    }

    // --- DATA OTA SYNC ENGINE (AUTO DATA UPDATE) ---
    suspend fun syncOtaConfig(): SyncResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            AppLogger.warn("UpdateRepository", "syncOtaConfig", "Offline. Fallback to cached local configurations.")
            return@withContext SyncResult.OfflineFallback(dao.getAllConfigs())
        }

        delay(1200) // Simulate downloading configs

        try {
            // Simulation parameters
            val simulateCorruption = prefs.getBoolean("sim_corruption_active", false)
            
            val remoteJsonString = if (simulateCorruption) {
                // Invalid JSON to trigger parse failure and Rollback
                "{ invalid_json_structure: missing_quotes "
            } else {
                // Modern incremental update simulation
                """
                {
                   "app_banner_title": "Super Promo Liburan Sekolah Meydi AI!",
                   "app_banner_image": "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
                   "category_data": "[\"AI Video\", \"Clipper\", \"Audio FX\", \"Upscale\", \"Updates\"]",
                   "maintenance_mode": "false",
                   "api_timeout_sec": "30",
                   "support_email": "support@meydiai.com"
                }
                """.trimIndent()
            }

            // --- 1. INTEGRITY VALIDATION ---
            val incomingHash = calculateSha256(remoteJsonString)
            
            // Validate JSON structure
            val parsedJson = JSONObject(remoteJsonString) // Will throw JSONException if corrupted

            // Validate essential keys exist
            if (!parsedJson.has("maintenance_mode") || !parsedJson.has("support_email")) {
                throw Exception("Data integrity check failed: Missing essential configuration fields.")
            }

            // --- 2. INCREMENTAL SYNC LOGIC ---
            // Fetch previous configs for rollback reference
            val oldConfigs = dao.getAllConfigs()
            
            val currentNow = System.currentTimeMillis()
            val newEntities = mutableListOf<AppConfigEntity>()
            
            val keys = parsedJson.keys()
            var incrementalCount = 0

            while (keys.hasNext()) {
                val key = keys.next()
                val value = parsedJson.getString(key)
                
                val existing = oldConfigs.find { it.configKey == key }
                if (existing == null || existing.configValue != value) {
                    // Only update if changed (incremental update)
                    newEntities.add(
                        AppConfigEntity(
                            configKey = key,
                            configValue = value,
                            updatedAt = currentNow,
                            versionHash = incomingHash,
                            category = "SYSTEM_CONFIG"
                        )
                    )
                    incrementalCount++
                }
            }

            if (newEntities.isNotEmpty()) {
                dao.insertConfigs(newEntities)
                AppLogger.info("UpdateRepository", "syncOtaConfig", "Successfully applied $incrementalCount incremental config updates.")
            } else {
                AppLogger.info("UpdateRepository", "syncOtaConfig", "No dynamic configuration changes on server. Local data is up-to-date.")
            }

            return@withContext SyncResult.Success(dao.getAllConfigs(), incrementalCount)

        } catch (e: Exception) {
            AppLogger.error("UpdateRepository", "syncOtaConfig", "OTA Update failed: ${e.message}. Performing ROLLBACK...")
            
            // --- 3. ROLLBACK SYSTEM ---
            // If anything goes wrong during download/parsing, we retain our existing cache or restore previous default config values.
            val recoveredConfigs = dao.getAllConfigs()
            if (recoveredConfigs.isEmpty()) {
                // If cache is empty, populate safe defaults to restore system stability
                val safeDefaults = listOf(
                    AppConfigEntity("app_banner_title", "Selamat Datang di Meydi AI Pro", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG"),
                    AppConfigEntity("app_banner_image", "", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG"),
                    AppConfigEntity("category_data", "[\"AI Video\", \"Clipper\", \"Upscale\"]", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG"),
                    AppConfigEntity("maintenance_mode", "false", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG"),
                    AppConfigEntity("api_timeout_sec", "15", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG"),
                    AppConfigEntity("support_email", "admin@meydiai.com", System.currentTimeMillis(), "SAFE_DEFAULT", "SYSTEM_CONFIG")
                )
                dao.insertConfigs(safeDefaults)
                AppLogger.warn("UpdateRepository", "syncOtaConfig", "Rollback populated secure default config values.")
                return@withContext SyncResult.RollbackTriggered(safeDefaults, "Koropsi terdeteksi: Reset ke Sesi Default Aman.")
            }

            return@withContext SyncResult.RollbackTriggered(recoveredConfigs, "Koropsi data server: Berhasil mengembalikan sesi cache yang valid (${e.localizedMessage}).")
        }
    }

    // --- CHANGELOG SEARCH & HISTORIC PERSISTENCE ---
    suspend fun getChangelogsHistory(query: String = ""): List<ChangelogEntity> = withContext(Dispatchers.IO) {
        // Pre-populate mock history if empty
        val existing = dao.getAllChangelogs()
        if (existing.isEmpty()) {
            val history = listOf(
                ChangelogEntity(
                    "1.1.0", System.currentTimeMillis() - 86400000 * 5,
                    "Cyber Admin Dashboard, Real-time user stats, Advanced security, Remote logging engine",
                    "Database null pointers on app termination",
                    "Room query optimizations using Indexed entities",
                    "Secure encryption middleware for owner permissions",
                    12400000L, false, ""
                ),
                ChangelogEntity(
                    "1.0.5", System.currentTimeMillis() - 86400000 * 15,
                    "Interactive AI Chat Companion, Real-time Network Monitor Screen, Media Downloader module",
                    "Media playing memory leakage on landscape orientation",
                    "Staggered layout loading under 120ms",
                    "OAuth2 consent verification",
                    11100000L, false, ""
                ),
                ChangelogEntity(
                    "1.0.0", System.currentTimeMillis() - 86400000 * 45,
                    "Initial release, Beautiful Retro Cyberpunk Material 3 theme, Basic auth setup",
                    "None (Initial release)",
                    "Edge-to-edge full fluid responsive layouts",
                    "Firebase security rules compliance",
                    9400000L, false, ""
                )
            )
            dao.insertChangelogs(history)
        }

        if (query.isBlank()) {
            dao.getAllChangelogs()
        } else {
            dao.searchChangelogs(query)
        }
    }

    // Helper functions
    private fun getLocalAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.1.0"
        } catch (e: Exception) {
            "1.1.0"
        }
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        val lParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(lParts.size, cParts.size)) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun calculateSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((input + KEY_INTEGRITY_SALT).toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

// Result representation
sealed class SyncResult {
    data class Success(val configs: List<AppConfigEntity>, val updatedCount: Int) : SyncResult()
    data class OfflineFallback(val cachedConfigs: List<AppConfigEntity>) : SyncResult()
    data class RollbackTriggered(val revertedConfigs: List<AppConfigEntity>, val alertMessage: String) : SyncResult()
}
