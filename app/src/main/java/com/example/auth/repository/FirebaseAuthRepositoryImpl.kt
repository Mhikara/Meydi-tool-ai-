package com.example.auth.repository

import com.example.auth.model.User
import com.example.auth.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl() : AuthRepository {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val messaging: FirebaseMessaging? by lazy {
        try {
            FirebaseMessaging.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    override val currentUser: Flow<User?> = callbackFlow {
        val currentAuth = auth
        val currentFirestore = firestore
        
        if (currentAuth == null || currentFirestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                // Fetch additional data from Firestore
                currentFirestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(mapFirebaseUserToUser(firebaseUser))
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null && snapshot.exists()) {
                            trySend(snapshot.toObject(User::class.java))
                        } else {
                            trySend(mapFirebaseUserToUser(firebaseUser))
                        }
                    }
            }
        }
        currentAuth.addAuthStateListener(listener)
        awaitClose { currentAuth.removeAuthStateListener(listener) }
    }

    override val isEmailVerified: Boolean
        get() = auth?.currentUser?.isEmailVerified ?: false

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            currentAuth.signInWithEmailAndPassword(email, password).await()
            updateLastLogin()
            logAuditEvent("LOGIN_EMAIL_SUCCESS", email)
            Result.success(Unit)
        } catch (e: Exception) {
            logAuditEvent("LOGIN_EMAIL_FAILURE", email, e.message)
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(name: String, username: String, email: String, password: String, phoneNumber: String?): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            // Check username uniqueness
            val isUnique = isUsernameUnique(username).getOrDefault(false)
            if (!isUnique) throw Exception("Username already taken")

            val result = currentAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            val newUser = User(
                uid = firebaseUser.uid,
                fullName = name,
                username = username,
                email = email,
                phoneNumber = phoneNumber,
                isEmailVerified = false,
                registrationDate = System.currentTimeMillis()
            )
            
            currentFirestore.collection("users").document(firebaseUser.uid).set(newUser).await()
            firebaseUser.sendEmailVerification().await()
            
            logAuditEvent("REGISTER_SUCCESS", email)
            Result.success(Unit)
        } catch (e: Exception) {
            logAuditEvent("REGISTER_FAILURE", email, e.message)
            Result.failure(e)
        }
    }

    override suspend fun isUsernameUnique(username: String): Result<Boolean> {
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val query = currentFirestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            Result.success(query.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        val currentMessaging = messaging ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val uid = currentAuth.currentUser?.uid ?: throw Exception("No user logged in")
            val token = currentMessaging.token.await()
            currentFirestore.collection("users").document(uid).update("fcmToken", token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google login failed")
            
            val userDoc = currentFirestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "",
                    username = firebaseUser.email?.split("@")?.get(0) ?: firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    profilePicture = firebaseUser.photoUrl?.toString(),
                    isEmailVerified = true, // Google accounts are usually verified
                    registrationDate = System.currentTimeMillis()
                )
                currentFirestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                logAuditEvent("GOOGLE_REGISTER_SUCCESS", firebaseUser.email)
            } else {
                updateLastLogin()
                logAuditEvent("GOOGLE_LOGIN_SUCCESS", firebaseUser.email)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            logAuditEvent("GOOGLE_LOGIN_FAILURE", "", e.message)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth?.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth?.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            currentAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            currentAuth.currentUser?.reload()?.await()
            // Update Firestore if email verified status changed
            currentAuth.currentUser?.let { firebaseUser ->
                if (firebaseUser.isEmailVerified) {
                    currentFirestore.collection("users").document(firebaseUser.uid)
                        .update("isEmailVerified", true).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            currentFirestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        val currentFirestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val uid = currentAuth.currentUser?.uid ?: throw Exception("No user logged in")
            currentFirestore.collection("users").document(uid).delete().await()
            currentAuth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateLastLogin() {
        val currentAuth = auth ?: return
        val currentFirestore = firestore ?: return
        currentAuth.currentUser?.let { user ->
            currentFirestore.collection("users").document(user.uid)
                .update("lastLogin", System.currentTimeMillis()).await()
        }
    }

    private suspend fun logAuditEvent(event: String, identifier: String?, details: String? = null) {
        val currentFirestore = firestore ?: return
        try {
            val log = hashMapOf(
                "event" to event,
                "identifier" to identifier,
                "details" to details,
                "timestamp" to System.currentTimeMillis(),
                "uid" to auth?.currentUser?.uid
            )
            currentFirestore.collection("audit_logs").add(log)
        } catch (e: Exception) {
            // Fail silently for logs
        }
    }

    private fun mapFirebaseUserToUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            fullName = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            profilePicture = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified
        )
    }
}
