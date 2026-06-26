package com.example.rbac.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.rbac.model.RbacUser
import com.example.rbac.model.UserRole
import com.example.rbac.model.UserStatus
import com.example.utils.FirebaseManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RbacRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("meydi_rbac_cache", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore? get() = FirebaseManager.firestore
    
    private val _currentUserState = MutableStateFlow<RbacUser?>(null)
    val currentUserState: StateFlow<RbacUser?> = _currentUserState

    init {
        // Load initially cached user
        loadCachedUser()
    }

    private fun loadCachedUser() {
        val uid = prefs.getString("cached_uid", null) ?: return
        val nama = prefs.getString("cached_nama", "") ?: ""
        val email = prefs.getString("cached_email", "") ?: ""
        val role = prefs.getString("cached_role", "user") ?: "user"
        val status = prefs.getString("cached_status", "active") ?: "active"
        val createdAt = prefs.getLong("cached_created_at", System.currentTimeMillis())
        val lastLogin = prefs.getLong("cached_last_login", System.currentTimeMillis())
        val photoUrl = prefs.getString("cached_photo", "") ?: ""
        val phone = prefs.getString("cached_phone", "") ?: ""

        val user = RbacUser(uid, nama, email, role, status, createdAt, lastLogin, photoUrl, phone)
        _currentUserState.value = user
        Log.d("RbacRepository", "Loaded cached user: ${user.email} with role: ${user.role}")
    }

    fun cacheCurrentUser(user: RbacUser) {
        prefs.edit().apply {
            putString("cached_uid", user.uid)
            putString("cached_nama", user.nama)
            putString("cached_email", user.email)
            putString("cached_role", user.role)
            putString("cached_status", user.status)
            putLong("cached_created_at", user.createdAt)
            putLong("cached_last_login", user.lastLogin)
            putString("cached_photo", user.photoURL)
            putString("cached_phone", user.phoneNumber)
        }.apply()
        _currentUserState.value = user
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        _currentUserState.value = null
    }

    /**
     * Fetch user profile from Firestore or fallback to mock list / local cache if offline or Firebase not setup.
     */
    suspend fun fetchUserProfile(uid: String, email: String? = null): RbacUser = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseSetup() && firestore != null) {
            try {
                val doc = firestore!!.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(RbacUser::class.java)!!
                    cacheCurrentUser(user)
                    // Log audit
                    recordSecurityLog("FETCH_PROFILE", "Successfully fetched user profile for ${user.email} from Firestore", "INFO")
                    return@withContext user
                } else {
                    // Create new user record
                    val initialRole = if (email == "meydihikara@gmail.com") "owner" else "user"
                    val newUser = RbacUser(
                        uid = uid,
                        nama = email?.substringBefore("@") ?: "User",
                        email = email ?: "",
                        role = initialRole,
                        status = "active",
                        createdAt = System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis()
                    )
                    firestore!!.collection("users").document(uid).set(newUser).await()
                    cacheCurrentUser(newUser)
                    recordSecurityLog("CREATE_PROFILE", "Created new profile for ${newUser.email} on Firestore with role: ${newUser.role}", "INFO")
                    return@withContext newUser
                }
            } catch (e: Exception) {
                Log.w("RbacRepository", "Error fetching Firestore profile, falling back to local/mock", e)
                recordSecurityLog("FETCH_PROFILE_OFFLINE", "Offline fallback: loaded profile for ${email ?: "local"} from cache/mock due to ${e.localizedMessage}", "WARNING")
            }
        }

        // Fallback or Simulated Mock Behavior (Excellent for physical device/emulator offline and missing google-services.json)
        val cached = _currentUserState.value
        if (cached != null && cached.uid == uid) {
            return@withContext cached
        }

        val fallbackRole = if (email == "meydihikara@gmail.com") "owner" else "user"
        val fallbackUser = RbacUser(
            uid = uid,
            nama = email?.substringBefore("@")?.capitalize() ?: "Simulated User",
            email = email ?: "user@meydiai.com",
            role = fallbackRole,
            status = "active",
            createdAt = System.currentTimeMillis() - 86400000L * 10, // 10 days ago
            lastLogin = System.currentTimeMillis()
        )
        cacheCurrentUser(fallbackUser)
        return@withContext fallbackUser
    }

    /**
     * Updates an arbitrary user's role on Firestore and/or locally.
     * Rules: Only Owner can change user roles.
     */
    suspend fun updateUserRole(targetUid: String, newRole: UserRole, editorRole: UserRole): Boolean = withContext(Dispatchers.IO) {
        if (editorRole != UserRole.OWNER) {
            recordSecurityLog("UNAUTHORIZED_ROLE_CHANGE", "Unauthorized attempt to change role for $targetUid by editor with role: $editorRole", "CRITICAL")
            return@withContext false
        }

        if (FirebaseManager.isFirebaseSetup() && firestore != null) {
            try {
                firestore!!.collection("users").document(targetUid).update("role", newRole.key).await()
                recordSecurityLog("ROLE_CHANGED_FIRESTORE", "Successfully changed role of $targetUid to ${newRole.label} in Firestore", "INFO")
                return@withContext true
            } catch (e: Exception) {
                Log.e("RbacRepository", "Error updating role in Firestore", e)
                recordSecurityLog("ROLE_CHANGE_ERROR", "Failed to update role of $targetUid in Firestore: ${e.localizedMessage}", "CRITICAL")
            }
        }

        // Offline / Simulation fallback: update inside mock local storage
        updateMockUserInPrefs(targetUid, role = newRole.key)
        recordSecurityLog("ROLE_CHANGED_MOCK", "Changed mock user $targetUid role to ${newRole.label} in local mock storage", "WARNING")
        return@withContext true
    }

    /**
     * Suspends or activates a user.
     * Rules: Owner and Admin can suspend users. Admin CANNOT suspend Owner or other Admins.
     */
    suspend fun updateUserStatus(targetUid: String, targetEmail: String, newStatus: UserStatus, editorRole: UserRole, targetRole: UserRole): Boolean = withContext(Dispatchers.IO) {
        // Owner has absolute powers
        // Admin can suspend Users, but cannot suspend Owner or Admins
        val isAuthorized = when (editorRole) {
            UserRole.OWNER -> true
            UserRole.ADMIN -> targetRole == UserRole.USER
            UserRole.USER -> false
        }

        if (!isAuthorized) {
            recordSecurityLog("UNAUTHORIZED_STATUS_CHANGE", "Unauthorized status change attempt of $targetEmail to ${newStatus.label} by $editorRole", "CRITICAL")
            return@withContext false
        }

        if (FirebaseManager.isFirebaseSetup() && firestore != null) {
            try {
                firestore!!.collection("users").document(targetUid).update("status", newStatus.key).await()
                recordSecurityLog("STATUS_CHANGED_FIRESTORE", "Updated user $targetEmail status to ${newStatus.label} in Firestore", "INFO")
                return@withContext true
            } catch (e: Exception) {
                Log.e("RbacRepository", "Error updating status in Firestore", e)
                recordSecurityLog("STATUS_CHANGE_ERROR", "Failed updating user $targetEmail status: ${e.localizedMessage}", "CRITICAL")
            }
        }

        updateMockUserInPrefs(targetUid, status = newStatus.key)
        recordSecurityLog("STATUS_CHANGED_MOCK", "Updated mock user $targetEmail status to ${newStatus.label} in mock storage", "WARNING")
        return@withContext true
    }

    /**
     * Loads list of all users from Firestore or fallbacks to locally saved mock database.
     */
    suspend fun loadAllUsers(): List<RbacUser> = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseSetup() && firestore != null) {
            try {
                val query = firestore!!.collection("users").get().await()
                val users = query.documents.mapNotNull { it.toObject(RbacUser::class.java) }
                saveMockUsersToPrefs(users) // keep local mock in sync
                return@withContext users
            } catch (e: Exception) {
                Log.w("RbacRepository", "Error reading all users from Firestore, using local mock store", e)
            }
        }

        // Return from Local Mock Store
        return@withContext loadMockUsersFromPrefs()
    }

    // ==========================================
    // MOCK DATA STORAGE HELPERS
    // ==========================================
    private fun loadMockUsersFromPrefs(): List<RbacUser> {
        val jsonStr = prefs.getString("mock_users_json", null)
        if (jsonStr.isNullOrEmpty()) {
            val defaultUsers = listOf(
                RbacUser("uid_owner", "Meydi Hikara", "meydihikara@gmail.com", "owner", "active", System.currentTimeMillis() - 86400000 * 30),
                RbacUser("uid_admin1", "Budi Santoso", "budi.admin@meydiai.com", "admin", "active", System.currentTimeMillis() - 86400000 * 15),
                RbacUser("uid_user1", "Reza Artamevia", "reza.user@meydiai.com", "user", "active", System.currentTimeMillis() - 86400000 * 5),
                RbacUser("uid_user2", "Siti Rahma", "siti.user@meydiai.com", "user", "suspended", System.currentTimeMillis() - 86400000 * 3),
                RbacUser("uid_user3", "Dian Sastro", "dian.user@meydiai.com", "user", "active", System.currentTimeMillis() - 86400000 * 2)
            )
            saveMockUsersToPrefs(defaultUsers)
            return defaultUsers
        }

        val list = mutableListOf<RbacUser>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RbacUser(
                        uid = obj.getString("uid"),
                        nama = obj.getString("nama"),
                        email = obj.getString("email"),
                        role = obj.getString("role"),
                        status = obj.getString("status"),
                        createdAt = obj.getLong("createdAt"),
                        lastLogin = obj.getLong("lastLogin"),
                        photoURL = obj.optString("photoURL", ""),
                        phoneNumber = obj.optString("phoneNumber", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveMockUsersToPrefs(users: List<RbacUser>) {
        val arr = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("uid", u.uid)
                put("nama", u.nama)
                put("email", u.email)
                put("role", u.role)
                put("status", u.status)
                put("createdAt", u.createdAt)
                put("lastLogin", u.lastLogin)
                put("photoURL", u.photoURL)
                put("phoneNumber", u.phoneNumber)
            }
            arr.put(obj)
        }
        prefs.edit().putString("mock_users_json", arr.toString()).apply()
    }

    private fun updateMockUserInPrefs(uid: String, role: String? = null, status: String? = null) {
        val users = loadMockUsersFromPrefs().map { u ->
            if (u.uid == uid) {
                RbacUser(
                    uid = u.uid,
                    nama = u.nama,
                    email = u.email,
                    role = role ?: u.role,
                    status = status ?: u.status,
                    createdAt = u.createdAt,
                    lastLogin = System.currentTimeMillis(),
                    photoURL = u.photoURL,
                    phoneNumber = u.phoneNumber
                )
            } else u
        }
        saveMockUsersToPrefs(users)
    }

    suspend fun createMockUser(nama: String, email: String, role: UserRole): Boolean = withContext(Dispatchers.IO) {
        val current = loadMockUsersFromPrefs().toMutableList()
        val generatedUid = "uid_mock_" + java.util.UUID.randomUUID().toString().substring(0, 8)
        val newUser = RbacUser(
            uid = generatedUid,
            nama = nama,
            email = email,
            role = role.key,
            status = "active",
            createdAt = System.currentTimeMillis()
        )
        current.add(newUser)
        saveMockUsersToPrefs(current)
        recordSecurityLog("CREATE_MOCK_USER", "Created simulated user: $email with role ${role.label}", "INFO")
        return@withContext true
    }

    suspend fun deleteMockUser(uid: String): Boolean = withContext(Dispatchers.IO) {
        val current = loadMockUsersFromPrefs().toMutableList()
        val user = current.find { it.uid == uid }
        if (user != null) {
            if (user.role == "owner") {
                recordSecurityLog("UNAUTHORIZED_DELETE", "Failed deleting user ${user.email} because they are an Owner!", "CRITICAL")
                return@withContext false
            }
            current.remove(user)
            saveMockUsersToPrefs(current)
            recordSecurityLog("DELETE_USER", "Deleted user ${user.email} from registry", "WARNING")
            return@withContext true
        }
        return@withContext false
    }

    // Security Audit Log records (shares with general logs)
    private fun recordSecurityLog(action: String, message: String, severity: String) {
        try {
            val formatNow = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            val logPref = context.getSharedPreferences("SecurityAuditLogs", Context.MODE_PRIVATE)
            val currentLogs = logPref.getStringSet("audit_logs", emptySet()) ?: emptySet()
            val newLogs = currentLogs.toMutableSet()
            newLogs.add("$formatNow | $action | $severity | $message")
            logPref.edit().putStringSet("audit_logs", newLogs).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
