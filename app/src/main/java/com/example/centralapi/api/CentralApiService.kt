package com.example.centralapi.api

import com.example.centralapi.domain.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface CentralApiService {

    // --- AUTHENTICATION ---
    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<UserProfile>

    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<UserProfile>

    @POST("api/auth/guest")
    suspend fun loginAsGuest(): Response<UserProfile>

    // --- USER PROFILE ---
    @GET("api/user/profile")
    suspend fun getProfile(): Response<UserProfile>

    @PUT("api/user/profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<UserProfile>

    // --- DASHBOARD ---
    @GET("api/dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStats>

    // --- AUTO UPDATE ---
    @GET("api/system/update")
    suspend fun checkUpdate(@Query("currentVersion") version: String): Response<UpdateInfo>

    // --- DOWNLOADER ---
    @GET("api/downloader/tasks")
    suspend fun getDownloadTasks(): Response<List<DownloadTask>>

    // --- AI ASSISTANT ---
    @POST("api/ai/chat")
    suspend fun chatWithAi(@Body request: AiPromptRequest): Response<AiResponse>

    // --- PREMIUM ---
    @POST("api/premium/activate")
    suspend fun activatePremium(@Body body: Map<String, String>): Response<Map<String, Any>>

    // --- SYNC SERVICES ---
    @POST("api/sync/cloud")
    suspend fun syncCloud(@Body localData: List<ActivityLog>): Response<Map<String, Any>>

    @POST("api/sync/db-firestore")
    suspend fun syncRoomToFirestore(@Body payload: Map<String, String>): Response<Map<String, Any>>

    // --- NOTIFICATIONS ---
    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<Map<String, String>>>

    // --- SETTINGS ---
    @GET("api/settings")
    suspend fun getSettings(): Response<Map<String, String>>

    @POST("api/settings")
    suspend fun updateSettings(@Body settings: Map<String, String>): Response<Map<String, String>>

    // --- STATS & LOGS ---
    @GET("api/stats")
    suspend fun getStats(): Response<Map<String, Any>>

    @GET("api/activity/logs")
    suspend fun getActivityLogs(): Response<List<ActivityLog>>

    @POST("api/activity/logs")
    suspend fun uploadActivityLogs(@Body logs: List<ActivityLog>): Response<Map<String, Any>>

    // --- FILES ---
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<Map<String, String>>

    @Streaming
    @GET("api/files/download")
    suspend fun downloadFile(@Query("fileId") fileId: String): Response<ResponseBody>

    // --- SEARCH ---
    @GET("api/search")
    suspend fun search(@Query("query") query: String): Response<List<Map<String, Any>>>

    // --- FEEDBACK ---
    @POST("api/feedback")
    suspend fun submitFeedback(@Body feedback: UserFeedback): Response<Map<String, Any>>

    // --- OWNER & ADMIN MANAGEMENT ---
    @GET("api/owner/admin-panel")
    suspend fun getOwnerPanelData(): Response<Map<String, Any>>

    @GET("api/admin/dashboard")
    suspend fun getAdminDashboard(): Response<Map<String, Any>>
}
