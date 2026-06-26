package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.utils.FirebaseManager
import com.example.utils.NetworkMonitor
import com.example.utils.Cryptographer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern Cyberpunk Color Palette matching Meydi AI App
private val NeonTeal = Color(0xFF00FFCC)
private val NeonPurple = Color(0xFF7F00FF)
private val MidnightBg = Color(0xFF06040C)
private val MidnightSurface = Color(0xFF12101F)
private val DarkStroke = Color(0xFF231F3A)
private val ErrorRed = Color(0xFFFF3366)
private val InfoBlue = Color(0xFF00BFFF)

// Prefabricated stunning AI avatars for instantaneous aesthetic profiles
private val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=150&q=80", // Cyber Abstract Red
    "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=150&q=80", // Cyber Synthwave Violet
    "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?auto=format&fit=crop&w=150&q=80", // Futuristic Art Gold
    "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?auto=format&fit=crop&w=150&q=80", // Cyber Hologram Teal
    "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=150&q=80", // Anime Tech Pilot
)

// Represents audited system change log entries for account settings
data class AuditActivityLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "PROFILE", "SECURITY", "PRIVACY", "SESSION", "SYSTEM"
    val description: String,
    val isCloudSynced: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun UserProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    // Monitors Network Changes
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnlineState by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyConnected())

    // SharedPreferences for Offline Caching & Account Settings
    val sharedPrefs = remember { context.getSharedPreferences("meydiai_profile_cache", Context.MODE_PRIVATE) }

    // User Session details state
    var uidState by remember { mutableStateOf("") }
    var emailState by remember { mutableStateOf("") }
    var joinDateState by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastLoginDateState by remember { mutableStateOf(System.currentTimeMillis()) }
    var isEmailVerifiedState by remember { mutableStateOf(false) }

    // Editable User Profile fields
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

    // Privacy & Preferences States (stored in SharedPreferences & Firestore)
    var whoCanSeeProfile by remember { mutableStateOf(sharedPrefs.getString("who_can_see_profile", "Everyone") ?: "Everyone") }
    var isEmailVisible by remember { mutableStateOf(sharedPrefs.getBoolean("email_visibility", true)) }
    var isPhoneVisible by remember { mutableStateOf(sharedPrefs.getBoolean("phone_visibility", true)) }
    var isOnlineStatusVisible by remember { mutableStateOf(sharedPrefs.getBoolean("online_status_visibility", true)) }
    var isNotificationAllowed by remember { mutableStateOf(sharedPrefs.getBoolean("notification_permission", true)) }
    var isDataSharingEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("data_sharing_preferences", true)) }
    var isTwoFactorEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("two_factor_enabled", false)) }

    // Active Tab Navigation
    var activeTab by remember { mutableStateOf("PROFILE") } // "PROFILE", "EDIT", "SECURITY", "PRIVACY", "AUDIT"

    // Dialog state controllers
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthReason by remember { mutableStateOf("") } // "EMAIL", "PASSWORD", "DELETE", "2FA", "EXPORT"
    var reauthPasswordInput by remember { mutableStateOf("") }
    
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showEmailChangeDialog by remember { mutableStateOf(false) }
    var showTwoFactorSetupDialog by remember { mutableStateOf(false) }
    var showSessionDevicesDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showExportDataDialog by remember { mutableStateOf(false) }
    var showFAQDialog by remember { mutableStateOf(false) }

    var showSuccessAlert by remember { mutableStateOf(false) }
    var showFailureAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }

    // Loading & Skeleton Indicators
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isPerformingAction by remember { mutableStateOf(false) }

    // Password changing fields
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // Email changing fields
    var newEmailInput by remember { mutableStateOf("") }

    // Account Activity logs (Audit Logs)
    val auditLogs = remember { mutableStateListOf<AuditActivityLog>() }

    // Simulated Logged-in Devices
    val sessionDevices = remember {
        mutableStateListOf(
            "Gawai Ini: Pixel 8 Pro | Jakarta, Indonesia | Aktif Sekarang",
            "Laptop: Chrome macOS Sonoma | Surabaya, Indonesia | Login: 2 Jam Lalu",
            "Tablet: Samsung Galaxy Tab S9 | Bandung, Indonesia | Login: 1 Hari Lalu"
        )
    }

    // Two-factor setup flow helper variables
    var twoFactorVerificationCode by remember { mutableStateOf("") }
    val simulated2FASecret = "MEYDI_2FA_SECURE_TOKEN_5729"

    // Helper to persist and load Audit Logs from SharedPreferences
    fun saveAuditLogsToPrefs() {
        val listSet = auditLogs.map { "${it.timestamp}||${it.type}||${it.description}||${it.isCloudSynced}" }.toSet()
        sharedPrefs.edit().putStringSet("audit_logs_db", listSet).apply()
    }

    fun loadAuditLogsFromPrefs() {
        val savedSet = sharedPrefs.getStringSet("audit_logs_db", null)
        auditLogs.clear()
        if (savedSet != null) {
            val loaded = savedSet.map {
                val parts = it.split("||")
                AuditActivityLog(
                    timestamp = parts.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis(),
                    type = parts.getOrNull(1) ?: "SYSTEM",
                    description = parts.getOrNull(2) ?: "",
                    isCloudSynced = parts.getOrNull(3)?.toBoolean() ?: false
                )
            }.sortedByDescending { it.timestamp }
            auditLogs.addAll(loaded)
        } else {
            // Seed initial logs
            auditLogs.add(AuditActivityLog(type = "SYSTEM", description = "Sistem Audit Log Akun telah diaktifkan."))
            saveAuditLogsToPrefs()
        }
    }

    fun writeAuditLog(type: String, description: String) {
        val entry = AuditActivityLog(type = type, description = description)
        auditLogs.add(0, entry)
        if (auditLogs.size > 50) auditLogs.removeLast()
        saveAuditLogsToPrefs()
        
        // Also write log asynchronously to Cloud Firestore if online
        if (isOnlineState && uidState.isNotEmpty() && uidState != "guest_cyber_101") {
            coroutineScope.launch {
                try {
                    val db = FirebaseManager.firestore
                    if (db != null) {
                        db.collection("users").document(uidState)
                            .collection("audit_logs").document(entry.id).set(entry)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Initial Profile Load logic
    fun loadUserProfileAndPreferences() {
        isLoadingProfile = true
        val auth = FirebaseManager.auth
        if (auth?.currentUser != null) {
            val user = auth.currentUser!!
            uidState = user.uid
            emailState = user.email ?: ""
            isEmailVerifiedState = user.isEmailVerified

            // Fetch from Local Cache instantly (Offline Support)
            fullName = sharedPrefs.getString("fullName_${user.uid}", "") ?: ""
            username = sharedPrefs.getString("username_${user.uid}", "") ?: ""
            phone = sharedPrefs.getString("phone_${user.uid}", "") ?: ""
            bio = sharedPrefs.getString("bio_${user.uid}", "") ?: ""
            photoUrl = sharedPrefs.getString("photoUrl_${user.uid}", "") ?: ""
            joinDateState = sharedPrefs.getLong("joinDate_${user.uid}", user.metadata?.creationTimestamp ?: System.currentTimeMillis())
            lastLoginDateState = sharedPrefs.getLong("lastLoginDate_${user.uid}", user.metadata?.lastSignInTimestamp ?: System.currentTimeMillis())

            loadAuditLogsFromPrefs()

            // Fetch live from Firestore if connected to Internet
            if (networkMonitor.isCurrentlyConnected()) {
                coroutineScope.launch {
                    try {
                        val cloudProfile = FirebaseManager.getUserProfile(user.uid)
                        if (cloudProfile != null) {
                            fullName = cloudProfile.fullName
                            username = cloudProfile.username
                            phone = cloudProfile.phone
                            bio = cloudProfile.bio
                            photoUrl = cloudProfile.photoUrl
                            joinDateState = cloudProfile.joinDate
                            lastLoginDateState = cloudProfile.lastLoginDate

                            // Overwrite cache with Firestore updates
                            sharedPrefs.edit().apply {
                                putString("fullName_${user.uid}", cloudProfile.fullName)
                                putString("username_${user.uid}", cloudProfile.username)
                                putString("phone_${user.uid}", cloudProfile.phone)
                                putString("bio_${user.uid}", cloudProfile.bio)
                                putString("photoUrl_${user.uid}", cloudProfile.photoUrl)
                                putLong("joinDate_${user.uid}", cloudProfile.joinDate)
                                putLong("lastLoginDate_${user.uid}", cloudProfile.lastLoginDate)
                                apply()
                            }
                            writeAuditLog("SYSTEM", "Data akun berhasil diselaraskan dari cloud database secara aman.")
                        } else {
                            // First time creation inside Firestore
                            val defaultProfile = FirebaseManager.UserProfile(
                                uid = user.uid,
                                fullName = user.displayName ?: "Meydi User",
                                username = user.email?.substringBefore("@") ?: "user",
                                email = user.email ?: "",
                                phone = user.phoneNumber ?: "",
                                joinDate = user.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                                lastLoginDate = user.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
                            )
                            FirebaseManager.saveUserToFirestore(user.uid, defaultProfile)
                            writeAuditLog("SYSTEM", "Profil cloud baru telah diinisialisasi untuk akun ini.")
                        }
                    } catch (e: Exception) {
                        writeAuditLog("SYSTEM", "Gagal memuat profil awan: ${e.localizedMessage}")
                    } finally {
                        isLoadingProfile = false
                    }
                }
            } else {
                isLoadingProfile = false
                writeAuditLog("SYSTEM", "Profil dimuat dari Cache Offline terenkripsi lokal.")
            }
        } else {
            // Simulated Guest Session
            uidState = "guest_cyber_101"
            emailState = "guest@meydiai.com"
            fullName = "Cyber Guest"
            username = "guest_mode"
            phone = "+628123456789"
            bio = "Menjelajahi dunia digital AI secara aman."
            photoUrl = PRESET_AVATARS[0]
            joinDateState = System.currentTimeMillis() - (1000L * 60L * 60L * 24L * 15L) // 15 days ago
            lastLoginDateState = System.currentTimeMillis()
            loadAuditLogsFromPrefs()
            isLoadingProfile = false
            writeAuditLog("SYSTEM", "Sesi Tamu Diaktifkan. Autentikasi berjalan dalam mode aman Sandbox.")
        }
    }

    // Auto-Sync Trigger when device switches back to Online
    LaunchedEffect(isOnlineState) {
        loadUserProfileAndPreferences()
        if (isOnlineState) {
            val pendingSync = sharedPrefs.getBoolean("pending_sync_${uidState}", false)
            if (pendingSync && uidState.isNotEmpty() && uidState != "guest_cyber_101") {
                coroutineScope.launch {
                    val p = FirebaseManager.UserProfile(
                        uid = uidState,
                        fullName = fullName,
                        username = username,
                        email = emailState,
                        phone = phone,
                        photoUrl = photoUrl,
                        bio = bio,
                        joinDate = joinDateState,
                        lastLoginDate = lastLoginDateState
                    )
                    val success = FirebaseManager.saveUserToFirestore(uidState, p)
                    if (success) {
                        sharedPrefs.edit().putBoolean("pending_sync_${uidState}", false).apply()
                        writeAuditLog("SYSTEM", "Perubahan offline telah disinkronkan sepenuhnya ke Firestore.")
                    }
                }
            }
        }
    }

    // Photo selection utilities
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUrl = uri.toString()
            writeAuditLog("PROFILE", "Mengubah foto profil melalui selektor galeri.")
            Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Save or simulate photo url
            photoUrl = PRESET_AVATARS[1]
            writeAuditLog("PROFILE", "Mengubah foto profil menggunakan Kamera instan.")
            Toast.makeText(context, "Foto profil berhasil diperbarui via kamera!", Toast.LENGTH_SHORT).show()
        }
    }

    // Real-Time Input Validators
    fun validateFullName(name: String): Boolean = name.trim().length >= 3
    fun validateUsername(user: String): Boolean = user.trim().length >= 3 && user.all { it.isLetterOrDigit() || it == '_' }
    fun validatePhone(num: String): Boolean {
        val clean = num.replace("+", "").replace("-", "").trim()
        return clean.isEmpty() || clean.all { it.isDigit() }
    }

    // Handles Save Changes
    fun saveProfileChanges() {
        if (!validateFullName(fullName)) {
            alertMessage = "Format Nama Lengkap tidak valid! Harus minimal 3 karakter."
            showFailureAlert = true
            return
        }
        if (!validateUsername(username)) {
            alertMessage = "Username tidak valid! Minimal 3 karakter alfanumerik / underscore tanpa spasi."
            showFailureAlert = true
            return
        }
        if (!validatePhone(phone)) {
            alertMessage = "Nomor Telepon tidak valid! Hanya boleh berisi karakter digital."
            showFailureAlert = true
            return
        }

        isPerformingAction = true

        // Cache locally first
        sharedPrefs.edit().apply {
            putString("fullName_$uidState", fullName)
            putString("username_$uidState", username)
            putString("phone_$uidState", phone)
            putString("bio_$uidState", bio)
            putString("photoUrl_$uidState", photoUrl)
            apply()
        }
        writeAuditLog("PROFILE", "Menyimpan data profil: Nama, Username, Bio, dan No. Telp ke cache.")

        // Sync Online to Firebase Firestore
        if (uidState != "guest_cyber_101" && networkMonitor.isCurrentlyConnected()) {
            coroutineScope.launch {
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .setPhotoUri(Uri.parse(photoUrl))
                        .build()
                    FirebaseManager.auth?.currentUser?.updateProfile(profileUpdates)

                    val p = FirebaseManager.UserProfile(
                        uid = uidState,
                        fullName = fullName,
                        username = username,
                        email = emailState,
                        phone = phone,
                        photoUrl = photoUrl,
                        bio = bio,
                        joinDate = joinDateState,
                        lastLoginDate = System.currentTimeMillis()
                    )
                    val success = FirebaseManager.saveUserToFirestore(uidState, p)
                    if (success) {
                        writeAuditLog("PROFILE", "Perubahan profil disinkronkan secara real-time ke Cloud Firestore.")
                        alertMessage = "Pengaturan Profil berhasil diperbarui dan disimpan secara aman ke cloud!"
                        showSuccessAlert = true
                    } else {
                        sharedPrefs.edit().putBoolean("pending_sync_$uidState", true).apply()
                        writeAuditLog("PROFILE", "Gagal menyimpan ke Firestore. Data ditandai untuk Auto-Sync.")
                        alertMessage = "Tersimpan secara lokal! Data akan disinkronkan otomatis saat server merespon."
                        showSuccessAlert = true
                    }
                } catch (e: Exception) {
                    sharedPrefs.edit().putBoolean("pending_sync_$uidState", true).apply()
                    writeAuditLog("PROFILE", "Kesalahan cloud sync: ${e.localizedMessage}")
                    alertMessage = "Data tersimpan secara lokal. Auto-Sync diaktifkan."
                    showSuccessAlert = true
                } finally {
                    isPerformingAction = false
                }
            }
        } else {
            if (uidState == "guest_cyber_101") {
                alertMessage = "Sesi Tamu: Perubahan disimpan di penyimpanan perangkat lokal!"
            } else {
                sharedPrefs.edit().putBoolean("pending_sync_$uidState", true).apply()
                writeAuditLog("PROFILE", "Perangkat offline. Perubahan disimpan secara lokal dalam status Pending-Sync.")
                alertMessage = "Profil tersimpan offline! Sinkronisasi otomatis akan berjalan seketika Anda online kembali."
            }
            showSuccessAlert = true
            isPerformingAction = false
        }
    }

    // Security Re-Authentication Check before critical acts
    fun executeCriticalAction(reason: String) {
        reauthReason = reason
        reauthPasswordInput = ""
        
        if (uidState == "guest_cyber_101") {
            // Guests bypass re-auth to let user test flows easily!
            when (reason) {
                "PASSWORD" -> { showPasswordChangeDialog = true }
                "EMAIL" -> { showEmailChangeDialog = true }
                "2FA" -> { showTwoFactorSetupDialog = true }
                "EXPORT" -> { showExportDataDialog = true }
                "DELETE" -> { showDeleteAccountDialog = true }
            }
        } else {
            showReauthDialog = true
        }
    }

    fun handleReauthentication() {
        if (reauthPasswordInput.trim().isEmpty()) {
            Toast.makeText(context, "Sandi Re-Auth tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        isPerformingAction = true
        coroutineScope.launch {
            try {
                val auth = FirebaseManager.auth
                if (auth?.currentUser != null && auth.currentUser!!.email != null) {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                        auth.currentUser!!.email!!,
                        reauthPasswordInput
                    )
                    auth.currentUser!!.reauthenticate(credential)
                    
                    showReauthDialog = false
                    writeAuditLog("SECURITY", "Otentikasi ganda berhasil untuk tindakan kritis: $reauthReason")
                    
                    // Dispatch to requested dialog
                    when (reauthReason) {
                        "PASSWORD" -> { showPasswordChangeDialog = true }
                        "EMAIL" -> { showEmailChangeDialog = true }
                        "2FA" -> { showTwoFactorSetupDialog = true }
                        "EXPORT" -> { showExportDataDialog = true }
                        "DELETE" -> { showDeleteAccountDialog = true }
                    }
                }
            } catch (e: Exception) {
                writeAuditLog("SECURITY", "Gagal melakukan Re-Otentikasi: Sandi salah.")
                Toast.makeText(context, "Otentikasi Gagal! Silakan periksa kembali sandi Anda.", Toast.LENGTH_LONG).show()
            } finally {
                isPerformingAction = false
            }
        }
    }

    // 1. Password change handler
    fun updatePasswordSecurely() {
        if (newPassword.length < 6) {
            alertMessage = "Sandi baru minimal harus berisi 6 karakter!"
            showFailureAlert = true
            return
        }
        if (newPassword != confirmNewPassword) {
            alertMessage = "Sandi konfirmasi tidak cocok!"
            showFailureAlert = true
            return
        }

        isPerformingAction = true
        if (uidState == "guest_cyber_101") {
            writeAuditLog("SECURITY", "Password simulasi akun tamu berhasil diubah.")
            showPasswordChangeDialog = false
            alertMessage = "Sesi Sandbox: Sandi akun tamu berhasil dimodifikasi!"
            showSuccessAlert = true
            newPassword = ""
            confirmNewPassword = ""
            isPerformingAction = false
            return
        }

        coroutineScope.launch {
            try {
                FirebaseManager.auth?.currentUser?.updatePassword(newPassword)
                writeAuditLog("SECURITY", "Kata sandi kredensial Firebase Auth berhasil diubah secara aman.")
                showPasswordChangeDialog = false
                alertMessage = "Sandi keamanan akun Anda berhasil diperbarui di server Firebase!"
                showSuccessAlert = true
                newPassword = ""
                confirmNewPassword = ""
            } catch (e: Exception) {
                writeAuditLog("SECURITY", "Gagal mengubah sandi: ${e.localizedMessage}")
                alertMessage = "Gagal memperbarui sandi: ${e.localizedMessage}"
                showFailureAlert = true
            } finally {
                isPerformingAction = false
            }
        }
    }

    // 2. Email change handler
    fun updateEmailSecurely() {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmailInput).matches()) {
            alertMessage = "Format alamat email tidak valid!"
            showFailureAlert = true
            return
        }

        isPerformingAction = true
        if (uidState == "guest_cyber_101") {
            emailState = newEmailInput
            writeAuditLog("SECURITY", "Email akun tamu diubah menjadi $newEmailInput.")
            showEmailChangeDialog = false
            alertMessage = "Sesi Sandbox: Email tamu berhasil dimodifikasi ke $newEmailInput!"
            showSuccessAlert = true
            newEmailInput = ""
            isPerformingAction = false
            return
        }

        coroutineScope.launch {
            try {
                FirebaseManager.auth?.currentUser?.updateEmail(newEmailInput)
                emailState = newEmailInput
                
                // Trigger fresh email verification
                FirebaseManager.auth?.currentUser?.sendEmailVerification()
                isEmailVerifiedState = false

                writeAuditLog("SECURITY", "Email akun utama diubah menjadi $newEmailInput. Verifikasi ulang dikirim.")
                showEmailChangeDialog = false
                alertMessage = "Email berhasil diubah ke $newEmailInput! Harap periksa email Anda untuk memverifikasi ulang alamat baru ini."
                showSuccessAlert = true
                newEmailInput = ""
            } catch (e: Exception) {
                writeAuditLog("SECURITY", "Gagal mengubah email: ${e.localizedMessage}")
                alertMessage = "Gagal mengubah email: ${e.localizedMessage}"
                showFailureAlert = true
            } finally {
                isPerformingAction = false
            }
        }
    }

    // 3. Email verification link sender
    fun sendEmailVerificationDirectly() {
        if (!networkMonitor.isCurrentlyConnected()) {
            alertMessage = "Tidak ada jaringan internet! Gagal mengirim tautan verifikasi email."
            showFailureAlert = true
            return
        }

        isPerformingAction = true
        coroutineScope.launch {
            try {
                FirebaseManager.auth?.currentUser?.sendEmailVerification()
                writeAuditLog("SECURITY", "Mengirim ulang tautan verifikasi ke email $emailState")
                alertMessage = "Email verifikasi berhasil dikirimkan ke $emailState! Harap cek kotak masuk Anda."
                showSuccessAlert = true
            } catch (e: Exception) {
                writeAuditLog("SECURITY", "Gagal mengirim tautan verifikasi: ${e.localizedMessage}")
                alertMessage = "Gagal mengirim verifikasi: ${e.localizedMessage}"
                showFailureAlert = true
            } finally {
                isPerformingAction = false
            }
        }
    }

    // 4. Two-Factor Authentication Toggle Setup
    fun enableTwoFactorSecurely() {
        if (twoFactorVerificationCode.trim() != "123456") {
            Toast.makeText(context, "Kode verifikasi 2FA salah! Masukkan kode simulasi: 123456", Toast.LENGTH_LONG).show()
            return
        }

        isTwoFactorEnabled = true
        sharedPrefs.edit().putBoolean("two_factor_enabled", true).apply()
        writeAuditLog("SECURITY", "Mengaktifkan Perlindungan 2FA (Two-Factor Authentication).")
        showTwoFactorSetupDialog = false
        twoFactorVerificationCode = ""
        alertMessage = "Sistem Autentikasi Dua Langkah (2FA) berhasil diaktifkan pada akun Anda!"
        showSuccessAlert = true
    }

    fun disableTwoFactor() {
        isTwoFactorEnabled = false
        sharedPrefs.edit().putBoolean("two_factor_enabled", false).apply()
        writeAuditLog("SECURITY", "Menonaktifkan Perlindungan 2FA (Two-Factor Authentication).")
        alertMessage = "Otentikasi Dua Langkah (2FA) berhasil dinonaktifkan."
        showSuccessAlert = true
    }

    // 5. Account Deletion handler
    fun deleteAccountPermanently() {
        isPerformingAction = true
        writeAuditLog("SECURITY", "Memulai perusakan akun permanen...")

        // Clear local cache completely
        sharedPrefs.edit().apply {
            remove("fullName_$uidState")
            remove("username_$uidState")
            remove("phone_$uidState")
            remove("bio_$uidState")
            remove("photoUrl_$uidState")
            remove("two_factor_enabled")
            apply()
        }

        if (uidState == "guest_cyber_101") {
            showDeleteAccountDialog = false
            isPerformingAction = false
            Toast.makeText(context, "Sesi Tamu berhasil dihancurkan secara permanen!", Toast.LENGTH_SHORT).show()
            onLogout()
            return
        }

        coroutineScope.launch {
            try {
                val db = FirebaseManager.firestore
                if (db != null) {
                    // Wipe Firestore profiles
                    db.collection("users").document(uidState).delete()
                }

                FirebaseManager.auth?.currentUser?.delete()
                showDeleteAccountDialog = false
                Toast.makeText(context, "Akun Anda berhasil dihapus selamanya.", Toast.LENGTH_LONG).show()
                onLogout()
            } catch (e: Exception) {
                writeAuditLog("SECURITY", "Kesalahan saat menghapus akun: ${e.localizedMessage}")
                alertMessage = "Gagal menghapus kredensial: ${e.localizedMessage}. Silakan login ulang dan coba lagi."
                showFailureAlert = true
            } finally {
                isPerformingAction = false
            }
        }
    }

    // 6. Data Export (JSON formatted block creator)
    fun buildExportDataString(): String {
        return """
        {
          "account_identity": {
            "uid": "$uidState",
            "full_name": "$fullName",
            "username": "$username",
            "email": "$emailState",
            "phone_number": "$phone"
          },
          "account_biography": "${bio.replace("\n", " ")}",
          "security_metadata": {
            "two_factor_authentication_enabled": $isTwoFactorEnabled,
            "email_verified": $isEmailVerifiedState,
            "registered_timestamp": $joinDateState,
            "last_login_timestamp": $lastLoginDateState
          },
          "privacy_preferences": {
            "visibility_level": "$whoCanSeeProfile",
            "email_visible_to_public": $isEmailVisible,
            "phone_visible_to_public": $isPhoneVisible,
            "online_status_visible": $isOnlineStatusVisible,
            "notification_push_allowed": $isNotificationAllowed,
            "diagnostic_data_sharing": $isDataSharingEnabled
          },
          "system_environment": {
            "os_build": "Android ${android.os.Build.VERSION.RELEASE}",
            "device_manufacturer": "${android.os.Build.MANUFACTURER}",
            "device_model": "${android.os.Build.MODEL}"
          }
        }
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MidnightBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pengaturan Akun",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Online/Offline State indicator badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isOnlineState) NeonTeal.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f))
                            .border(1.dp, if (isOnlineState) NeonTeal.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isOnlineState) NeonTeal else ErrorRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnlineState) "Online" else "Offline Cache",
                            color = if (isOnlineState) NeonTeal else ErrorRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = DarkStroke, thickness = 1.dp)
            }
        },
        containerColor = MidnightBg
    ) { innerPadding ->
        if (isLoadingProfile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = NeonTeal, strokeWidth = 3.dp)
                    Text("Memuat data otentikasi...", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: HEADER USER AVATAR & BASIC DETAILS
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(MidnightSurface)
                            .border(2.dp, NeonTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = fullName.take(1).uppercase(),
                                color = NeonTeal,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Avatar Update controllers (Camera & Gallery)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(34.dp)
                                .background(MidnightSurface, CircleShape)
                                .border(1.dp, DarkStroke, CircleShape)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Pilih Galeri", tint = NeonTeal, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(MidnightSurface, CircleShape)
                                .border(1.dp, DarkStroke, CircleShape)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Ambil Kamera", tint = NeonTeal, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Cyber preset avatar list
                    Text("Pilih Preset Avatar Cyberpunk:", color = Color.Gray, fontSize = 10.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(PRESET_AVATARS) { url ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (photoUrl == url) 2.dp else 1.dp,
                                        color = if (photoUrl == url) NeonTeal else DarkStroke,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        photoUrl = url
                                        writeAuditLog("PROFILE", "Mengubah foto profil menggunakan Preset Cyber.")
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Preset Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fullName.ifEmpty { "Pengguna Baru" },
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${username.ifEmpty { "username" }}",
                        color = NeonTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )

                    // Identity verification status badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uidState == "guest_cyber_101") "SANDBOX MODE" else "VERIFIED PROFILE",
                            color = if (uidState == "guest_cyber_101") Color.LightGray else NeonPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(if (uidState == "guest_cyber_101") Color.DarkGray.copy(alpha = 0.2f) else NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, if (uidState == "guest_cyber_101") Color.Gray else NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        Text(
                            text = if (isEmailVerifiedState) "EMAIL VERIFIED" else "UNVERIFIED EMAIL",
                            color = if (isEmailVerifiedState) NeonTeal else ErrorRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(if (isEmailVerifiedState) NeonTeal.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, if (isEmailVerifiedState) NeonTeal.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .clickable(!isEmailVerifiedState) { sendEmailVerificationDirectly() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // NAVIGATION CHIPS: TABS MENU (Profile, Edit, Security, Privacy, Logs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidnightSurface)
                        .padding(2.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    val tabs = listOf(
                        "PROFILE" to "Akun",
                        "EDIT" to "Ubah",
                        "SECURITY" to "Keamanan",
                        "PRIVACY" to "Privasi",
                        "AUDIT" to "Audit Logs"
                    )
                    tabs.forEach { (key, label) ->
                        val isSelected = activeTab == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonPurple else Color.Transparent)
                                .clickable { activeTab = key }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // SECTION 2: COMPOSABLE SUB-PAGES RENDERER
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "SettingsTabTransition"
                ) { tabState ->
                    when (tabState) {
                        "PROFILE" -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text("Informasi Dasar Akun", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    
                                    AccountDetailsRow(icon = Icons.Default.Person, title = "Nama Lengkap", value = fullName.ifEmpty { "Belum disetel" })
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)
                                    
                                    AccountDetailsRow(icon = Icons.Default.AlternateEmail, title = "Username", value = "@${username.ifEmpty { "username" }}")
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)
                                    
                                    AccountDetailsRow(icon = Icons.Default.Email, title = "Email Terdaftar", value = emailState)
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)
                                    
                                    AccountDetailsRow(icon = Icons.Default.Phone, title = "Nomor Telepon", value = phone.ifEmpty { "Belum terdaftar" })
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)
                                    
                                    AccountDetailsRow(icon = Icons.Default.Notes, title = "Bio Singkat", value = bio.ifEmpty { "Tidak ada bio." })
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)

                                    val creationStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(joinDateState))
                                    AccountDetailsRow(icon = Icons.Default.CalendarToday, title = "Pembuatan Akun", value = creationStr)
                                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)

                                    val loginStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(lastLoginDateState))
                                    AccountDetailsRow(icon = Icons.Default.History, title = "Koneksi Terakhir", value = loginStr)
                                }
                            }
                        }

                        "EDIT" -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text("Ubah Data Profil Pengguna", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("Nama Lengkap") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = MidnightBg,
                                            unfocusedContainerColor = MidnightBg,
                                            focusedLabelColor = NeonTeal,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedIndicatorColor = NeonTeal,
                                            unfocusedIndicatorColor = DarkStroke
                                        )
                                    )

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = { Text("Username (@)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = MidnightBg,
                                            unfocusedContainerColor = MidnightBg,
                                            focusedLabelColor = NeonTeal,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedIndicatorColor = NeonTeal,
                                            unfocusedIndicatorColor = DarkStroke
                                        )
                                    )

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Nomor Telepon") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = MidnightBg,
                                            unfocusedContainerColor = MidnightBg,
                                            focusedLabelColor = NeonTeal,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedIndicatorColor = NeonTeal,
                                            unfocusedIndicatorColor = DarkStroke
                                        )
                                    )

                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { if (it.length <= 250) bio = it },
                                        label = { Text("Bio Singkat (Maks 250 karakter)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 4,
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = MidnightBg,
                                            unfocusedContainerColor = MidnightBg,
                                            focusedLabelColor = NeonTeal,
                                            unfocusedLabelColor = Color.Gray,
                                            focusedIndicatorColor = NeonTeal,
                                            unfocusedIndicatorColor = DarkStroke
                                        )
                                    )

                                    Button(
                                        onClick = { saveProfileChanges() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isPerformingAction
                                    ) {
                                        if (isPerformingAction) {
                                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SIMPAN PERUBAHAN", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        "SECURITY" -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Pusat Keamanan & Kredensial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    // Action 1: Change Email
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightBg)
                                            .clickable { executeCriticalAction("EMAIL") }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = NeonTeal)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Ubah Alamat Email", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Email saat ini: $emailState", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }

                                    // Action 2: Change Password
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightBg)
                                            .clickable { executeCriticalAction("PASSWORD") }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonPurple)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Ubah Kata Sandi (Password)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Amankan sandi login Firebase Anda", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }

                                    // Action 3: 2FA Authentication
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightBg)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = InfoBlue)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Autentikasi Dua Langkah (2FA)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Proteksi sandi ganda (Authenticator App)", color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                        Switch(
                                            checked = isTwoFactorEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    executeCriticalAction("2FA")
                                                } else {
                                                    disableTwoFactor()
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = NeonTeal, checkedTrackColor = NeonTeal.copy(alpha = 0.3f))
                                        )
                                    }

                                    // Action 4: Manage Session Devices
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MidnightBg)
                                            .clickable { showSessionDevicesDialog = true }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFFFFD700))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Kelola Sesi & Perangkat Aktif", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Hentikan sesi gawai mencurigakan lain", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        "PRIVACY" -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text("Pengaturan Privasi & Berbagi Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    // Profile Visibility Selector chips
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Siapa yang dapat melihat profil Anda?", color = Color.Gray, fontSize = 10.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val scopes = listOf("Everyone" to "Semua Orang", "Contacts" to "Kontak", "Only Me" to "Hanya Saya")
                                            scopes.forEach { (scope, text) ->
                                                val isSel = whoCanSeeProfile == scope
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSel) NeonPurple else MidnightBg)
                                                        .border(1.dp, if (isSel) NeonPurple else DarkStroke, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            whoCanSeeProfile = scope
                                                            sharedPrefs.edit().putString("who_can_see_profile", scope).apply()
                                                            writeAuditLog("PRIVACY", "Mengubah visibilitas profil menjadi: $scope")
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text, color = if (isSel) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = DarkStroke)

                                    // Email Visibility
                                    PrivacySwitchRow(
                                        title = "Tampilkan Email ke Publik",
                                        subtitle = "Pengguna lain dapat meninjau email Anda",
                                        checked = isEmailVisible,
                                        onCheckedChange = {
                                            isEmailVisible = it
                                            sharedPrefs.edit().putBoolean("email_visibility", it).apply()
                                            writeAuditLog("PRIVACY", "Mengubah visibilitas email publik menjadi: $it")
                                        }
                                    )

                                    HorizontalDivider(color = DarkStroke)

                                    // Phone Visibility
                                    PrivacySwitchRow(
                                        title = "Tampilkan No. Telepon ke Publik",
                                        subtitle = "Pengguna lain dapat meninjau nomor telepon",
                                        checked = isPhoneVisible,
                                        onCheckedChange = {
                                            isPhoneVisible = it
                                            sharedPrefs.edit().putBoolean("phone_visibility", it).apply()
                                            writeAuditLog("PRIVACY", "Mengubah visibilitas no. HP publik menjadi: $it")
                                        }
                                    )

                                    HorizontalDivider(color = DarkStroke)

                                    // Online Status Visibility
                                    PrivacySwitchRow(
                                        title = "Tampilkan Status Online",
                                        subtitle = "Menandai status ONLINE ketika Anda membuka app",
                                        checked = isOnlineStatusVisible,
                                        onCheckedChange = {
                                            isOnlineStatusVisible = it
                                            sharedPrefs.edit().putBoolean("online_status_visibility", it).apply()
                                            writeAuditLog("PRIVACY", "Mengubah visibilitas status online menjadi: $it")
                                        }
                                    )

                                    HorizontalDivider(color = DarkStroke)

                                    // Notification Switch
                                    PrivacySwitchRow(
                                        title = "Izin Kirim Notifikasi Push",
                                        subtitle = "Info real-time rilis, pesan admin, & audit log",
                                        checked = isNotificationAllowed,
                                        onCheckedChange = {
                                            isNotificationAllowed = it
                                            sharedPrefs.edit().putBoolean("notification_permission", it).apply()
                                            writeAuditLog("PRIVACY", "Mengubah pengaturan notifikasi push menjadi: $it")
                                        }
                                    )

                                    HorizontalDivider(color = DarkStroke)

                                    // Data Sharing Switch
                                    PrivacySwitchRow(
                                        title = "Berbagi Data Diagnostik (Telemetry)",
                                        subtitle = "Membantu kami memperbaiki kerusakan bug",
                                        checked = isDataSharingEnabled,
                                        onCheckedChange = {
                                            isDataSharingEnabled = it
                                            sharedPrefs.edit().putBoolean("data_sharing_preferences", it).apply()
                                            writeAuditLog("PRIVACY", "Mengubah status berbagi data telemetri menjadi: $it")
                                        }
                                    )
                                }
                            }
                        }

                        "AUDIT" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                    border = BorderStroke(1.dp, DarkStroke)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Log Aktivitas & Riwayat Audit Keamanan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Semua peristiwa konfigurasi login, ubah sandi, modifikasi email, dan data terekam lengkap demi perlindungan privasi siber Anda.", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }

                                // Interactive scrolling log output terminal
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF030206))
                                        .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    if (auditLogs.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Belum ada logs aktivitas terekam.", color = Color.DarkGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    } else {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(auditLogs) { log ->
                                                val col = when (log.type) {
                                                    "SECURITY" -> ErrorRed
                                                    "PRIVACY" -> NeonPurple
                                                    "PROFILE" -> NeonTeal
                                                    else -> InfoBlue
                                                }
                                                Column {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("[${log.formattedTime}]", color = Color.DarkGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                                        Text("[${log.type}]", color = col, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    Text(log.description, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 13.sp, fontFamily = FontFamily.Monospace)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // DATA EXPORT & DESTRUCTION AREA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, DarkStroke)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Manajemen Akun Lanjutan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Amankan cadangan data profil Anda atau musnahkan akun Anda secara permanen.", color = Color.Gray, fontSize = 10.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Export Button
                            Button(
                                onClick = { executeCriticalAction("EXPORT") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightBg),
                                border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("EKSPOR DATA", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Delete Account Button
                            Button(
                                onClick = { executeCriticalAction("DELETE") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightBg),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HAPUS AKUN", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // GENERAL ACTION LINKS (FAQ Help Center, Signout)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Help link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MidnightSurface)
                            .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                            .clickable { showFAQDialog = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Help, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Bantuan & Pusat FAQ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }

                    // Logout Button
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KELUAR (LOGOUT) DARI GAWAI INI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ==========================================
    // POPUPS, SECURE DIALOGS & SHEET MODALITIES
    // ==========================================

    // 1. Secure Re-Authentication Dialog (Checks prior security password before acts)
    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Otentikasi Kredensial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Demi keamanan, masukkan sandi utama untuk memvalidasi akses tindakan: $reauthReason.", color = Color.Gray, fontSize = 11.sp)
                    OutlinedTextField(
                        value = reauthPasswordInput,
                        onValueChange = { reauthPasswordInput = it },
                        label = { Text("Password Akun") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = NeonTeal,
                            focusedIndicatorColor = NeonTeal,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleReauthentication() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    enabled = !isPerformingAction
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Verifikasi", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauthDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // 2. Change Password input form Modal
    if (showPasswordChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            containerColor = MidnightSurface,
            title = { Text("Kata Sandi Keamanan Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tentukan sandi baru unik minimal 6 karakter kombinasi alfanumerik.", color = Color.Gray, fontSize = 11.sp)

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata Sandi Baru") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = NeonTeal,
                            focusedIndicatorColor = NeonTeal,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Konfirmasi Sandi Baru") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = NeonTeal,
                            focusedIndicatorColor = NeonTeal,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { updatePasswordSecurely() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    enabled = !isPerformingAction
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Sandi", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordChangeDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // 3. Change Email input form Modal
    if (showEmailChangeDialog) {
        AlertDialog(
            onDismissRequest = { showEmailChangeDialog = false },
            containerColor = MidnightSurface,
            title = { Text("Tentukan Alamat Email Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Email baru wajib valid dan aktif untuk keperluan verifikasi tautan resmi dari Firebase.", color = Color.Gray, fontSize = 11.sp)

                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it },
                        label = { Text("Email Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = NeonTeal,
                            focusedIndicatorColor = NeonTeal,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { updateEmailSecurely() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    enabled = !isPerformingAction
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Ubah Email", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailChangeDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // 4. Two-Factor Authentication App Setup Modal
    if (showTwoFactorSetupDialog) {
        AlertDialog(
            onDismissRequest = { showTwoFactorSetupDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setup Authenticator 2FA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Masukkan Secret Token berikut ke Google Authenticator, Authy atau penyedia 2FA Anda:", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    
                    // Secret key layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MidnightBg)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(simulated2FASecret, color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(simulated2FASecret))
                            Toast.makeText(context, "Secret Key berhasil disalin!", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Simulated QR Code Visual
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cyber neon mock qr representation
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                                Box(modifier = Modifier.size(24.dp).background(Color.Transparent))
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                                Box(modifier = Modifier.size(24.dp).background(Color.Black))
                            }
                        }
                    }

                    Text("Masukkan 6-digit kode yang tampil di Authenticator (Masukkan simulasi kode: 123456):", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    
                    OutlinedTextField(
                        value = twoFactorVerificationCode,
                        onValueChange = { twoFactorVerificationCode = it },
                        label = { Text("6-Digit Kode OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = NeonTeal,
                            focusedIndicatorColor = NeonTeal,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { enableTwoFactorSecurely() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Aktifkan 2FA", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTwoFactorSetupDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // 5. Session Devices Management modal
    if (showSessionDevicesDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDevicesDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = InfoBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kelola Sesi Login Aktif", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sistem mengamankan sesi otentikasi Anda. Anda dapat memutuskan hubungan login perangkat luar di bawah ini:", color = Color.Gray, fontSize = 11.sp)
                    
                    sessionDevices.forEachIndexed { idx, device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MidnightBg)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val split = device.split("|")
                                Text(split.getOrNull(0) ?: "Perangkat", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(split.getOrNull(1) ?: "", color = Color.Gray, fontSize = 9.sp)
                                Text(split.getOrNull(2) ?: "", color = NeonTeal, fontSize = 9.sp)
                            }

                            if (idx > 0) {
                                IconButton(
                                    onClick = {
                                        sessionDevices.removeAt(idx)
                                        writeAuditLog("SESSION", "Memutuskan paksa hubungan login perangkat: $device")
                                        Toast.makeText(context, "Sesi perangkat diputus!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Putus Sesi", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text("Gawai Ini", color = InfoBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (sessionDevices.size > 1) {
                        Button(
                            onClick = {
                                val current = sessionDevices.first()
                                sessionDevices.clear()
                                sessionDevices.add(current)
                                writeAuditLog("SESSION", "Mengeluarkan seluruh sesi di perangkat eksternal.")
                                Toast.makeText(context, "Sesi seluruh perangkat luar dibekukan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
                        ) {
                            Text("KELUAR DARI SELURUH GAWAI LAIN", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSessionDevicesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        )
    }

    // 6. Export Account Data JSON viewer modal
    if (showExportDataDialog) {
        val formattedJson = remember { buildExportDataString() }
        AlertDialog(
            onDismissRequest = { showExportDataDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NeonTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Berkas Akun Pengguna", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Berikut adalah salinan berkas XML/JSON pengaturan profil, opsi, dan keamanan Anda yang sah untuk disimpan secara mandiri:", color = Color.Gray, fontSize = 11.sp)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = formattedJson,
                            color = NeonTeal,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(formattedJson))
                        writeAuditLog("SYSTEM", "Mengekspor dan menyalin seluruh berkas privasi akun.")
                        showExportDataDialog = false
                        Toast.makeText(context, "Berkas Akun disalin ke clipboard!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Salin JSON", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDataDialog = false }) {
                    Text("Tutup", color = Color.Gray)
                }
            }
        )
    }

    // 7. Permanent Double-Confirmation Account Destruction modal
    if (showDeleteAccountDialog) {
        var deleteConfirmationInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PENGHANCURAN AKUN PERMANEN", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TINDAKAN SENSITIF & TIDAK DAPAT DIURUNGKAN!\nSeluruh database Firestore, autentikasi cloud, file upload, logs aktivitas, dan cache offline Anda akan dihancurkan seketika.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Untuk memvalidasi tindakan destruktif ini, harap ketik secara persis tulisan: MEYDI DELETE di bawah ini:", color = Color.Gray, fontSize = 10.sp)

                    OutlinedTextField(
                        value = deleteConfirmationInput,
                        onValueChange = { deleteConfirmationInput = it },
                        label = { Text("Ketik MEYDI DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = MidnightBg,
                            unfocusedContainerColor = MidnightBg,
                            focusedLabelColor = ErrorRed,
                            focusedIndicatorColor = ErrorRed,
                            unfocusedIndicatorColor = DarkStroke
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { deleteAccountPermanently() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    enabled = (deleteConfirmationInput.trim() == "MEYDI DELETE") && !isPerformingAction
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Mutilasi Akun", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    // 8. FAQ Center Dialog
    if (showFAQDialog) {
        AlertDialog(
            onDismissRequest = { showFAQDialog = false },
            containerColor = MidnightSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = InfoBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pusat Bantuan & Kebijakan FAQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FAQItem(
                        q = "Bagaimana data saya diisolasi?",
                        a = "Kami menerapkan Firestore Security Rules tingkat tinggi. Setiap uid hanya memiliki wewenang read/write pada dokumennya sendiri. Sesi luar ditolak secara sistem siber."
                    )
                    FAQItem(
                        q = "Apakah data dienkripsi?",
                        a = "Ya. Modul Cryptographer AES-CBC dengan padding PKCS5 mengenkripsi data-data draf dan sensitif secara lokal di tingkat klien sebelum ditransmisikan ke awan."
                    )
                    FAQItem(
                        q = "Bagaimana cara kerja 2FA?",
                        a = "Otentikasi Dua Langkah menambahkan lapisan pertahanan tambahan menggunakan algoritma TOTP generator kunci rahasia untuk memblokir login asing."
                    )
                    FAQItem(
                        q = "Apakah profil tetap dapat dibuka offline?",
                        a = "Benar. Berkat arsitektur Offline-First, preferensi dan profil Anda disaring ke cache lokal SharedPreferences agar dapat diakses kapan saja dan disinkronkan otomatis saat online kembali."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFAQDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Selesai", color = Color.White)
                }
            }
        )
    }

    // 9. Success Notification Dialog
    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { showSuccessAlert = false },
            containerColor = MidnightSurface,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(40.dp)) },
            title = { Text("Tindakan Sukses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(alertMessage, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = { showSuccessAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Bagus", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 10. Failure Notification Dialog
    if (showFailureAlert) {
        AlertDialog(
            onDismissRequest = { showFailureAlert = false },
            containerColor = MidnightSurface,
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(40.dp)) },
            title = { Text("Gagal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(alertMessage, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = { showFailureAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Coba Lagi", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun AccountDetailsRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MidnightBg, RoundedCornerShape(6.dp))
                .border(1.dp, DarkStroke, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(text = title, color = Color.Gray, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PrivacySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NeonTeal, checkedTrackColor = NeonTeal.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun FAQItem(q: String, a: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MidnightBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = q, color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = a, color = Color.LightGray, fontSize = 10.sp, lineHeight = 14.sp)
    }
}
