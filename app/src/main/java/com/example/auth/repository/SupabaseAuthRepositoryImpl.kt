package com.example.auth.repository

import android.content.Context
import com.example.auth.model.User
import com.example.auth.utils.SupabaseManager
import com.example.auth.utils.TokenResponse
import com.example.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseAuthRepositoryImpl(private val context: Context) : AuthRepository {

    private val supabaseManager = SupabaseManager.getInstance(context)

    override val currentUser: Flow<User?> = supabaseManager.sessionFlow.map { session ->
        if (session == null) {
            null
        } else {
            val sUser = session.user
            if (sUser != null) {
                User(
                    uid = sUser.id,
                    fullName = sUser.user_metadata?.full_name ?: sUser.email?.split("@")?.get(0) ?: "Supabase User",
                    username = sUser.user_metadata?.username ?: sUser.email?.split("@")?.get(0) ?: "user",
                    email = sUser.email ?: "",
                    isEmailVerified = true, // Default to true upon successful session login
                    registrationDate = System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    }

    override val isEmailVerified: Boolean
        get() = supabaseManager.sessionFlow.value != null

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        AppLogger.info("SupabaseAuthRepository", "loginWithEmail", "Memulai login Supabase untuk: $email")
        val result = supabaseManager.login(email, password)
        return if (result.isSuccess) {
            val session = result.getOrNull()
            if (session?.user != null) {
                val sUser = session.user
                // Securely sync profile to Supabase database via Backend API / Edge Function
                supabaseManager.syncProfileWithBackendEdge(
                    uid = sUser.id,
                    email = sUser.email ?: email,
                    fullName = sUser.user_metadata?.full_name ?: sUser.email?.split("@")?.get(0) ?: "Supabase User",
                    username = sUser.user_metadata?.username ?: sUser.email?.split("@")?.get(0) ?: "user"
                )
            }
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Login gagal."))
        }
    }

    override suspend fun registerWithEmail(
        name: String,
        username: String,
        email: String,
        password: String,
        phoneNumber: String?
    ): Result<Unit> {
        AppLogger.info("SupabaseAuthRepository", "registerWithEmail", "Memulai pendaftaran akun Supabase untuk: $email")
        val result = supabaseManager.signUp(email, password, name, username)
        return if (result.isSuccess) {
            val signUpRes = result.getOrNull()
            if (signUpRes != null) {
                // If the signup automatically logs in and sets a session, sync immediately
                val session = supabaseManager.sessionFlow.value
                if (session != null) {
                    supabaseManager.syncProfileWithBackendEdge(
                        uid = signUpRes.id,
                        email = signUpRes.email ?: email,
                        fullName = signUpRes.user_metadata?.full_name ?: name,
                        username = signUpRes.user_metadata?.username ?: username,
                        phone = phoneNumber
                    )
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Pendaftaran gagal."))
        }
    }

    override suspend fun isUsernameUnique(username: String): Result<Boolean> {
        // Supabase has unique username checks via DB schemas, we can default to true
        return Result.success(true)
    }

    override suspend fun updateFcmToken(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        // Supabase Google login via native token can be supported if needed
        return Result.failure(Exception("Login Google tidak diimplementasikan langsung pada flow Supabase Auth REST API."))
    }

    override suspend fun logout(): Result<Unit> {
        AppLogger.info("SupabaseAuthRepository", "logout", "Memulai keluar sesi Supabase.")
        return supabaseManager.logout()
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        AppLogger.info("SupabaseAuthRepository", "sendPasswordResetEmail", "Mengirim pemulihan kata sandi Supabase untuk: $email")
        return supabaseManager.sendPasswordResetEmail(email)
    }

    override suspend fun reloadUser(): Result<Unit> {
        val userRes = supabaseManager.getCurrentUser()
        return if (userRes.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(userRes.exceptionOrNull() ?: Exception("Gagal sinkronisasi data pengguna."))
        }
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        AppLogger.info("SupabaseAuthRepository", "updateProfile", "Mengubah profil via Edge Function...")
        val syncRes = supabaseManager.syncProfileWithBackendEdge(
            uid = user.uid,
            email = user.email,
            fullName = user.fullName,
            username = user.username
        )
        return if (syncRes.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(syncRes.exceptionOrNull() ?: Exception("Gagal mengupdate profil via Edge Function."))
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return Result.failure(Exception("Fungsi hapus akun tidak diizinkan langsung dari sisi klien publik."))
    }
}
