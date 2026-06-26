package com.example.utils

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    
    // Inisialisasi Auth dengan Exception Handling agar aplikasi tidak crash 
    // jika google-services.json belum ditambahkan oleh pengguna.
    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun isFirebaseSetup(): Boolean {
        return auth != null && firestore != null
    }

    fun showNotSetupMessage(context: Context) {
        Toast.makeText(
            context,
            "Firebase belum dikonfigurasi. Harap tambahkan google-services.json (Lihat FIREBASE_SETUP.md)",
            Toast.LENGTH_LONG
        ).show()
    }

    // Role User Model
    data class UserProfile(
        val uid: String = "",
        val fullName: String = "",
        val username: String = "",
        val email: String = "",
        val phone: String = "",
        val photoUrl: String = "",
        val bio: String = "",
        val role: String = "user", // "user", "admin", "owner"
        val joinDate: Long = System.currentTimeMillis(),
        val lastLoginDate: Long = System.currentTimeMillis()
    )

    // Menyimpan/Update data user ke Firestore
    suspend fun saveUserToFirestore(uid: String, profile: UserProfile): Boolean {
        return try {
            if (firestore != null) {
                firestore!!.collection("users").document(uid).set(profile).await()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Mengambil profil dari Firestore
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            if (firestore != null) {
                val snapshot = firestore!!.collection("users").document(uid).get().await()
                snapshot.toObject(UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun isGuest(): Boolean {
        return auth?.currentUser?.isAnonymous ?: true
    }

    suspend fun signInAnonymously(): Result<Boolean> {
        return try {
            auth?.signInAnonymously()?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkCredential(credential: com.google.firebase.auth.AuthCredential): Result<Boolean> {
        return try {
            auth?.currentUser?.linkWithCredential(credential)?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth?.signOut()
    }
}
