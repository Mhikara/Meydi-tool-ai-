package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// --- MODEL ---
data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isForceUpdate: Boolean,
    val changelog: String,
    val downloadUrl: String
)

// --- LOGIC ---
object AppUpdateChecker {
    private const val CACHE_KEY = "last_update_check_time"
    private const val CACHE_DURATION_MS = 1000 * 60 * 60 // 1 Jam Cache

    suspend fun checkForUpdate(context: Context, currentVersion: String = ""): AppUpdateInfo? {
        val resolvedVersion = if (currentVersion.isNotBlank()) {
            currentVersion
        } else {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "1.1.0"
            } catch (e: Exception) {
                "1.1.0"
            }
        }
        val prefs = context.getSharedPreferences("meydiai_updater", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(CACHE_KEY, 0)
        val now = System.currentTimeMillis()

        // 1. Cek Koneksi Internet (Mencegah crash jika offline)
        if (!isInternetAvailable(context)) {
            return null // Gagal senyap jika tidak ada internet
        }

        // 2. Sistem Cache (Mencegah spam request ke server)
        // Hapus komentar di bawah untuk mengaktifkan cache secara penuh di production
        /*
        if (now - lastCheck < CACHE_DURATION_MS) {
            return null 
        }
        */

        prefs.edit().putLong(CACHE_KEY, now).apply()

        // 3. Simulasi Fetch ke Firebase Remote Config / Realtime Database
        // Di aplikasi nyata, gunakan FirebaseRemoteConfig.getInstance().fetchAndActivate()
        delay(1500) // Simulasi network delay
        
        val mockLatestVersion = "1.1.0"
        val isForceUpdate = true
        val changelog = "🚀 Fitur Baru:\n• AI Clipper (Auto Cut Video)\n• Keamanan Biometrik Level Tinggi\n• Peningkatan Performa Server\n\n🐛 Bug Fixes:\n• Perbaikan UI pada mode gelap"
        val downloadUrl = "https://play.google.com/store/apps/details?id=com.example.meydiai" // Ganti URL APK/PlayStore

        // 4. Bandingkan Versi
        if (isVersionGreater(mockLatestVersion, resolvedVersion)) {
            return AppUpdateInfo(
                currentVersion = resolvedVersion,
                latestVersion = mockLatestVersion,
                isForceUpdate = isForceUpdate,
                changelog = changelog,
                downloadUrl = downloadUrl
            )
        }
        return null
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        val lParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(lParts.size, cParts.size)) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// --- UI COMPONENT (MODERN DIALOG) ---
@Composable
fun AutoUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { 
            // Jika force update, tidak bisa ditutup sembarangan
            if (!updateInfo.isForceUpdate) onDismiss() 
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B0F19), // Match the app's dark theme
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update Icon",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(56.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Update Terbaru Tersedia!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Versi Anda: ${updateInfo.currentVersion}", color = Color.Gray, fontSize = 12.sp)
                    Text("Versi Baru: ${updateInfo.latestVersion}", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Yang Baru:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(updateInfo.changelog, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                
                if (updateInfo.isForceUpdate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Update ini bersifat wajib untuk melanjutkan penggunaan aplikasi.",
                        color = Color(0xFFFF5722),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback jika tidak ada browser
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                ) {
                    Text("Update Sekarang", color = Color(0xFF060D1E), fontWeight = FontWeight.Bold)
                }
                
                if (!updateInfo.isForceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nanti Saja", color = Color.Gray)
                    }
                }
            }
        }
    }
}
