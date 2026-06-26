package com.example.security

import android.content.Context
import com.example.utils.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

// Struktur informasi sesi pengguna yang aman
data class UserSession(
    val email: String = "",
    val uid: String = "",
    val isLoggedIn: Boolean = false,
    val loginTime: Long = 0L,
    val isVerified: Boolean = false
)

class SessionManager(context: Context) {
    private val secureStorage = SecureStorage(context)
    private val auth: FirebaseAuth? = FirebaseManager.auth

    companion object {
        private const val KEY_USER_EMAIL = "session_user_email"
        private const val KEY_USER_UID = "session_user_uid"
        private const val KEY_IS_LOGGED_IN = "session_is_logged_in"
        private const val KEY_LOGIN_TIME = "session_login_time"
        private const val KEY_IS_VERIFIED = "session_is_verified"
    }

    // Menyimpan sesi secara aman ke SecureStorage
    fun saveSession(email: String, uid: String, isLoggedIn: Boolean, isVerified: Boolean) {
        secureStorage.putString(KEY_USER_EMAIL, email)
        secureStorage.putString(KEY_USER_UID, uid)
        secureStorage.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        secureStorage.putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
        secureStorage.putBoolean(KEY_IS_VERIFIED, isVerified)
    }

    // Mendapatkan data sesi terenkripsi saat ini
    fun getSession(): UserSession {
        val email = secureStorage.getString(KEY_USER_EMAIL, "guest") ?: "guest"
        val uid = secureStorage.getString(KEY_USER_UID, "") ?: ""
        val isLoggedIn = secureStorage.getBoolean(KEY_IS_LOGGED_IN, false)
        val loginTime = secureStorage.getLong(KEY_LOGIN_TIME, 0L)
        val isVerified = secureStorage.getBoolean(KEY_IS_VERIFIED, false)
        return UserSession(email, uid, isLoggedIn, loginTime, isVerified)
    }

    // Memeriksa apakah token sesi Firebase masih valid
    suspend fun isSessionValid(): Boolean {
        val currentUser = auth?.currentUser ?: return false
        return try {
            // Memaksa sinkronisasi token dengan server Firebase untuk memvalidasi status token sesi
            currentUser.getIdToken(true).await()
            // Perbarui info verifikasi secara lokal di penyimpanan sesi
            secureStorage.putBoolean(KEY_IS_VERIFIED, currentUser.isEmailVerified)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false // Token kadaluarsa atau koneksi bermasalah
        }
    }

    // Menghapus data sesi secara aman (Logout)
    fun clearSession() {
        secureStorage.remove(KEY_USER_EMAIL)
        secureStorage.remove(KEY_USER_UID)
        secureStorage.putBoolean(KEY_IS_LOGGED_IN, false)
        secureStorage.putLong(KEY_LOGIN_TIME, 0L)
        secureStorage.putBoolean(KEY_IS_VERIFIED, false)
        auth?.signOut()
    }

    // Memeriksa status login otomatis (Auto Login)
    fun isAutoLoginEnabled(): Boolean {
        return getSession().isLoggedIn
    }
}
