package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.SecurityLog
import com.example.ui.theme.*
import com.example.viewmodel.SecurityViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboardScreen(
    onBack: () -> Unit,
    viewModel: SecurityViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Sinkronisasi data real-time dengan StateFlow di ViewModel
    val isRooted by viewModel.isRooted.collectAsState()
    val isEmulator by viewModel.isEmulator.collectAsState()
    val apiKeyPreview by viewModel.apiKeyPreview.collectAsState()
    val isApiKeyValid by viewModel.isApiKeyValid.collectAsState()
    val securityLogs by viewModel.securityLogs.collectAsState()
    
    val simulatedResponse by viewModel.simulatedResponse.collectAsState()
    val isRequestLoading by viewModel.isRequestLoading.collectAsState()
    val requestError by viewModel.requestError.collectAsState()
    
    val plainText by viewModel.plainText.collectAsState()
    val encryptedText by viewModel.encryptedText.collectAsState()
    val decryptedText by viewModel.decryptedText.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🛡️ STATUS", "✉️ ANTI-SPAM", "🔑 ENKRIPSI", "📋 AUDIT LOG", "📱 IZIN OS")

    // -- BAGIAN DATA PERIZINAN ORISINAL (DIAMBIL PENUH DARI MEYDIAIAPP.KT) --
    val permPrefs = remember { context.getSharedPreferences("PermissionPrefs", android.content.Context.MODE_PRIVATE) }
    val allPermissions = remember {
        listOf(
            SecurityPermissionItem("p_camera", "Akses Kamera", "Mengambil foto dan video secara langsung untuk modul Vision Prompt dan auto-clipper.", "Izin Dasar Perangkat", Icons.Default.Camera),
            SecurityPermissionItem("p_gallery", "Akses Galeri/Penyimpanan", "Memilih dan menyimpan file hasil render video secara aman.", "Izin Dasar Perangkat", Icons.Default.FolderOpen),
            SecurityPermissionItem("p_mic", "Akses Mikrofon", "Merekam suara atau panggilan suara di editor klip.", "Izin Dasar Perangkat", Icons.Default.Mic),
            SecurityPermissionItem("p_location", "Akses Lokasi (GPS)", "Menentukan lokasi pengguna secara akurat untuk metadata konten.", "Izin Dasar Perangkat", Icons.Default.LocationOn),
            SecurityPermissionItem("p_contacts", "Akses Kontak", "Memilih atau mengelola kontak tim microstock Anda.", "Izin Dasar Perangkat", Icons.Default.Contacts),
            SecurityPermissionItem("p_notifications", "Akses Notifikasi", "Membaca atau mengelola notifikasi dari daemon pemantau render.", "Izin Sistem", Icons.Default.Notifications),
            SecurityPermissionItem("p_overlay", "Akses Overlay (Tampil di Atas)", "Menampilkan widget atau pop-up mengambang di atas aplikasi lainnya.", "Izin Sistem", Icons.Default.Layers),
            SecurityPermissionItem("p_autostart", "Akses Auto Start", "Menjalankan aplikasi secara otomatis saat perangkat menyembur nyala.", "Izin Sistem", Icons.Default.FlashOn),
            SecurityPermissionItem("p_bgservice", "Akses Background Service", "Menjalankan proses kompilasi klip & looping di latar belakang secara stabil.", "Izin Sistem", Icons.Default.Sync),
            SecurityPermissionItem("p_vibration", "Akses Getaran", "Memberikan umpan balik haptic halus saat berinteraksi di canvas.", "Izin Sistem", Icons.Default.Vibration),
            SecurityPermissionItem("p_documents", "Akses Dokumen", "Membaca dan mengelola seluruh salinan dokumen ekspor.", "Izin File & Media", Icons.Default.Description),
            SecurityPermissionItem("p_downloads", "Akses Unduhan", "Menyimpan hasil video atau klip ke folder download internal.", "Izin File & Media", Icons.Default.Download),
            SecurityPermissionItem("p_audio_media", "Akses Media Audio", "Mengakses file musik loop dan klip audio pendukung.", "Izin File & Media", Icons.Default.MusicNote),
            SecurityPermissionItem("p_video_media", "Akses Media Video", "Mengakses video perangkat untuk penyuntingan frame detail.", "Izin File & Media", Icons.Default.PlayCircle),
            SecurityPermissionItem("p_image_media", "Akses Media Gambar", "Mengakses seluruh foto untuk background canvas template.", "Izin File & Media", Icons.Default.Image),
            SecurityPermissionItem("p_internet", "Akses Internet", "Menghubungkan aplikasi ke server automasi cloud MeydiAi.", "Izin Konektivitas", Icons.Default.Language),
            SecurityPermissionItem("p_wifi", "Akses Wi-Fi", "Mendeteksi secara real-time status koneksi jaringan nirkabel.", "Izin Konektivitas", Icons.Default.Wifi),
            SecurityPermissionItem("p_bluetooth", "Akses Bluetooth", "Terhubung secara instan ke headset/speaker monitoring eksternal.", "Izin Konektivitas", Icons.Default.Bluetooth),
            SecurityPermissionItem("p_nfc", "Akses NFC", "Membaca atau menulis data verifikasi NFC sertifikat lisensi.", "Izin Konektivitas", Icons.Default.CreditCard),
            SecurityPermissionItem("p_biometric", "Akses Biometrik", "Masuk login aman dengan sidik jari atau enkripsi pengenalan wajah.", "Izin Lanjutan", Icons.Default.Fingerprint),
            SecurityPermissionItem("p_clipboard", "Akses Clipboard", "Membaca dan menempel cepat materi teks dari papan klip eksternal.", "Izin Lanjutan", Icons.Default.Assignment),
            SecurityPermissionItem("p_calendar", "Akses Kalender", "Membaca dan menyusun jadwal rilis terjadwal konten sosial media.", "Izin Lanjutan", Icons.Default.DateRange),
            SecurityPermissionItem("p_sensors", "Akses Sensor Perangkat", "Akses kompas, giroskop dan sensor gerakan untuk rotasi dinamis.", "Izin Lanjutan", Icons.Default.Explore),
            SecurityPermissionItem("p_battery", "Akses Baterai Optimization", "Pengecualian baterai khusus agar aktivitas render latar belakang tidak dijeda OS.", "Izin Lanjutan", Icons.Default.BatteryAlert)
        )
    }

    val manifestMapping = remember {
        mapOf(
            "p_camera" to listOf(android.Manifest.permission.CAMERA),
            "p_mic" to listOf(android.Manifest.permission.RECORD_AUDIO),
            "p_location" to listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            "p_contacts" to listOf(android.Manifest.permission.READ_CONTACTS),
            "p_notifications" to if (android.os.Build.VERSION.SDK_INT >= 33) {
                listOf(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            },
            "p_gallery" to if (android.os.Build.VERSION.SDK_INT >= 33) {
                listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            "p_image_media" to if (android.os.Build.VERSION.SDK_INT >= 33) {
                listOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            "p_video_media" to if (android.os.Build.VERSION.SDK_INT >= 33) {
                listOf(android.Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            "p_audio_media" to if (android.os.Build.VERSION.SDK_INT >= 33) {
                listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
    }

    val permissionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allPermissions.forEach { item ->
                val defaultVal = if (item.key == "p_internet" || item.key == "p_wifi") true else false
                put(item.key, permPrefs.getBoolean(item.key, defaultVal))
            }
        }
    }

    val systemGrantedStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allPermissions.forEach { item ->
                val perms = manifestMapping[item.key]
                val isGranted = if (perms.isNullOrEmpty()) {
                    false
                } else {
                    perms.all { p ->
                        androidx.core.content.ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                }
                put(item.key, isGranted)
            }
        }
    }

    var activeRequestKey by remember { mutableStateOf<String?>(null) }
    
    val systemPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        activeRequestKey?.let { key ->
            val pItem = allPermissions.find { it.key == key }
            if (pItem != null) {
                systemGrantedStates[key] = allGranted
                permissionStates[key] = allGranted
                permPrefs.edit().putBoolean(key, allGranted).apply()
                val statusStr = if (allGranted) "AKTIF ✅" else "DITOLAK ❌"
                Toast.makeText(context, "Izin OS: ${pItem.title} $statusStr", Toast.LENGTH_SHORT).show()
                viewModel.refreshSecurityState()
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("Semua", "Dasar Perangkat", "Izin Sistem", "File & Media", "Izin Konektivitas", "Izin Lanjutan")

    // -- RENDER UI UTAMA --
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Meydi OS Security Hub",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda",
                            tint = NeonTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                modifier = Modifier.border(0.5.dp, DarkStroke, RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Menu Cyberpunk
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MidnightSurface,
                contentColor = NeonTeal,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = NeonTeal,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = DarkStroke) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = if (activeTab == index) NeonTeal else TextMuted
                            )
                        },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            // Body Area berdasarkan Tab aktif
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MidnightSurface.copy(alpha = 0.2f), ObsidianBg)
                        )
                    )
            ) {
                when (activeTab) {
                    0 -> StatusTabContent(
                        isRooted = isRooted,
                        isEmulator = isEmulator,
                        apiKeyPreview = apiKeyPreview,
                        isApiKeyValid = isApiKeyValid,
                        viewModel = viewModel
                    )
                    1 -> AntiSpamTabContent(
                        simulatedResponse = simulatedResponse,
                        isRequestLoading = isRequestLoading,
                        requestError = requestError,
                        viewModel = viewModel,
                        clipboardManager = clipboardManager,
                        context = context
                    )
                    2 -> EncryptionTabContent(
                        plainText = plainText,
                        encryptedText = encryptedText,
                        decryptedText = decryptedText,
                        viewModel = viewModel,
                        clipboardManager = clipboardManager,
                        context = context
                    )
                    3 -> AuditLogTabContent(
                        securityLogs = securityLogs,
                        viewModel = viewModel
                    )
                    4 -> PermissionTabContent(
                        allPermissions = allPermissions,
                        permissionStates = permissionStates,
                        systemGrantedStates = systemGrantedStates,
                        manifestMapping = manifestMapping,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategoryIndex = selectedCategoryIndex,
                        onCategorySelect = { selectedCategoryIndex = it },
                        categories = categories,
                        systemPermissionLauncher = systemPermissionLauncher,
                        activeRequestKeySetter = { activeRequestKey = it },
                        context = context,
                        permPrefs = permPrefs
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. CONTENT TAB: STATUS & API KEY PROTECTION
// ==========================================
@Composable
private fun StatusTabContent(
    isRooted: Boolean,
    isEmulator: Boolean,
    apiKeyPreview: String,
    isApiKeyValid: Boolean,
    viewModel: SecurityViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🛡️ STATUS ENVIROMENT & API KEY SECURITY",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        // Card Audit Perangkat
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Deteksi Integritas Perangkat",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Status Root
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            tint = if (isRooted) ErrorRed else TerminalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deteksi Perangkat Root:", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isRooted) ErrorRed.copy(alpha = 0.15f) else TerminalGreen.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, if (isRooted) ErrorRed else TerminalGreen), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRooted) "TERDETEKSI ROOT (BAHAYA)" else "AMAN (PERANGKAT ASLI/WAJAR)",
                            color = if (isRooted) ErrorRed else TerminalGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Status Emulator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeveloperBoard,
                            contentDescription = null,
                            tint = if (isEmulator) Color.Yellow else TerminalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deteksi Emulator OS:", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isEmulator) Color.Yellow.copy(alpha = 0.15f) else TerminalGreen.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, if (isEmulator) Color.Yellow else TerminalGreen), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isEmulator) "EMULATOR TERDETEKSI" else "PERANGKAT ASLI",
                            color = if (isEmulator) Color.Yellow else TerminalGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Card API Key Protection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Perlindungan Kredensial API Key",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "API Key ditarik dinamis dari backend setelah login dan disimpan terenkripsi di dalam Android Keystore.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // API Key Preview area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .border(BorderStroke(0.5.dp, DarkStroke), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text("API Key Lokal Saat Ini:", color = TextMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = apiKeyPreview,
                        color = if (isApiKeyValid) NeonTeal else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Validitas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Lisensi API Key:", color = Color.LightGray, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isApiKeyValid) TerminalGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, if (isApiKeyValid) TerminalGreen else ErrorRed), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isApiKeyValid) "AKTIF & TERSERTIFIKASI" else "KEDALUWARSA / BELUM AMBIL",
                            color = if (isApiKeyValid) TerminalGreen else ErrorRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol aksi API key
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerApiKeyRotation() },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonTeal),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotasi Key", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.revokeLocalApiKey() },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revoke Key", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card Audit Info & Reset
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tindakan Keamanan Tambahan",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan tombol ini untuk mereset seluruh database keamanan lokal, cache request, dan log.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        viewModel.clearAllSecurityData()
                        Toast.makeText(context, "Seluruh data keamanan lokal berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pembersihan & Reset Keamanan Penuh", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 2. CONTENT TAB: ANTI-SPAM SIMULATOR
// ==========================================
@Composable
private fun AntiSpamTabContent(
    simulatedResponse: String,
    isRequestLoading: Boolean,
    requestError: String?,
    viewModel: SecurityViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val scrollState = rememberScrollState()
    var payloadInput by remember { mutableStateOf("{\"task_id\": \"meydi_clip_009\", \"speed\": 2}") }
    
    // Config Slider State
    var maxReqSlider by remember { mutableFloatStateOf(5f) }
    var windowSlider by remember { mutableFloatStateOf(10f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "✉️ SIMULATOR PROTEKSI ANTI-SPAM & RATE LIMIT",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        // Card Konfigurasi Remote Config Dinamis
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konfigurasi Dinamis (Simulasi Remote Config)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Slider Max Request
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Batas Maks Request: ${maxReqSlider.toInt()} Kali", color = Color.LightGray, fontSize = 11.sp)
                }
                Slider(
                    value = maxReqSlider,
                    onValueChange = { 
                        maxReqSlider = it
                        viewModel.updateSpamConfig(maxReqSlider.toInt(), windowSlider.toInt())
                    },
                    valueRange = 2f..15f,
                    steps = 12,
                    colors = SliderDefaults.colors(thumbColor = NeonTeal, activeTrackColor = NeonTeal)
                )

                // Slider Window Size
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Durasi Jendela Waktu: ${windowSlider.toInt()} Detik", color = Color.LightGray, fontSize = 11.sp)
                }
                Slider(
                    value = windowSlider,
                    onValueChange = { 
                        windowSlider = it
                        viewModel.updateSpamConfig(maxReqSlider.toInt(), windowSlider.toInt())
                    },
                    valueRange = 5f..60f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }
        }

        // Card Simulasi Permintaan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aksi Pengujian Cepat (Klik Cepat Untuk Cooldown)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Klik tombol di bawah secara beruntun dan cepat. Sistem anti-spam akan langsung memotong request ganda (double-click prevention) dan mengaktifkan cooldown otomatis di sisi klien!",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = payloadInput,
                    onValueChange = { payloadInput = it },
                    label = { Text("Request Payload (JSON)", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dua tombol simulasi (POST & GET)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.runSimulatedRequest("POST", payloadInput) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRequestLoading
                    ) {
                        if (isRequestLoading) {
                            CircularProgressIndicator(color = ObsidianBg, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("POST Secure", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.runSimulatedRequest("GET") },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRequestLoading
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GET (Cache)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tampilan Hasil Simulasi
        Column {
            Text("Console Output:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Box Terminal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (requestError != null) {
                        Text(
                            text = "[SECURITY TRIGGERED]\n$requestError",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (simulatedResponse.isNotEmpty()) {
                        Text(
                            text = "[HTTP 200 SUCCESS]\n$simulatedResponse",
                            color = TerminalGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "Menunggu trigger aksi simulasi request...",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Tombol Copy Response
                if (simulatedResponse.isNotEmpty() && requestError == null) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(simulatedResponse))
                            Toast.makeText(context, "Respon disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Respon", tint = NeonTeal, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. CONTENT TAB: ENKRIPSI KEYSTORE INTERAKTIF
// ==========================================
@Composable
private fun EncryptionTabContent(
    plainText: String,
    encryptedText: String,
    decryptedText: String,
    viewModel: SecurityViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val scrollState = rememberScrollState()
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔑 ENKRIPSI HARDWARE-BACKED KEYSTORE (AES-GCM)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enkripsi Sandbox Mandiri",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Masukkan teks rahasia di bawah. Aplikasi akan melakukan enkripsi AES 256-bit GCM menggunakan kunci yang disimpan langsung di dalam area aman chip hardware perangkat (Android Keystore).",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Teks Rahasia / Sandi Sensitif", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.encryptText(textInput) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enkripsi AES", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.decryptText(encryptedText) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        enabled = encryptedText.isNotEmpty() && !encryptedText.contains("Gagal")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dekripsi Kunci", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tampilan Hasil Enkripsi
        if (encryptedText.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ciphertext (Base64 + IV + Tag):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = encryptedText,
                        color = NeonMagenta,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(encryptedText))
                            Toast.makeText(context, "Ciphertext disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin", tint = NeonTeal, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Tampilan Hasil Dekripsi
        if (decryptedText.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Hasil Dekripsi Keystore (Original):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = decryptedText,
                        color = TerminalGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. CONTENT TAB: REAL-TIME AUDIT LOG LIST
// ==========================================
@Composable
private fun AuditLogTabContent(
    securityLogs: List<SecurityLog>,
    viewModel: SecurityViewModel
) {
    val formatter = remember { java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 LOG AUDIT KEAMANAN REAL-TIME",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "${securityLogs.size} Log",
                color = NeonTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (securityLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada aktivitas keamanan yang tercatat.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(securityLogs) { log ->
                        val color = when (log.severity) {
                            "INFO" -> TerminalGreen
                            "WARNING" -> Color.Yellow
                            "CRITICAL" -> ErrorRed
                            else -> Color.LightGray
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(0.5.dp, DarkStroke.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                .background(MidnightSurface.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${log.severity}] ${log.event}",
                                    color = color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = formatter.format(java.util.Date(log.timestamp)),
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.details,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = { viewModel.refreshSecurityState() },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Segarkan Log", color = Color.White, fontSize = 12.sp)
        }
    }
}

// ==========================================
// 5. CONTENT TAB: PERMISSION MANAGER (ORIGINAL RECONCILED)
// ==========================================
@Composable
private fun PermissionTabContent(
    allPermissions: List<SecurityPermissionItem>,
    permissionStates: MutableMap<String, Boolean>,
    systemGrantedStates: MutableMap<String, Boolean>,
    manifestMapping: Map<String, List<String>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategoryIndex: Int,
    onCategorySelect: (Int) -> Unit,
    categories: List<String>,
    systemPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    activeRequestKeySetter: (String) -> Unit,
    context: android.content.Context,
    permPrefs: android.content.SharedPreferences
) {
    val filteredPermissions = remember(searchQuery, selectedCategoryIndex) {
        allPermissions.filter { perm ->
            val matchesSearch = perm.title.contains(searchQuery, ignoreCase = true) || perm.desc.contains(searchQuery, ignoreCase = true)
            val matchesCategory = if (selectedCategoryIndex == 0) true else perm.category == categories[selectedCategoryIndex]
            matchesSearch && matchesCategory
        }
    }

    val activeCount = permissionStates.values.count { it }
    val totalCount = allPermissions.size
    val ratio = if (totalCount > 0) activeCount.toFloat() / totalCount.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, DarkStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status Sandbox Izin Aplikasi",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$activeCount dari $totalCount Izin Aktif",
                                color = NeonTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = "${(ratio * 100).toInt()}% Diizinkan",
                            color = if (ratio > 0.7f) TerminalGreen else if (ratio > 0.3f) NeonTeal else Color.Yellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonTeal,
                        trackColor = DarkStroke
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bulk Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                allPermissions.forEach { item ->
                                    permissionStates[item.key] = true
                                    permPrefs.edit().putBoolean(item.key, true).apply()
                                }
                                Toast.makeText(context, "Semua izin lokal diaktifkan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, NeonTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Izinkan Semua", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                allPermissions.forEach { item ->
                                    permissionStates[item.key] = false
                                    permPrefs.edit().putBoolean(item.key, false).apply()
                                }
                                Toast.makeText(context, "Semua izin lokal dinonaktifkan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Cabut Semua", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search Bar & Filters row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Cari Izin Spesifik...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = NeonTeal, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke,
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Scrollable category select row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { index, catName ->
                        val isSelected = selectedCategoryIndex == index
                        val borderCol = if (isSelected) NeonTeal else DarkStroke
                        val bgCol = if (isSelected) NeonTeal.copy(alpha = 0.15f) else MidnightSurface
                        val textCol = if (isSelected) NeonTeal else Color.LightGray

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30))
                                .background(bgCol)
                                .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(30))
                                .clickable { onCategorySelect(index) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catName,
                                color = textCol,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // List of permission cards
        if (filteredPermissions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada izin ditemukan.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredPermissions) { permission ->
                val isChecked = permissionStates[permission.key] ?: false
                val isMapped = manifestMapping.containsKey(permission.key)
                val isOSGranted = systemGrantedStates[permission.key] ?: false

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, if (isOSGranted) NeonTeal.copy(alpha = 0.6f) else if (isChecked) NeonPurple.copy(alpha = 0.4f) else DarkStroke)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isOSGranted) NeonTeal.copy(alpha = 0.15f) else if (isChecked) NeonPurple.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = permission.icon,
                                contentDescription = permission.title,
                                tint = if (isOSGranted) NeonTeal else if (isChecked) NeonPurple else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(permission.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(permission.desc, color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                            
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(permission.category, color = NeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                if (isMapped) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isOSGranted) TerminalGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                                            .border(BorderStroke(0.5.dp, if (isOSGranted) TerminalGreen else ErrorRed.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (isOSGranted) "● OS GRANTED" else "○ OS DENIED",
                                            color = if (isOSGranted) TerminalGreen else ErrorRed,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isMapped && !isOSGranted) {
                            Button(
                                onClick = {
                                    val systemPerms = manifestMapping[permission.key]
                                    if (!systemPerms.isNullOrEmpty()) {
                                        activeRequestKeySetter(permission.key)
                                        systemPermissionLauncher.launch(systemPerms.toTypedArray())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Minta OS", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Switch(
                            checked = isChecked || isOSGranted,
                            onCheckedChange = { checked ->
                                permissionStates[permission.key] = checked
                                permPrefs.edit().putBoolean(permission.key, checked).apply()
                                if (checked && isMapped && !isOSGranted) {
                                    val systemPerms = manifestMapping[permission.key]
                                    if (!systemPerms.isNullOrEmpty()) {
                                        activeRequestKeySetter(permission.key)
                                        systemPermissionLauncher.launch(systemPerms.toTypedArray())
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = NeonTeal,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = DarkStroke
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }
        
        // Android 10+ Education
        item {
            Text(
                text = "🛡️ Model Arsitektur Keamanan Android 10+",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, DarkStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val guides = listOf(
                        "Android 10 (Scoped Storage)" to "Aplikasi diisolasikan dalam folder Sandbox lokal. Izin akses penyimpanan lama didepresiasi.",
                        "Android 11 (One-Time Permissions)" to "Sistem privasi mengizinkan pemberian akses satu kali saja. Sistem otomatis mencabut seluruh izin jika aplikasi dianggurkan lama.",
                        "Android 12 (Presisi Lokasi)" to "User mendapat pilihan mengizinkan status lokasi Presisi (Fine) atau Perkiraan saja (Coarse) demi privasi.",
                        "Android 13 (Media Tersegmentasi)" to "Akses baca file diganti tipe spesifik: Audio, Video, & Foto. Notifikasi dikunci lewat izin runtime.",
                        "Android 14 (Visual Media Picker)" to "Pengguna kini dapat membatasi akses foto secara parsial (hanya foto tertentu)."
                    )
                    guides.forEachIndexed { idx, guide ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx+1}", color = NeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(guide.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(guide.second, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
