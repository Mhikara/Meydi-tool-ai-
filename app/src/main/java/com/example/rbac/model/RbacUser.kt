package com.example.rbac.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RbacUser(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val role: String = "user", // "owner", "admin", "user"
    val status: String = "active", // "active", "suspended"
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val photoURL: String = "",
    val phoneNumber: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "nama" to nama,
            "email" to email,
            "role" to role,
            "status" to status,
            "createdAt" to createdAt,
            "lastLogin" to lastLogin,
            "photoURL" to photoURL,
            "phoneNumber" to phoneNumber
        )
    }

    val userRole: UserRole get() = UserRole.fromKey(role)
    val userStatus: UserStatus get() = UserStatus.fromKey(status)
}
