package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.*
import com.example.security.NetworkInsecurityException

// Modern Cyberpunk colors
private val NeonTeal = Color(0xFF00FFCC)
private val NeonPurple = Color(0xFF7F00FF)
private val DarkBg = Color(0xFF06040C)
private val SurfaceBg = Color(0xFF12101F)
private val BorderStrokeCol = Color(0xFF231F3A)
private val RedAlert = Color(0xFFFF3366)
private val TerminalGreen = Color(0xFF00FF66)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // KeyStore & Encryption state values
    var isKeyExists by remember { mutableStateOf(KeyStoreManager.keyExists()) }
    var securityLogs by remember { mutableStateOf(EncryptionManager.getSecurityLogs()) }
    
    // Cryptography Sandbox states
    var sandboxPlainText by remember { mutableStateOf("Meydi AI Token Kredensial Rahasia 2026") }
    var sandboxCipherText by remember { mutableStateOf("") }
    var sandboxDecryptedText by remember { mutableStateOf("") }
    var sandboxIVHex by remember { mutableStateOf("") }

    // Secure Storage Inspector states
    var secureStorageList by remember { mutableStateOf(emptyMap<String, String>()) }
    var rawStorageList by remember { mutableStateOf(emptyMap<String, String>()) }
    var viewDecryptedValues by remember { mutableStateOf(false) }
    
    // New Key-Value input form
    var customPrefKey by remember { mutableStateOf("") }
    var customPrefValue by remember { mutableStateOf("") }

    // Network TLS state values
    var testUrlInput by remember { mutableStateOf("https://api.meydiai.com/v1/auth") }
    var isUrlSecureState by remember { mutableStateOf(NetworkSecurity.isConnectionSecure(testUrlInput)) }
    var networkTestResult by remember { mutableStateOf("") }

    // Initialize/Refresh storage viewer
    fun refreshStorageData() {
        val storage = EncryptionManager.getSecureStorageInstance()
        secureStorageList = storage.getAllDecrypted()
        rawStorageList = storage.getAllRaw()
        securityLogs = EncryptionManager.getSecurityLogs()
        isKeyExists = KeyStoreManager.keyExists()
    }

    // Run once on enter
    LaunchedEffect(Unit) {
        // Seed some sample data inside encrypted SharedPreferences if empty
        val storage = EncryptionManager.getSecureStorageInstance()
        if (storage.getString("session_token") == null) {
            storage.putString("session_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiTWV5ZGkgSGlrYXJhIn0")
            storage.putString("api_token", "meydi_sk_live_2026_x9p2z")
            storage.putString("user_email", "meydihikara@gmail.com")
            storage.putString("user_phone", "+628123456789")
        }
        refreshStorageData()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DarkBg)) {
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
                            text = "Konsol Enkripsi Kriptografi",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Key Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isKeyExists) NeonTeal.copy(alpha = 0.12f) else RedAlert.copy(alpha = 0.12f))
                            .border(1.dp, if (isKeyExists) NeonTeal.copy(alpha = 0.4f) else RedAlert.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isKeyExists) NeonTeal else RedAlert, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isKeyExists) "KEYSTORE ACTIVE" else "KEYSTORE MISSING",
                            color = if (isKeyExists) NeonTeal else RedAlert,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = BorderStrokeCol, thickness = 1.dp)
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 🔒 SECTION 1: ANDROID KEYSTORE MASTER KEY METADATA & CONSOLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderStrokeCol)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manajemen Master Key (Android Keystore)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Encryption Spec Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderStrokeCol, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                KeySpecRow("Alias Kunci", "MeydiAISecureEncryptionKey_v2")
                                KeySpecRow("Algoritma", "AES (Symmetric)")
                                KeySpecRow("Ukuran Kunci", "256-Bit (Maksimum Keamanan)")
                                KeySpecRow("Mode Enkripsi", "GCM (Galois/Counter Mode)")
                                KeySpecRow("Padding", "NoPadding")
                                KeySpecRow("Keystore Provider", "AndroidKeyStore (Sisi Perangkat Keras)")
                            }
                        }

                        // Management Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val success = EncryptionManager.performKeyRotation()
                                    if (success) {
                                        Toast.makeText(context, "Master Key berhasil dirotasi & seluruh data dipindahkan!", Toast.LENGTH_LONG).show()
                                        refreshStorageData()
                                    } else {
                                        Toast.makeText(context, "Rotasi kunci gagal!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, NeonPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotasi Kunci", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    EncryptionManager.wipeAllUserData()
                                    Toast.makeText(context, "Sandi Master Key dihapus & Seluruh data dimusnahkan!", Toast.LENGTH_LONG).show()
                                    refreshStorageData()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RedAlert.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, RedAlert),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pemusnahan Data", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 🧪 SECTION 2: INTERACTIVE CRYPTOGRAPHY SANDBOX (TRY ENCRYPT/DECRYPT)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderStrokeCol)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Interactive Cryptography Sandbox", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "Ketik teks sensitif apa saja di bawah ini untuk melihat simulasi enkripsi AES-256 GCM secara visual real-time.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = sandboxPlainText,
                            onValueChange = { sandboxPlainText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Masukkan data rahasia...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = BorderStrokeCol,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (sandboxPlainText.trim().isEmpty()) {
                                        Toast.makeText(context, "Masukkan teks terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    try {
                                        sandboxCipherText = CryptoUtils.encrypt(sandboxPlainText)
                                        // Retrieve combined bytes to show IV
                                        val combined = android.util.Base64.decode(sandboxCipherText, android.util.Base64.NO_WRAP)
                                        val iv = combined.take(12).toByteArray()
                                        sandboxIVHex = iv.joinToString("") { String.format("%02X", it) }
                                        EncryptionManager.logSecurityEvent("Sandbox: Teks berhasil dienkripsi dengan GCM.")
                                        refreshStorageData()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal enkripsi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enkripsi AES-256", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (sandboxCipherText.trim().isEmpty()) {
                                        Toast.makeText(context, "Enkripsi teks terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    try {
                                        sandboxDecryptedText = CryptoUtils.decrypt(sandboxCipherText)
                                        EncryptionManager.logSecurityEvent("Sandbox: Teks berhasil didekripsi kembali.")
                                        refreshStorageData()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal dekripsi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderStrokeCol),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Dekripsi Kembali", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Sandbox Result Blocks
                        if (sandboxCipherText.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("GCM Initialization Vector (12-Byte IV HEX):", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkBg)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = sandboxIVHex,
                                        color = TerminalGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ciphertext (Base64 Encrypted Output):", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(sandboxCipherText))
                                            Toast.makeText(context, "Ciphertext disalin!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkBg)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = sandboxCipherText,
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (sandboxDecryptedText.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Decrypted Output (Teks Hasil Dekripsi):", color = TerminalGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TerminalGreen.copy(alpha = 0.08f))
                                        .border(1.dp, TerminalGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sandboxDecryptedText,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 💾 SECTION 3: SECURE STORAGE INSPECTOR (SHARED PREFERENCES VIEWER)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderStrokeCol)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SdCard, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Secure Storage Viewer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            // Toggle view decrypted values
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dekripsi", color = if (viewDecryptedValues) NeonTeal else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = viewDecryptedValues,
                                    onCheckedChange = { viewDecryptedValues = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBg,
                                        checkedTrackColor = NeonTeal,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = DarkBg
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }

                        Text(
                            text = "Data tersimpan di SharedPreferences lokal di bawah ini. Aktifkan sakelar \"Dekripsi\" untuk membandingkan teks enkripsi tersembunyi dengan data aslinya.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        // Add new secure entry form
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text("Simpan Key-Value Enkripsi Baru:", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customPrefKey,
                                    onValueChange = { customPrefKey = it },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    placeholder = { Text("Kunci (Key)", color = Color.Gray, fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonTeal,
                                        unfocusedBorderColor = BorderStrokeCol,
                                        focusedContainerColor = SurfaceBg,
                                        unfocusedContainerColor = SurfaceBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = customPrefValue,
                                    onValueChange = { customPrefValue = it },
                                    modifier = Modifier.weight(1.5f).height(48.dp),
                                    placeholder = { Text("Nilai Rahasia", color = Color.Gray, fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonTeal,
                                        unfocusedBorderColor = BorderStrokeCol,
                                        focusedContainerColor = SurfaceBg,
                                        unfocusedContainerColor = SurfaceBg
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (customPrefKey.trim().isEmpty() || customPrefValue.trim().isEmpty()) {
                                            Toast.makeText(context, "Key dan Value wajib diisi!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        EncryptionManager.getSecureStorageInstance().putString(customPrefKey, customPrefValue)
                                        EncryptionManager.logSecurityEvent("SecureStorage: Menyimpan entry rahasia untuk '$customPrefKey'")
                                        customPrefKey = ""
                                        customPrefValue = ""
                                        refreshStorageData()
                                    },
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Simpan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Storage Table
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rawStorageList.isEmpty()) {
                                Text("Tidak ada data tersimpan di Secure Storage.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            } else {
                                rawStorageList.forEach { (key, rawVal) ->
                                    val decryptedVal = secureStorageList[key] ?: "[Gagal Dekripsi]"
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkBg)
                                            .border(1.dp, BorderStrokeCol, RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = key, color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                IconButton(
                                                    onClick = {
                                                        EncryptionManager.getSecureStorageInstance().remove(key)
                                                        EncryptionManager.logSecurityEvent("SecureStorage: Menghapus entry '$key'")
                                                        refreshStorageData()
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedAlert.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            
                                            Text(
                                                text = if (viewDecryptedValues) decryptedVal else rawVal,
                                                color = if (viewDecryptedValues) Color.White else Color.Gray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 3
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🌐 SECTION 4: NETWORK TLS SECURITY VALIDATOR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderStrokeCol)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Keamanan Jaringan & TLS Validator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "Memastikan seluruh pengiriman data ke server API eksternal dilindungi oleh enkripsi TLS/HTTPS yang aman (Data in Transit).",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = testUrlInput,
                            onValueChange = {
                                testUrlInput = it
                                isUrlSecureState = NetworkSecurity.isConnectionSecure(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Uji URL API") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = BorderStrokeCol,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Encryption in transit indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isUrlSecureState) TerminalGreen.copy(alpha = 0.08f) else RedAlert.copy(alpha = 0.08f))
                                .border(1.dp, if (isUrlSecureState) TerminalGreen.copy(alpha = 0.3f) else RedAlert.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUrlSecureState) Icons.Default.Https else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isUrlSecureState) TerminalGreen else RedAlert
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isUrlSecureState) "Koneksi Transit Aman (HTTPS)" else "Koneksi Transit Tidak Aman (HTTP)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isUrlSecureState) "Data terenkripsi penuh selama dalam transmisi (TLS 1.3)." else "Data dikirim dalam format teks biasa! Sangat rentan pencurian data.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (testUrlInput.trim().isEmpty()) return@Button
                                EncryptionManager.logSecurityEvent("NetworkSecurity: Melakukan simulasi koneksi aman untuk URL: $testUrlInput")
                                
                                val isSecure = NetworkSecurity.isConnectionSecure(testUrlInput)
                                if (isSecure) {
                                    networkTestResult = "Koneksi Terkonfigurasi secara Aman!\n- Protokol: TLS v1.3 Aktif\n- HostnameVerifier: STANDAR\n- Sertifikat SSL divalidasi dengan sukses."
                                } else {
                                    val err = com.example.security.NetworkInsecurityException("Koneksi ditolak karena host menggunakan protokol HTTP polos yang dilarang oleh CleartextTrafficPolicy.")
                                    networkTestResult = "Gagal Menghubungkan!\nError: ${NetworkSecurity.sanitizeNetworkErrorMessage(err)}"
                                }
                                refreshStorageData()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BorderStrokeCol),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulasi Tes Jaringan Aman", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (networkTestResult.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = networkTestResult,
                                    color = if (networkTestResult.contains("Aman")) TerminalGreen else RedAlert,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 📜 SECTION 5: GLOWING CYBER SECURITY EVENT CONSOLE (AUDIT LOGS)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    border = BorderStroke(1.dp, BorderStrokeCol)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Konsol Audit Log Keamanan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            IconButton(
                                onClick = {
                                    EncryptionManager.clearSecurityLogs()
                                    refreshStorageData()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Terminal block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderStrokeCol, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(securityLogs) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("GAGAL") || log.contains("Peringatan")) RedAlert else TerminalGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeySpecRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
