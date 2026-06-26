package com.example.auth.repository

import android.content.Context
import com.example.api.ApiKeyRegistry
import com.example.auth.model.User
import com.example.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class DynamicAuthRepositoryImpl : AuthRepository {

    private val firebaseRepo by lazy { FirebaseAuthRepositoryImpl() }
    private val supabaseRepo by lazy { 
        val ctx = ApiKeyRegistry.getContext()
        if (ctx != null) SupabaseAuthRepositoryImpl(ctx) else null
    }

    private fun getActiveRepository(): AuthRepository {
        val ctx = ApiKeyRegistry.getContext()
        val isSupabaseConfigured = ApiKeyRegistry.hasCustomSupabase()
        
        // Read preference for preferred auth system if both are present
        val prefs = ctx?.getSharedPreferences("AuthSystemPrefs", Context.MODE_PRIVATE)
        val selectedSystem = prefs?.getString("active_auth_system", "auto") ?: "auto"

        return if (selectedSystem == "supabase" || (selectedSystem == "auto" && isSupabaseConfigured)) {
            val sRepo = supabaseRepo
            if (sRepo != null) {
                AppLogger.debug("DynamicAuthRepository", "getActiveRepository", "Menggunakan backend Supabase Authentication.")
                sRepo
            } else {
                AppLogger.warn("DynamicAuthRepository", "getActiveRepository", "Konteks aplikasi kosong. Default ke Firebase.")
                firebaseRepo
            }
        } else {
            AppLogger.debug("DynamicAuthRepository", "getActiveRepository", "Menggunakan backend Firebase Authentication.")
            firebaseRepo
        }
    }

    override val currentUser: Flow<User?>
        get() = getActiveRepository().currentUser

    override val isEmailVerified: Boolean
        get() = getActiveRepository().isEmailVerified

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return getActiveRepository().loginWithEmail(email, password)
    }

    override suspend fun registerWithEmail(
        name: String,
        username: String,
        email: String,
        password: String,
        phoneNumber: String?
    ): Result<Unit> {
        return getActiveRepository().registerWithEmail(name, username, email, password, phoneNumber)
    }

    override suspend fun isUsernameUnique(username: String): Result<Boolean> {
        return getActiveRepository().isUsernameUnique(username)
    }

    override suspend fun updateFcmToken(): Result<Unit> {
        return getActiveRepository().updateFcmToken()
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return getActiveRepository().loginWithGoogle(idToken)
    }

    override suspend fun logout(): Result<Unit> {
        return getActiveRepository().logout()
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return getActiveRepository().sendEmailVerification()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return getActiveRepository().sendPasswordResetEmail(email)
    }

    override suspend fun reloadUser(): Result<Unit> {
        return getActiveRepository().reloadUser()
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        return getActiveRepository().updateProfile(user)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return getActiveRepository().deleteAccount()
    }
}
