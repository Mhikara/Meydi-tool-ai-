package com.example.centralapi.repository

import com.example.centralapi.database.CentralCacheDao
import com.example.centralapi.domain.*
import com.example.centralapi.network.ApiManager
import com.example.centralapi.security.CentralSecurityManager
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File
import java.io.InputStream

// --- BASE REPOSITORY UTILITY ---
abstract class BaseRepository(
    protected val apiManager: ApiManager,
    protected val cacheDao: CentralCacheDao,
    protected val securityManager: CentralSecurityManager
) {
    protected val moshi = apiManager.moshi

    /**
     * Executes an API request with dynamic role-based verification and local Room cache fallback.
     */
    protected fun <T> networkBoundResource(
        requiredRole: UserRole,
        cacheKey: String?,
        clazz: Class<T>,
        apiCall: suspend () -> retrofit2.Response<T>
    ): Flow<Result<T>> = flow {
        // 1. Check access control locally
        if (!apiManager.validateAccess(requiredRole)) {
            emit(Result.failure(Exception("ACCESS DENIED: Required role $requiredRole")))
            return@flow
        }

        // 2. Emit cached data first if requested
        if (cacheKey != null) {
            val cached = cacheDao.getResponse(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(clazz)
                    val cachedData = adapter.fromJson(cached.jsonResponse)
                    if (cachedData != null) {
                        emit(Result.success(cachedData))
                    }
                } catch (e: Exception) {
                    // Ignore parsing error, proceed to network
                }
            }
        }

        // 3. Perform network call
        try {
            val response = apiCall()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Save to local cache
                if (cacheKey != null) {
                    val adapter = moshi.adapter(clazz)
                    cacheDao.saveResponse(
                        CachedResponse(
                            cacheKey = cacheKey,
                            jsonResponse = adapter.toJson(data),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                emit(Result.success(data))
            } else {
                emit(Result.failure(Exception("API Error ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Offline or connection failed. Fallback loaded if present.", e)))
        }
    }
}

// --- 1. LOGIN REPOSITORY ---
class LoginRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun login(email: String, password: String): Flow<Result<UserProfile>> = flow {
        try {
            val response = apiManager.getService().login(mapOf("email" to email, "password" to password))
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                securityManager.saveUserToken(profile.token)
                securityManager.saveUserRole(profile.role)
                securityManager.saveUserEmail(profile.email)
                emit(Result.success(profile))
            } else {
                emit(Result.failure(Exception("Login Failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 2. REGISTER REPOSITORY ---
class RegisterRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun register(name: String, email: String, password: String): Flow<Result<UserProfile>> = flow {
        try {
            val response = apiManager.getService().register(
                mapOf("fullName" to name, "email" to email, "password" to password)
            )
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                securityManager.saveUserToken(profile.token)
                securityManager.saveUserRole(profile.role)
                securityManager.saveUserEmail(profile.email)
                emit(Result.success(profile))
            } else {
                emit(Result.failure(Exception("Registration Failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 3. GUEST REPOSITORY ---
class GuestRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun signInAsGuest(): Flow<Result<UserProfile>> = flow {
        try {
            val response = apiManager.getService().loginAsGuest()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                securityManager.saveUserToken(profile.token)
                securityManager.saveUserRole(UserRole.USER)
                securityManager.saveUserEmail("guest@meydi.ai")
                emit(Result.success(profile))
            } else {
                // Return static fallback guest if server not available
                val guestProfile = UserProfile(
                    uid = "guest_offline",
                    fullName = "Guest Offline",
                    email = "guest@meydi.ai",
                    role = UserRole.USER,
                    token = "offline_guest_token"
                )
                securityManager.saveUserToken(guestProfile.token)
                securityManager.saveUserRole(guestProfile.role)
                securityManager.saveUserEmail(guestProfile.email)
                emit(Result.success(guestProfile))
            }
        } catch (e: Exception) {
            val guestProfile = UserProfile(
                uid = "guest_offline",
                fullName = "Guest Offline",
                email = "guest@meydi.ai",
                role = UserRole.USER,
                token = "offline_guest_token"
            )
            securityManager.saveUserToken(guestProfile.token)
            securityManager.saveUserRole(guestProfile.role)
            securityManager.saveUserEmail(guestProfile.email)
            emit(Result.success(guestProfile))
        }
    }
}

// --- 4. PROFILE REPOSITORY ---
class ProfileRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getProfile(): Flow<Result<UserProfile>> = networkBoundResource(
        requiredRole = UserRole.USER,
        cacheKey = "user_profile_cache",
        clazz = UserProfile::class.java
    ) {
        apiManager.getService().getProfile()
    }
}

// --- 5. DASHBOARD REPOSITORY ---
class DashboardRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getDashboardStats(): Flow<Result<DashboardStats>> = networkBoundResource(
        requiredRole = UserRole.USER,
        cacheKey = "dashboard_stats_cache",
        clazz = DashboardStats::class.java
    ) {
        apiManager.getService().getDashboardStats()
    }
}

// --- 6. AUTO UPDATE REPOSITORY ---
class UpdateRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun checkUpdate(currentVersion: String): Flow<Result<UpdateInfo>> = flow {
        try {
            val response = apiManager.getService().checkUpdate(currentVersion)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Check Update failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 7. DOWNLOADER REPOSITORY ---
class DownloaderRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getTasks(): Flow<Result<List<DownloadTask>>> = flow {
        try {
            val response = apiManager.getService().getDownloadTasks()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Failed downloading tasks")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 8. AI ASSISTANT REPOSITORY ---
class AiAssistantRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun askAi(prompt: String): Flow<Result<AiResponse>> = flow {
        if (!apiManager.validateAccess(UserRole.USER)) {
            emit(Result.failure(Exception("Access Denied")))
            return@flow
        }
        try {
            val response = apiManager.getService().chatWithAi(AiPromptRequest(prompt))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("AI Response Failed")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 9. PREMIUM REPOSITORY ---
class PremiumRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun activatePremium(activationCode: String): Flow<Result<Boolean>> = flow {
        try {
            val response = apiManager.getService().activatePremium(mapOf("code" to activationCode))
            if (response.isSuccessful) {
                securityManager.saveUserRole(UserRole.PREMIUM)
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("Invalid premium activation code")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 10. CLOUD SYNC REPOSITORY ---
class SyncRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    suspend fun syncOfflineData(): Result<Boolean> {
        val unsynced = cacheDao.getUnsyncedLogs()
        if (unsynced.isEmpty()) return Result.success(true)

        return try {
            val response = apiManager.getService().syncCloud(unsynced)
            if (response.isSuccessful) {
                cacheDao.markLogsSynced(unsynced.map { it.id })
                Result.success(true)
            } else {
                Result.failure(Exception("Server sync returned failure code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// --- 11. NOTIFICATION REPOSITORY ---
class NotificationRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getNotifications(): Flow<Result<List<Map<String, String>>>> = flow {
        // Emit cache
        val cached = cacheDao.getResponse("notifications_cache")
        if (cached != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
                val cachedData = moshi.adapter<List<Map<String, String>>>(type).fromJson(cached.jsonResponse)
                if (cachedData != null) emit(Result.success(cachedData))
            } catch (e: Exception) {}
        }

        try {
            val response = apiManager.getService().getNotifications()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val type = Types.newParameterizedType(List::class.java, Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
                cacheDao.saveResponse(
                    CachedResponse("notifications_cache", moshi.adapter<List<Map<String, String>>>(type).toJson(data), System.currentTimeMillis())
                )
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 12. SETTINGS REPOSITORY ---
class SettingsRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getSettings(): Flow<Result<Map<String, String>>> = flow {
        val cached = cacheDao.getResponse("settings_cache")
        if (cached != null) {
            try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val cachedData = moshi.adapter<Map<String, String>>(type).fromJson(cached.jsonResponse)
                if (cachedData != null) emit(Result.success(cachedData))
            } catch (e: Exception) {}
        }
        try {
            val response = apiManager.getService().getSettings()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                cacheDao.saveResponse(
                    CachedResponse("settings_cache", moshi.adapter<Map<String, String>>(type).toJson(data), System.currentTimeMillis())
                )
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 13. STATS REPOSITORY ---
class StatsRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getStats(): Flow<Result<Map<String, Any>>> = flow {
        val cached = cacheDao.getResponse("stats_cache")
        if (cached != null) {
            try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val cachedData = moshi.adapter<Map<String, Any>>(type).fromJson(cached.jsonResponse)
                if (cachedData != null) emit(Result.success(cachedData))
            } catch (e: Exception) {}
        }
        try {
            val response = apiManager.getService().getStats()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                cacheDao.saveResponse(
                    CachedResponse("stats_cache", moshi.adapter<Map<String, Any>>(type).toJson(data), System.currentTimeMillis())
                )
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 14. ACTIVITY REPOSITORY ---
class ActivityRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getLogs(): Flow<Result<List<ActivityLog>>> = flow {
        // Emit cached log first
        emit(Result.success(cacheDao.getActivityLogs()))
        try {
            val response = apiManager.getService().getActivityLogs()
            if (response.isSuccessful && response.body() != null) {
                val networkLogs = response.body()!!
                networkLogs.forEach { log -> cacheDao.logActivity(log.copy(synced = true)) }
                emit(Result.success(cacheDao.getActivityLogs()))
            }
        } catch (e: Exception) {
            // gracefully fallback to local cache
        }
    }

    suspend fun addLog(action: String) {
        val newLog = ActivityLog(
            id = java.util.UUID.randomUUID().toString(),
            action = action,
            timestamp = System.currentTimeMillis(),
            synced = false
        )
        cacheDao.logActivity(newLog)
    }
}

// --- 15. FILE REPOSITORY ---
class FileRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun uploadFile(file: File): Flow<Result<Map<String, String>>> = flow {
        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = apiManager.getService().uploadFile(body)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Upload failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun downloadFile(fileId: String): Flow<Result<InputStream>> = flow {
        try {
            val response = apiManager.getService().downloadFile(fileId)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!.byteStream()))
            } else {
                emit(Result.failure(Exception("Download failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 16. SEARCH REPOSITORY ---
class SearchRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun search(query: String): Flow<Result<List<Map<String, Any>>>> = flow {
        try {
            val response = apiManager.getService().search(query)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Search Error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 17. FEEDBACK REPOSITORY ---
class FeedbackRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun submitFeedback(rating: Int, comment: String): Flow<Result<Boolean>> = flow {
        val feedback = UserFeedback(
            id = java.util.UUID.randomUUID().toString(),
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )
        try {
            val response = apiManager.getService().submitFeedback(feedback)
            if (response.isSuccessful) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("Feedback submit failed on server")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 18. OWNER REPOSITORY (Access control restriction: OWNER) ---
class OwnerRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getOwnerPanel(): Flow<Result<Map<String, Any>>> = flow {
        if (!apiManager.validateAccess(UserRole.OWNER)) {
            emit(Result.failure(Exception("ACCESS DENIED: Required role OWNER")))
            return@flow
        }
        val cached = cacheDao.getResponse("owner_panel_cache")
        if (cached != null) {
            try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val cachedData = moshi.adapter<Map<String, Any>>(type).fromJson(cached.jsonResponse)
                if (cachedData != null) emit(Result.success(cachedData))
            } catch (e: Exception) {}
        }
        try {
            val response = apiManager.getService().getOwnerPanelData()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                cacheDao.saveResponse(
                    CachedResponse("owner_panel_cache", moshi.adapter<Map<String, Any>>(type).toJson(data), System.currentTimeMillis())
                )
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

// --- 19. ADMIN REPOSITORY (Access control restriction: ADMIN) ---
class AdminRepository(
    apiManager: ApiManager,
    cacheDao: CentralCacheDao,
    securityManager: CentralSecurityManager
) : BaseRepository(apiManager, cacheDao, securityManager) {
    fun getAdminDashboard(): Flow<Result<Map<String, Any>>> = flow {
        if (!apiManager.validateAccess(UserRole.ADMIN)) {
            emit(Result.failure(Exception("ACCESS DENIED: Required role ADMIN")))
            return@flow
        }
        val cached = cacheDao.getResponse("admin_dashboard_cache")
        if (cached != null) {
            try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val cachedData = moshi.adapter<Map<String, Any>>(type).fromJson(cached.jsonResponse)
                if (cachedData != null) emit(Result.success(cachedData))
            } catch (e: Exception) {}
        }
        try {
            val response = apiManager.getService().getAdminDashboard()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                cacheDao.saveResponse(
                    CachedResponse("admin_dashboard_cache", moshi.adapter<Map<String, Any>>(type).toJson(data), System.currentTimeMillis())
                )
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
