package com.example.auth.repository

import com.example.auth.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isEmailVerified: Boolean
    
    suspend fun loginWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(name: String, username: String, email: String, password: String, phoneNumber: String? = null): Result<Unit>
    suspend fun isUsernameUnique(username: String): Result<Boolean>
    suspend fun updateFcmToken(): Result<Unit>
    suspend fun loginWithGoogle(idToken: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun reloadUser(): Result<Unit>
    suspend fun updateProfile(user: User): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
