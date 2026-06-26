package com.example.admin.repository

import com.example.auth.model.User
import com.example.admin.model.AuditLog
import com.example.admin.model.SystemStats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

class OwnerRepository(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")
    private val logsCollection = firestore.collection("audit_logs")
    private val statsCollection = firestore.collection("system_stats")

    suspend fun getAllUsers(): List<User> {
        return usersCollection.get().await().toObjects(User::class.java)
    }

    suspend fun updateUserRole(uid: String, newRole: String, adminUid: String) {
        usersCollection.document(uid).update("role", newRole).await()
        logAction(adminUid, "UPDATE_ROLE", uid, "Changed role to $newRole")
    }

    suspend fun setUserBlockStatus(uid: String, isBlocked: Boolean, adminUid: String) {
        usersCollection.document(uid).update("isBlocked", isBlocked).await()
        val action = if (isBlocked) "BLOCK_USER" else "UNBLOCK_USER"
        logAction(adminUid, action, uid, "Set block status to $isBlocked")
    }

    suspend fun getSystemStats(): Flow<SystemStats> = flow {
        // In real production, this would be a real-time listener or a cloud function call
        val stats = statsCollection.document("current").get().await().toObject(SystemStats::class.java)
        emit(stats ?: SystemStats())
    }

    private suspend fun logAction(adminUid: String, action: String, targetUid: String?, details: String) {
        val log = AuditLog(
            adminUid = adminUid,
            action = action,
            targetUid = targetUid,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        logsCollection.add(log).await()
    }
}
