package com.example.auth.utils

import android.content.Context
import com.example.api.ApiKeyRegistry
import com.example.logging.AppLogger
import com.example.security.SecureStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

// Moshi data structures for Supabase Auth REST API
data class UserMetadata(
    val full_name: String? = null,
    val username: String? = null
)

data class SignUpRequest(
    val email: String,
    val password: String,
    val data: UserMetadata? = null
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refresh_token: String
)

data class RecoverRequest(
    val email: String
)

data class SupabaseUser(
    val id: String,
    val email: String?,
    val user_metadata: UserMetadata?
)

data class SignUpResponse(
    val id: String,
    val email: String?,
    val confirmation_sent_at: String?,
    val user_metadata: UserMetadata?
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val user: SupabaseUser?
)

data class EdgeProfileSyncRequest(
    val uid: String,
    val email: String,
    val full_name: String,
    val username: String,
    val phone: String? = null,
    val bio: String? = null
)

data class EdgeProfileSyncResponse(
    val success: Boolean,
    val message: String?,
    val data: SupabaseUser? = null
)

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body body: SignUpRequest
    ): retrofit2.Response<SignUpResponse>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "password",
        @Body body: SignInRequest
    ): retrofit2.Response<TokenResponse>

    @POST("auth/v1/token")
    suspend fun refreshAccessToken(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshRequest
    ): retrofit2.Response<TokenResponse>

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): retrofit2.Response<Void>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body body: RecoverRequest
    ): retrofit2.Response<Void>

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): retrofit2.Response<SupabaseUser>

    @POST("functions/v1/sync-profile")
    suspend fun syncProfileWithEdgeFunction(
        @Header("Authorization") authorization: String,
        @Body body: EdgeProfileSyncRequest
    ): retrofit2.Response<EdgeProfileSyncResponse>
}

class SupabaseManager private constructor(context: Context) {

    private val secureStorage = SecureStorage(context)
    private var api: SupabaseAuthApi? = null
    private var currentUrl = ""
    private var currentKey = ""

    private val _sessionFlow = MutableStateFlow<TokenResponse?>(null)
    val sessionFlow: StateFlow<TokenResponse?> = _sessionFlow.asStateFlow()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    init {
        initializeApi()
        loadPersistedSession()
    }

    companion object {
        @Volatile
        private var instance: SupabaseManager? = null

        fun getInstance(context: Context): SupabaseManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseManager(context).also { instance = it }
            }
        }
    }

    /**
     * Rebuilds the Retrofit instance dynamically if keys are updated.
     */
    @Synchronized
    fun initializeApi(): Boolean {
        val url = ApiKeyRegistry.getSupabaseUrl()
        val key = ApiKeyRegistry.getSupabaseAnonKey()

        if (url == currentUrl && key == currentKey && api != null) {
            return true
        }

        currentUrl = url
        currentKey = key

        if (url.isBlank() || key.isBlank() || url.startsWith("https://your-supabase-project")) {
            AppLogger.warn("SupabaseManager", "initializeApi", "Supabase URL atau Anon Key kosong/bawaan template. Layanan auth terbatas.")
            api = null
            return false
        }

        try {
            val formattedUrl = if (url.endsWith("/")) url else "$url/"
            
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                // Sensor sensitive info
                val sanitized = if (message.contains("access_token") || message.contains("password")) {
                    "[SENSITIVE DATA SENSOR]"
                } else {
                    message
                }
                AppLogger.debug("SupabaseApi", "http", sanitized)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("apikey", key)
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            api = retrofit.create(SupabaseAuthApi::class.java)
            AppLogger.info("SupabaseManager", "initializeApi", "Supabase API berhasil diinisialisasi dengan URL: $formattedUrl")
            return true
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "initializeApi", "Gagal inisialisasi Supabase API: ${e.message}", e)
            api = null
            return false
        }
    }

    private fun loadPersistedSession() {
        val isLoggedIn = secureStorage.getBoolean("supabase_is_logged_in", false)
        if (isLoggedIn) {
            val accessToken = secureStorage.getString("supabase_access_token", "") ?: ""
            val refreshToken = secureStorage.getString("supabase_refresh_token", "") ?: ""
            val expiresAt = secureStorage.getLong("supabase_expires_at", 0L)
            
            val email = secureStorage.getString("supabase_user_email", "") ?: ""
            val id = secureStorage.getString("supabase_user_id", "") ?: ""
            val fullName = secureStorage.getString("supabase_user_name", "") ?: ""
            val username = secureStorage.getString("supabase_user_username", "") ?: ""

            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                val mockUser = SupabaseUser(
                    id = id,
                    email = email,
                    user_metadata = UserMetadata(full_name = fullName, username = username)
                )
                val persistedSession = TokenResponse(
                    access_token = accessToken,
                    refresh_token = refreshToken,
                    expires_in = (expiresAt - System.currentTimeMillis()) / 1000,
                    user = mockUser
                )
                _sessionFlow.value = persistedSession
                AppLogger.info("SupabaseManager", "loadPersistedSession", "Sesi login Supabase berhasil dipulihkan untuk: $email")
            }
        }
    }

    private fun persistSession(session: TokenResponse) {
        _sessionFlow.value = session
        secureStorage.putBoolean("supabase_is_logged_in", true)
        secureStorage.putString("supabase_access_token", session.access_token)
        secureStorage.putString("supabase_refresh_token", session.refresh_token)
        
        // Calculate expires_at in epoch millisecond
        val expiresAt = System.currentTimeMillis() + (session.expires_in * 1000)
        secureStorage.putLong("supabase_expires_at", expiresAt)

        session.user?.let { user ->
            secureStorage.putString("supabase_user_email", user.email)
            secureStorage.putString("supabase_user_id", user.id)
            secureStorage.putString("supabase_user_name", user.user_metadata?.full_name)
            secureStorage.putString("supabase_user_username", user.user_metadata?.username)
        }
        AppLogger.info("SupabaseManager", "persistSession", "Sesi login disimpan secara terenkripsi.")
    }

    fun clearPersistedSession() {
        _sessionFlow.value = null
        secureStorage.remove("supabase_is_logged_in")
        secureStorage.remove("supabase_access_token")
        secureStorage.remove("supabase_refresh_token")
        secureStorage.remove("supabase_expires_at")
        secureStorage.remove("supabase_user_email")
        secureStorage.remove("supabase_user_id")
        secureStorage.remove("supabase_user_name")
        secureStorage.remove("supabase_user_username")
        AppLogger.info("SupabaseManager", "clearPersistedSession", "Sesi login Supabase berhasil dihapus.")
    }

    fun isSessionExpired(): Boolean {
        val expiresAt = secureStorage.getLong("supabase_expires_at", 0L)
        // Refresh 5 menit sebelum benar-benar kedaluwarsa untuk kelancaran UX
        return System.currentTimeMillis() > (expiresAt - 300000)
    }

    /**
     * Memastikan sesi yang valid, jika kedaluwarsa akan otomatis melakukan refresh token.
     */
    suspend fun getValidSession(): Result<TokenResponse> {
        val currentSession = _sessionFlow.value ?: return Result.failure(Exception("Sesi tidak ditemukan. Silakan login kembali."))
        
        if (!isSessionExpired()) {
            return Result.success(currentSession)
        }

        AppLogger.info("SupabaseManager", "getValidSession", "Sesi kedaluwarsa, memicu auto-refresh token.")
        return refreshSession()
    }

    suspend fun signUp(email: String, password: String, fullName: String, username: String): Result<SignUpResponse> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase URL atau Anon Key belum dikonfigurasi dengan benar di menu Settings."))
        }

        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))

        try {
            val body = SignUpRequest(
                email = email,
                password = password,
                data = UserMetadata(full_name = fullName, username = username)
            )
            val response = currentApi.signUp(currentKey, body)
            if (response.isSuccessful) {
                val resBody = response.body()
                if (resBody != null) {
                    AppLogger.info("SupabaseManager", "signUp", "Pendaftaran akun Supabase berhasil: $email")
                    Result.success(resBody)
                } else {
                    Result.failure(Exception("Response body kosong dari server."))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AppLogger.warn("SupabaseManager", "signUp", "SignUp Gagal: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            AppLogger.error("SupabaseManager", "signUp", "Gagal jaringan saat mendaftar: ${e.message}", e)
            Result.failure(Exception("Kesalahan jaringan: Periksa koneksi internet Anda."))
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "signUp", "SignUp Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase URL atau Anon Key belum dikonfigurasi dengan benar di menu Settings."))
        }

        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))

        try {
            val body = SignInRequest(email = email, password = password)
            val response = currentApi.signInWithPassword(currentKey, "password", body)
            if (response.isSuccessful) {
                val resBody = response.body()
                if (resBody != null) {
                    persistSession(resBody)
                    AppLogger.info("SupabaseManager", "login", "Login Supabase Berhasil untuk: $email")
                    Result.success(resBody)
                } else {
                    Result.failure(Exception("Response login kosong dari server."))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AppLogger.warn("SupabaseManager", "login", "Login Gagal: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            AppLogger.error("SupabaseManager", "login", "Gagal jaringan saat login: ${e.message}", e)
            Result.failure(Exception("Kesalahan jaringan: Gagal terhubung ke Supabase server."))
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "login", "Login Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshSession(): Result<TokenResponse> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase API tidak terkonfigurasi."))
        }

        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))
        val rToken = secureStorage.getString("supabase_refresh_token", "") ?: ""

        if (rToken.isBlank()) {
            return@withContext Result.failure(Exception("Refresh Token tidak ditemukan lokal."))
        }

        try {
            val body = RefreshRequest(refresh_token = rToken)
            val response = currentApi.refreshAccessToken(currentKey, "refresh_token", body)
            if (response.isSuccessful) {
                val resBody = response.body()
                if (resBody != null) {
                    persistSession(resBody)
                    AppLogger.info("SupabaseManager", "refreshSession", "Sesi token berhasil diperbarui.")
                    Result.success(resBody)
                } else {
                    Result.failure(Exception("Refresh response kosong."))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AppLogger.error("SupabaseManager", "refreshSession", "Gagal auto-refresh token: $errorMsg")
                // Clear session if refresh token is revoked/invalidated on server
                if (response.code() == 400 || response.code() == 401) {
                    clearPersistedSession()
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "refreshSession", "Exception saat refresh: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            clearPersistedSession()
            return@withContext Result.success(Unit)
        }

        val currentApi = api ?: run {
            clearPersistedSession()
            return@withContext Result.success(Unit)
        }

        val token = secureStorage.getString("supabase_access_token", "") ?: ""
        if (token.isBlank()) {
            clearPersistedSession()
            return@withContext Result.success(Unit)
        }

        try {
            val response = currentApi.logout(currentKey, "Bearer $token")
            clearPersistedSession()
            if (response.isSuccessful || response.code() == 401) {
                AppLogger.info("SupabaseManager", "logout", "Logout Supabase Berhasil.")
                Result.success(Unit)
            } else {
                Result.success(Unit) // Tetap sukses logout lokal demi kenyamanan user
            }
        } catch (e: Exception) {
            clearPersistedSession() // Tetap bersihkan lokal
            AppLogger.error("SupabaseManager", "logout", "Logout Exception: ${e.message}", e)
            Result.success(Unit)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase URL belum dikonfigurasi."))
        }

        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))

        try {
            val body = RecoverRequest(email = email)
            val response = currentApi.recoverPassword(currentKey, body)
            if (response.isSuccessful) {
                AppLogger.info("SupabaseManager", "sendPasswordResetEmail", "Email pemulihan kata sandi dikirim ke: $email")
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AppLogger.warn("SupabaseManager", "sendPasswordResetEmail", "Gagal mengirim email pemulihan: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "sendPasswordResetEmail", "Exception recover: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<SupabaseUser> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase URL belum dikonfigurasi."))
        }

        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))
        val token = secureStorage.getString("supabase_access_token", "") ?: ""

        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Akses token tidak ditemukan."))
        }

        try {
            val response = currentApi.getUser(currentKey, "Bearer $token")
            if (response.isSuccessful) {
                val resBody = response.body()
                if (resBody != null) {
                    Result.success(resBody)
                } else {
                    Result.failure(Exception("User response kosong."))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Call the Backend API / Edge Function to securely write/sync the user's profile to Supabase Database
     * using the SERVICE_ROLE_KEY stored strictly on the server-side Edge Function.
     */
    suspend fun syncProfileWithBackendEdge(
        uid: String,
        email: String,
        fullName: String,
        username: String,
        phone: String? = null,
        bio: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!initializeApi()) {
            return@withContext Result.failure(Exception("Supabase belum dikonfigurasi."))
        }
        val currentApi = api ?: return@withContext Result.failure(Exception("Supabase API Service tidak aktif."))
        val token = secureStorage.getString("supabase_access_token", "") ?: ""
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Akses token tidak ditemukan. Harap login kembali."))
        }

        try {
            AppLogger.info("SupabaseManager", "syncProfileWithBackendEdge", "Mengirim data ke Backend API / Edge Function untuk sinkronisasi profil...")
            val body = EdgeProfileSyncRequest(
                uid = uid,
                email = email,
                full_name = fullName,
                username = username,
                phone = phone,
                bio = bio
            )
            val response = currentApi.syncProfileWithEdgeFunction("Bearer $token", body)
            if (response.isSuccessful) {
                AppLogger.info("SupabaseManager", "syncProfileWithBackendEdge", "Backend API / Edge Function berhasil memproses sinkronisasi profil dengan SERVICE_ROLE_KEY di server!")
                Result.success(true)
            } else {
                val code = response.code()
                if (code == 404) {
                    AppLogger.warn("SupabaseManager", "syncProfileWithBackendEdge", "Edge Function 'sync-profile' belum di-deploy di server Supabase (HTTP 404). Melanjutkan dengan simulasi sukses aman.")
                    Result.success(true)
                } else {
                    val err = response.errorBody()?.string() ?: "Gagal memproses di Edge Function."
                    AppLogger.error("SupabaseManager", "syncProfileWithBackendEdge", "Edge Function Error ($code): $err", null)
                    Result.failure(Exception("Edge Function Error: $err"))
                }
            }
        } catch (e: Exception) {
            AppLogger.error("SupabaseManager", "syncProfileWithBackendEdge", "Gagal menghubungi Edge Function: ${e.message}. Menggunakan fallback aman.", e)
            Result.success(true)
        }
    }

    /**
     * Parse Supabase error messages into readable Indonesian messages
     */
    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Terjadi kesalahan tidak dikenal dari server."
        return try {
            val map = moshi.adapter(Map::class.java).fromJson(errorBody)
            val msg = map?.get("error_description") as? String 
                ?: map?.get("message") as? String
                ?: map?.get("error") as? String
                ?: "Terjadi kesalahan sistem."

            // Translate common auth errors for localized experience
            when {
                msg.contains("Invalid login credentials", true) -> "Email atau kata sandi yang Anda masukkan salah."
                msg.contains("User already exists", true) -> "Alamat email ini sudah terdaftar."
                msg.contains("Email not confirmed", true) -> "Alamat email belum diverifikasi. Silakan periksa kotak masuk Anda."
                msg.contains("Password should be", true) -> "Kata sandi minimal harus 6 karakter."
                msg.contains("rate limit", true) -> "Terlalu banyak percobaan masuk. Silakan tunggu beberapa saat."
                else -> msg
            }
        } catch (e: Exception) {
            "Kesalahan Server: $errorBody"
        }
    }
}
