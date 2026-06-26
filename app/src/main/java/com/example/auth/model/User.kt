package com.example.auth.model

import java.util.Date

enum class UserRole {
    FREE, PREMIUM, ADMIN, OWNER
}

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val role: String = "FREE", // FREE, PREMIUM, ADMIN, OWNER
    val isPremium: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isActive: Boolean = true,
    val isBlocked: Boolean = false,
    val registrationDate: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
    val fcmToken: String? = null,
    val permissions: Map<String, Boolean> = emptyMap()
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    object EmailNotVerified : AuthState()
    data class Error(val message: String) : AuthState()
}
