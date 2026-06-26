package com.example.security

import com.example.utils.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.IOException

// Status Verifikasi Email
object VerificationStatusConstants {
    const val UNVERIFIED = "Belum Diverifikasi"
    const val VERIFYING = "Sedang Memverifikasi"
    const val VERIFIED = "Berhasil Diverifikasi"
    const val FAILED = "Gagal Memverifikasi"
}

// Model data penyimpanan informasi verifikasi di Cloud Firestore
data class VerificationData(
    val uid: String = "",
    val email: String = "",
    val status: String = VerificationStatusConstants.UNVERIFIED,
    val registrationTime: Long = 0L,
    val emailSentTime: Long = 0L,
    val verifiedTime: Long = 0L,
    val lastLoginTime: Long = 0L
)

class EmailVerificationService {

    private val auth: FirebaseAuth? = FirebaseManager.auth
    private val firestore: FirebaseFirestore? = FirebaseManager.firestore

    // Mengirim email verifikasi secara otomatis atau manual
    suspend fun sendVerificationEmail(): Boolean {
        val currentUser = auth?.currentUser ?: return false
        return try {
            currentUser.sendEmailVerification().await()
            
            // Perbarui data pengiriman email ke Cloud Firestore
            updateSentEmailMeta(currentUser.uid, currentUser.email ?: "")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Melakukan pengecekan status verifikasi email terupdate langsung dari Firebase Auth
    suspend fun checkVerificationStatus(): Pair<Boolean, VerificationData?> {
        val currentUser = auth?.currentUser ?: return Pair(false, null)
        try {
            // Paksa reload status user dari Firebase Server
            currentUser.reload().await()
            val isVerified = currentUser.isEmailVerified
            
            var data = getVerificationInfo(currentUser.uid)
            if (data == null) {
                data = VerificationData(
                    uid = currentUser.uid,
                    email = currentUser.email ?: "",
                    status = if (isVerified) VerificationStatusConstants.VERIFIED else VerificationStatusConstants.UNVERIFIED,
                    registrationTime = System.currentTimeMillis() - 120000, // Estimasi waktu lampau jika belum ada
                    lastLoginTime = System.currentTimeMillis()
                )
            }

            val updatedData = if (isVerified) {
                data.copy(
                    status = VerificationStatusConstants.VERIFIED,
                    verifiedTime = if (data.verifiedTime == 0L) System.currentTimeMillis() else data.verifiedTime,
                    lastLoginTime = System.currentTimeMillis()
                )
            } else {
                data.copy(
                    status = if (data.emailSentTime > 0L) VerificationStatusConstants.VERIFYING else VerificationStatusConstants.UNVERIFIED,
                    lastLoginTime = System.currentTimeMillis()
                )
            }

            // Simpan status terupdate ke Cloud Firestore
            saveVerificationInfo(currentUser.uid, updatedData)
            
            return Pair(isVerified, updatedData)
        } catch (e: Exception) {
            e.printStackTrace()
            // Mengembalikan status lokal jika kegagalan koneksi terjadi
            val data = getVerificationInfo(currentUser.uid) ?: VerificationData(
                uid = currentUser.uid,
                email = currentUser.email ?: "",
                status = VerificationStatusConstants.FAILED,
                lastLoginTime = System.currentTimeMillis()
            )
            return Pair(false, data)
        }
    }

    // Mengambil info verifikasi dari Cloud Firestore
    suspend fun getVerificationInfo(uid: String): VerificationData? {
        if (firestore == null) return null
        return try {
            val snapshot = firestore.collection("email_verifications").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(VerificationData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Menyimpan info verifikasi baru ke Cloud Firestore
    suspend fun saveVerificationInfo(uid: String, data: VerificationData): Boolean {
        if (firestore == null) return false
        return try {
            firestore.collection("email_verifications").document(uid).set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Update metadata saat email verifikasi terkirim
    private suspend fun updateSentEmailMeta(uid: String, email: String) {
        if (firestore == null) return
        try {
            val existing = getVerificationInfo(uid)
            val updated = if (existing != null) {
                existing.copy(
                    status = VerificationStatusConstants.VERIFYING,
                    emailSentTime = System.currentTimeMillis()
                )
            } else {
                VerificationData(
                    uid = uid,
                    email = email,
                    status = VerificationStatusConstants.VERIFYING,
                    registrationTime = System.currentTimeMillis(),
                    emailSentTime = System.currentTimeMillis()
                )
            }
            saveVerificationInfo(uid, updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Menghapus sesi / logout jika diperlukan
    fun logout() {
        auth?.signOut()
    }
}
