package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- DESKTOP CONSTANTS & COLOR THEMES (Meydi OS Core Engine) ---
private val PrimaryTheme = Color(0xFF00FFCC)
private val PanelBackground = Color(0xFF131524)
private val WindowBg = Color(0xFF1E2032)
private val TaskbarColor = Color(0xF10D0F1B)
private val TaskbarBorder = Color(0x22FFFFFF)
private val TerminalGreen = Color(0xFF00FF99)
private val ErrorRed = Color(0xFFFF3366)

enum class OSTheme(val displayName: String, val primary: Color, val bgTop: Color, val bgBottom: Color) {
    SLATE_CYBERPUNK("Slate Cyberpunk", Color(0xFF00FFCC), Color(0xFF1E2032), Color(0xFF0F111A)),
    CLASSIC_MINT("Meydi Mint Core", Color(0xFF00FF99), Color(0xFF0C1D15), Color(0xFF050E0A)),
    SOLAR_PURPLE("Solarized Purple", Color(0xFFE040FB), Color(0xFF1A0F26), Color(0xFF0C0712)),
    DARK_MIDNIGHT("Midnight Nebula", Color(0xFF2196F3), Color(0xFF050B14), Color(0xFF020408))
}

data class DesktopApp(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSDesktopScreen(
    isAdmin: Boolean = false,
    isOwner: Boolean = false,
    isIosUser: Boolean = false,
    systemAlertMessage: String = "",
    permissionsGranted: Boolean = true,
    onRequestPermissions: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToOwner: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToRemotion: () -> Unit,
    onNavigateToClipper: () -> Unit,
    onNavigateToDownloader: () -> Unit,
    onNavigateToEnhancer: () -> Unit,
    onNavigateToPromptGenerator: () -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToTemplateStudio: () -> Unit,
    onNavigateToNetworkMonitor: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Settings Shared Preferences for Persistence
    val sysPrefs = remember { context.getSharedPreferences("OSCoreEnginePrefs", android.content.Context.MODE_PRIVATE) }

    // --- SYSTEMS STATE ENG_CORE ---
    var themeSelection by remember { 
        mutableStateOf(
            try { 
                OSTheme.valueOf(sysPrefs.getString("system_theme", OSTheme.SLATE_CYBERPUNK.name) ?: OSTheme.SLATE_CYBERPUNK.name) 
            } catch (e: Exception) { 
                OSTheme.SLATE_CYBERPUNK 
            }
        ) 
    }
    var isAutoSaveEnabled by remember { mutableStateOf(sysPrefs.getBoolean("auto_save", true)) }
    var isCloudSyncEnabled by remember { mutableStateOf(sysPrefs.getBoolean("cloud_sync", true)) }
    var isRealtimeNotifEnabled by remember { mutableStateOf(sysPrefs.getBoolean("realtime_notif", true)) }
    var isPremiumLicense by remember { mutableStateOf(sysPrefs.getBoolean("premium_license", false)) }

    // API Key State variables
    var geminiKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getGeminiKey()) }
    var falKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFalKey()) }
    var azbryKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getAzbryKey()) }

    // Saved/Shared parameters
    var lastBackupTime by remember { mutableStateOf(sysPrefs.getString("last_cloud_backup", "12/06/2026 10:45 AM") ?: "12/06/2026 10:45 AM") }
    var updateBuildVersion by remember { mutableStateOf("V2.5.0-MeydiCore") }
    
    // UI Panels Toggles
    var showStartMenu by remember { mutableStateOf(false) }
    var showSettingsWindow by remember { mutableStateOf(false) }
    var showTerminalWindow by remember { mutableStateOf(false) }
    var showNotificationBanner by remember { mutableStateOf(false) }
    var currentNotifText by remember { mutableStateOf("") }

    // Clock
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Logs Console & Terminal States
    var terminalOutput by remember { 
        mutableStateOf(listOf(
            "==================================================",
            "  MEYDI OS CORE ENGINE [KERNEL BOOT SUCCESSFUL]     ",
            "  Developer : Meydi | Architecture: Enterprise AI   ",
            "==================================================",
            "Type /help to display list of available core scripts.",
            ""
        )) 
    }
    var currentCommandText by remember { mutableStateOf("") }
    var isSystemSyncInProgress by remember { mutableStateOf(false) }
    var systemSyncProgress by remember { mutableStateOf(0f) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    // Clock auto refresh
    LaunchedEffect(Unit) {
        val formatterTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formatterDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = formatterTime.format(now)
            currentDate = formatterDate.format(now)
            delay(1000)
        }
    }

    // Auto save daemon background simulation (Interval of 8s)
    LaunchedEffect(isAutoSaveEnabled) {
        if (isAutoSaveEnabled) {
            while (true) {
                delay(8000)
                sysPrefs.edit().putBoolean("auto_save", true).apply()
                if (isRealtimeNotifEnabled) {
                    currentNotifText = "💾 Auto-save: State draft system tersimpan aman [OS-Meydi]"
                    showNotificationBanner = true
                    delay(2500)
                    showNotificationBanner = false
                }
            }
        }
    }

    // Desktop Apps Icons list
    val desktopApps = listOf(
        DesktopApp("template", "Template Studio", Icons.Default.AutoFixHigh, Color(0xFFE040FB), onNavigateToTemplateStudio),
        DesktopApp("assistant", "Meydi Assistant", Icons.Default.ChatBubbleOutline, Color(0xFF2196F3), onNavigateToAssistant),
        DesktopApp("monitor", "Network Monitor", Icons.Default.NetworkCheck, Color(0xFF00E676), onNavigateToNetworkMonitor),
        DesktopApp("downloader", "Media Downloader", Icons.Default.Download, Color(0xFFFF5722), onNavigateToDownloader),
        DesktopApp("enhancer", "HD Enhancer", Icons.Default.Image, Color(0xFF9C27B0), onNavigateToEnhancer),
        DesktopApp("clipper", "AI Clipper", Icons.Default.Movie, Color(0xFFFFEB3B), onNavigateToClipper),
        DesktopApp("remotion", "Remotion", Icons.Default.Layers, Color(0xFF00B0FF), onNavigateToRemotion),
        DesktopApp("prompt", "Prompt Gen", Icons.Default.FlashOn, Color(0xFFFF9800), onNavigateToPromptGenerator),
    )

    // Helper functions
    fun triggerPushNotification(msg: String) {
        currentNotifText = msg
        scope.launch {
            showNotificationBanner = true
            delay(3000)
            showNotificationBanner = false
        }
    }

    fun executeTerminalCommand(cmdString: String) {
        if (cmdString.isBlank()) return
        val command = cmdString.trim()
        val parts = command.split(" ")
        val rootcmd = parts[0].lowercase()

        terminalOutput = terminalOutput + "meydi@gemini-core-os:~$ $command"

        scope.launch {
            when (rootcmd) {
                "/help" -> {
                    terminalOutput = terminalOutput + listOf(
                        "Daftar Perintah Core Engine:",
                        "  /auth_login           - Menampilkan status & log autentikasi pengguna API",
                        "  /system_autosave      - Mengaktifkan/menonaktifkan daemon pelacak draf state",
                        "  /cloud_sync           - Menjalankan sinkronisasi database manual ke cloud",
                        "  /push_notification    - Memicu notifikasi uji coba ke Desktop OS",
                        "  /admin_panel          - Menampilkan statistik server & DAU realtime",
                        "  /ai_assistant         - Memanggil asisten Gemini Kernel Meydi",
                        "  /premium_billing      - Memeriksa status lisensi & aktivasi premium",
                        "  /toggle_theme         - Mengubah style/tema dari OS ini secara acak",
                        "  /check_update         - Memeriksa file patch versi firmware terbaru",
                        "  /clear                - Membersihkan layar konsol terminal"
                    )
                }
                "/clear" -> {
                    terminalOutput = listOf("Terminal cleared.")
                }
                "/auth_login" -> {
                    terminalOutput = terminalOutput + listOf(
                        "🔍 Memvalidasi sesi autentikasi keamanan API...",
                        "🟢 Sesi Terenkripsi: OK (Argon2Id)",
                        "👤 Pengguna: meydihikara@gmail.com",
                        "🛡️ Hak Akses: SYSTEM ADMINISTRATOR"
                    )
                    triggerPushNotification("🔑 Autentikasi log terenkripsi & aman.")
                }
                "/system_autosave" -> {
                    isAutoSaveEnabled = !isAutoSaveEnabled
                    sysPrefs.edit().putBoolean("auto_save", isAutoSaveEnabled).apply()
                    val status = if (isAutoSaveEnabled) "AKTIF" else "NONAKTIF"
                    terminalOutput = terminalOutput + "💾 Daemon Autosave diubah menjadi: $status"
                    triggerPushNotification("💾 Autosave Engine: Sekarang $status")
                }
                "/cloud_sync" -> {
                    if (isSystemSyncInProgress) {
                        terminalOutput = terminalOutput + "⚠️ Sinkronisasi sedang berlangsung!"
                    } else {
                        isSystemSyncInProgress = true
                        terminalOutput = terminalOutput + "🚀 Memulai Upload Cadangan Cloud Meydi Database..."
                        for (i in 1..5) {
                            delay(400)
                            systemSyncProgress = i * 20f
                            terminalOutput = terminalOutput + "📤 Mengirim paket log_${i}.db.aes ... ${i*20}%"
                        }
                        delay(250)
                        lastBackupTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                        sysPrefs.edit().putString("last_cloud_backup", lastBackupTime).apply()
                        terminalOutput = terminalOutput + listOf(
                            "✅ Cadangan Server Awan Selesai. Status DB: Sinkron.",
                            "📅 Waktu Backup: $lastBackupTime"
                        )
                        isSystemSyncInProgress = false
                        systemSyncProgress = 0f
                        triggerPushNotification("☁️ Cloud database sukses terdistribusi & di-backup.")
                    }
                }
                "/push_notification" -> {
                    terminalOutput = terminalOutput + "🔔 Menguji sistem WebSocket push notification..."
                    triggerPushNotification("🔔 Notifikasi Real-Time: Mesin pengirim pesan aktif!")
                }
                "/admin_panel" -> {
                    terminalOutput = terminalOutput + listOf(
                        "🛡️ METRIK PARAMETER ARCHITECTURE (Meydi Kernel):",
                        "  📱 Layanan DAU/MAU    : 4501 / 18,940 Pengunjung Aktif",
                        "  🚀 CPU Thread Load    : 14% (Optimized under Meydi Kernel API)",
                        "  🖥️ RAM System Memory  : 2.15 GB / 8.00 GB",
                        "  🛡️ Kebocoran Port     : 0 Terdeteksi (Tembok api aktif)",
                        "  🔴 Kegagalan Sync Draf: 0% Insiden"
                    )
                }
                "/ai_assistant" -> {
                    val messages = listOf(
                        "🤖 [Gemini Kernel]: Meydi AI merancang layout ini dengan negative space yang super lega.",
                        "🤖 [Gemini Kernel]: Gunakan perintah /cloud_sync untuk memutus potensi kehilangan draf model filter.",
                        "🤖 [Gemini Kernel]: Semua modul terenkripsi presisi tinggi di bawah lisensi enterprise.",
                        "🤖 [Gemini Kernel]: Tips: Ketik /toggle_theme untuk merubah warna tema neon desktop secara ajaib."
                    )
                    terminalOutput = terminalOutput + messages.random()
                }
                "/premium_billing" -> {
                    terminalOutput = terminalOutput + "💎 Memeriksa data pembayaran lisensi..."
                    delay(500)
                    if (isPremiumLicense) {
                        terminalOutput = terminalOutput + "🏅 Status: PRESTIGE PREMIUM ENTERPRISE UNLIMITED ACTIVE"
                    } else {
                        terminalOutput = terminalOutput + listOf(
                            "⚠️ Status: FREE TIER LEVEL",
                            "💡 Tip: Jalankan transaksi billing untuk membuka filter 4K Master & Cloud Sync tanpa batas."
                        )
                    }
                }
                "/toggle_theme" -> {
                    val nextTheme = OSTheme.values().random()
                    themeSelection = nextTheme
                    sysPrefs.edit().putString("system_theme", nextTheme.name).apply()
                    terminalOutput = terminalOutput + "🧙‍♂️ Style tema diubah menjadi: ${nextTheme.displayName}!"
                    triggerPushNotification("🎨 Tema visual berganti ke ${nextTheme.displayName}")
                }
                "/check_update" -> {
                    isCheckingUpdates = true
                    terminalOutput = terminalOutput + "🌐 Menghubungi server distribusi update Meydi..."
                    delay(1200)
                    terminalOutput = terminalOutput + listOf(
                        "📦 Patch baru terdeteksi: MeydiOS Core Engine V2.5.1-Hotfix",
                        "⬇️ Mengunduh pembaruan kernel otomatis di latar belakang...",
                        "⚙️ Mengaplikasikan hot-reload state draf... Selesai!",
                        "🎉 Sistem Operasi sekarang mutakhir pada versi terbaru V2.5.1!"
                    )
                    updateBuildVersion = "V2.5.1-MeydiCore"
                    isCheckingUpdates = false
                    triggerPushNotification("🚀 Core Engine OS teraktualisasi ke V2.5.1 !")
                }
                else -> {
                    terminalOutput = terminalOutput + "❌ Perintah '$rootcmd' tidak dikenal. Ketik /help untuk daftar perintah."
                }
            }
        }
        currentCommandText = ""
    }

    // Main Desktop Workspace
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(themeSelection.bgTop, themeSelection.bgBottom)))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    showStartMenu = false
                })
            }
            .testTag("meydi_os_desktop_canvas")
    ) {
        // --- AMBIENT BLUEPRINT GRID BACKGROUND ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 44.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.015f)
            val lineStroke = 1.dp.toPx()
            
            // Draw vertical grid lines
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = lineStroke
                )
                x += gridSpacing
            }
            
            // Draw horizontal grid lines
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = lineStroke
                )
                y += gridSpacing
            }
        }
        
        // --- CENTRAL WATERMARK LOGO ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MEYDI OS",
                    color = Color.White.copy(alpha = 0.025f),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Core Engine Architecture  •  Gemini-Powered Workspace",
                    color = Color.White.copy(alpha = 0.025f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- GRID OF DESKTOP ICONS (Scrollable column) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp, top = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Apps Icons
                items(desktopApps.size) { index ->
                    val app = desktopApps[index]
                    DesktopIcon(app = app)
                }

                // Systems Control Center Icon
                item {
                    DesktopIcon(
                        app = DesktopApp(
                            "control_panel", 
                            "Settings", 
                            Icons.Default.Settings, 
                            themeSelection.primary
                        ) {
                            showSettingsWindow = true
                        }
                    )
                }

                // Terminal Shell Icon
                item {
                    DesktopIcon(
                        app = DesktopApp(
                            "terminal_console", 
                            "Core Shell", 
                            Icons.Default.Terminal, 
                            Color.LightGray
                        ) {
                            showTerminalWindow = true
                        }
                    )
                }
            }
        }

        // --- DIALOG: SYSTEM CONTROL PANEL WINDOW ---
        if (showSettingsWindow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 490.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .shadow(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelBackground)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = themeSelection.primary, modifier = Modifier.size(18.dp))
                                Text("SYSTEMS CONTROL CENTER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { showSettingsWindow = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Content
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Core Engine Daemons Tuning
                            Column {
                                Text("DAEMONS AUTO ENGINE", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // AutoSave Toggle Card
                                SettingToggleCard(
                                    title = "Sistem Autosave (/system_autosave)",
                                    desc = "Keluarkan pelacakan perubahan state model draf otomatis ke index lokal.",
                                    checked = isAutoSaveEnabled,
                                    onCheckedChange = {
                                        isAutoSaveEnabled = it
                                        sysPrefs.edit().putBoolean("auto_save", it).apply()
                                        triggerPushNotification(" Autocommit Autosave: ${if(it) "AKTIF" else "DIBATALKAN"}")
                                    },
                                    tint = themeSelection.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Secure Notification Toggle Card
                                SettingToggleCard(
                                    title = "Push Notifikasi Real-Time (/push_notification)",
                                    desc = "Kirim broadcast WebSocket saat mutasi backend selesai.",
                                    checked = isRealtimeNotifEnabled,
                                    onCheckedChange = {
                                        isRealtimeNotifEnabled = it
                                        sysPrefs.edit().putBoolean("realtime_notif", it).apply()
                                    },
                                    tint = themeSelection.primary
                                )
                            }

                            // Section: Cloud Sync DB
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CLOUD PERSISTENT ARCHITECTURE", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Text("TERHUBUNG", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Database Cloud Meydi Sync", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Titik pemulih terakhir draf: $lastBackupTime", color = Color.Gray, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (isSystemSyncInProgress) {
                                            Column {
                                                LinearProgressIndicator(
                                                    progress = systemSyncProgress / 100f,
                                                    color = themeSelection.primary,
                                                    trackColor = Color.DarkGray,
                                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Mengirim payload database... ${(systemSyncProgress).toInt()}%", color = themeSelection.primary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        } else {
                                            Button(
                                                onClick = { executeTerminalCommand("/cloud_sync") },
                                                colors = ButtonDefaults.buttonColors(containerColor = themeSelection.primary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Cloud Sync Manual (/cloud_sync)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: License / Premium Billing
                            Column {
                                Text("LOMPATAN PRESTASI PREMIUM (/premium_billing)", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPremiumLicense) themeSelection.primary.copy(alpha = 0.07f) else Color.Red.copy(alpha = 0.03f)
                                    ),
                                    border = BorderStroke(1.dp, if (isPremiumLicense) themeSelection.primary.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if(isPremiumLicense) "MEYDI ENTERPRISE GOLD PLATINUM" else "TIER AKUN REGULER GRATIS",
                                                color = if(isPremiumLicense) themeSelection.primary else Color.Red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if(isPremiumLicense) "Fitur model 4K upscaling, filter raw, dan cloud sync ultra tak terbatas aktif." else "Beberapa limitasi kuota cloud sync diberlakukan.",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = isPremiumLicense,
                                            onCheckedChange = {
                                                isPremiumLicense = it
                                                sysPrefs.edit().putBoolean("premium_license", it).apply()
                                                triggerPushNotification(if(it) "🏆 Lisensi Akun premium diaktifkan!" else "💼 Lisensi premium ditangguhkan.")
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = themeSelection.primary)
                                        )
                                    }
                                }
                            }

                            // Section: Theme Changer / System Customization
                            Column {
                                Text("THEME ENGINE / VISUAL OS STYLE (/toggle_theme)", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OSTheme.values().forEach { theme ->
                                        val isSel = themeSelection == theme
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) theme.primary else Color.White.copy(alpha = 0.05f))
                                                .border(BorderStroke(1.dp, if (isSel) Color.White else Color.Transparent), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    themeSelection = theme
                                                    sysPrefs.edit().putString("system_theme", theme.name).apply()
                                                    triggerPushNotification("🌈 Visual berganti ke: ${theme.displayName}")
                                                }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = theme.displayName.split(" ").firstOrNull() ?: "",
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: API Gateways & Credentials Management
                            Column {
                                Text("API GATEWAYS & INTEGRATION SECRETS", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Kelola atau ganti Kredensial API eksternal yang terhubung langsung dengan sistem MeydiAI Core.", color = Color.Gray, fontSize = 10.sp)
                                        
                                        // 1. Gemini Key
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("GEMINI AI KEY", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                val hasCustom = com.example.api.ApiKeyRegistry.hasCustomGeminiKey()
                                                val statusText = if (hasCustom) "Custom Saved / .env" else "Shared Gateway (Fallback)"
                                                val statusColor = if (hasCustom) Color(0xFF00E676) else Color(0xFFFF9100)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(statusText, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = geminiKeyInput,
                                                onValueChange = { geminiKeyInput = it },
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .padding(8.dp),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(themeSelection.primary)
                                            )
                                        }

                                        // 2. Fal Key
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("FAL AI KEY", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                val hasCustom = com.example.api.ApiKeyRegistry.hasCustomFalKey()
                                                val statusText = if (hasCustom) "Custom Saved / .env" else "Shared Gateway (Fallback)"
                                                val statusColor = if (hasCustom) Color(0xFF00E676) else Color(0xFFFF9100)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(statusText, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = falKeyInput,
                                                onValueChange = { falKeyInput = it },
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .padding(8.dp),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(themeSelection.primary)
                                            )
                                        }

                                        // 3. Azbry Key
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("AZBRY DOWNLOADER KEY", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                val hasCustom = com.example.api.ApiKeyRegistry.hasCustomAzbryKey()
                                                val statusText = if (hasCustom) "Custom Saved / .env" else "Shared Gateway (Fallback)"
                                                val statusColor = if (hasCustom) Color(0xFF00E676) else Color(0xFFFF9100)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(statusText, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = azbryKeyInput,
                                                onValueChange = { azbryKeyInput = it },
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .padding(8.dp),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(themeSelection.primary)
                                            )
                                        }

                                        // Save Button
                                        Button(
                                            onClick = {
                                                com.example.api.ApiKeyRegistry.saveGeminiKey(geminiKeyInput)
                                                com.example.api.ApiKeyRegistry.saveFalKey(falKeyInput)
                                                com.example.api.ApiKeyRegistry.saveAzbryKey(azbryKeyInput)
                                                triggerPushNotification("🔐 Kredensial API MeydiAI Berhasil Diperbarui!")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeSelection.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Simpan & Sinkronisasi API Key", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Section: Updater
                            Column {
                                Text("FIRMWARE ENGINE UPDATE (/check_update)", color = themeSelection.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Versi Kernel: $updateBuildVersion", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Tipe Core: Enterprise Microkernel Platform", color = Color.Gray, fontSize = 9.sp)
                                    }
                                    if (isCheckingUpdates) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = themeSelection.primary, strokeWidth = 2.dp)
                                    } else {
                                        Button(
                                            onClick = { executeTerminalCommand("/check_update") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Periksa Update", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // --- DIALOG: TERMINAL CORE SHELL WINDOW ---
        if (showTerminalWindow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .shadow(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF04060C))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C101B))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = themeSelection.primary, modifier = Modifier.size(18.dp))
                                Text("INTERACTIVE SYSTEM SHELL CORE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(onClick = { showTerminalWindow = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Terminal output feed
                        val terminalScroll = rememberScrollState()
                        LaunchedEffect(terminalOutput.size) {
                            terminalScroll.scrollTo(terminalScroll.maxValue)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF030509))
                                .padding(12.dp)
                                .verticalScroll(terminalScroll),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            terminalOutput.forEach { logLine ->
                                Text(
                                    text = logLine,
                                    color = if (logLine.startsWith("❌")) Color.Red else if (logLine.startsWith("✅") || logLine.startsWith("🟢")) Color(0xFF00FF99) else if (logLine.startsWith("meydi@")) themeSelection.primary else Color(0xFFD3D7E0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Input strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF080B13))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "meydi@gemini-core-os:~$ ", 
                                color = themeSelection.primary, 
                                fontSize = 11.sp, 
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextFieldWithConsole(
                                    value = currentCommandText,
                                    onValueChange = { currentCommandText = it },
                                    label = "type /help...",
                                    onSend = {
                                        executeTerminalCommand(currentCommandText)
                                        keyboardController?.hide()
                                    },
                                    cursorColor = themeSelection.primary
                                )
                            }
                            IconButton(
                                onClick = { 
                                    executeTerminalCommand(currentCommandText)
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(28.dp),
                                enabled = currentCommandText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Run", tint = if (currentCommandText.isNotBlank()) themeSelection.primary else Color.DarkGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- REAL-TIME BANNER NOTIFICATION PUSH ---
        AnimatedVisibility(
            visible = showNotificationBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).padding(horizontal = 24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = PanelBackground.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, themeSelection.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().shadow(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(themeSelection.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = themeSelection.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Meydi Core OS System Log", color = themeSelection.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(currentNotifText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- START MENU OVERLAY ---
        if (showStartMenu) {
            StartMenu(
                onClose = { showStartMenu = false },
                onLogout = onLogout,
                isAdmin = isAdmin,
                isOwner = isOwner,
                onNavigateToAdmin = onNavigateToAdmin,
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToOwner = onNavigateToOwner,
                onOpenSettings = {
                    showSettingsWindow = true
                    showStartMenu = false
                },
                onOpenTerminal = {
                    showTerminalWindow = true
                    showStartMenu = false
                }
            )
        }

        // --- TASKBAR SYSTEM MAIN STRIP (At bottom) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(TaskbarColor)
                .border(width = 1.dp, color = TaskbarBorder)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start Menu button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { showStartMenu = !showStartMenu }
                    .background(if (showStartMenu) themeSelection.primary.copy(alpha = 0.22f) else Color.Transparent)
                    .border(1.dp, if (showStartMenu) themeSelection.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DashboardCustomize,
                    contentDescription = "Start OS",
                    tint = themeSelection.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Pinned Core shortcuts (Monitor System)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = { showSettingsWindow = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Control Panel", tint = themeSelection.primary, modifier = Modifier.size(20.dp))
                }

                IconButton(
                    onClick = { showTerminalWindow = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Shell Terminal", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Desktop System Tray Icons
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isIosUser) {
                    Icon(Icons.Default.PhoneIphone, contentDescription = "Sync", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                if (isPremiumLicense) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP Status", tint = themeSelection.primary, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Default.Wifi, contentDescription = "Wifi", tint = Color.White, modifier = Modifier.size(18.dp))
                Icon(Icons.Default.BatteryFull, contentDescription = "Battery", tint = Color.White, modifier = Modifier.size(18.dp))
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Clock Time display
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = currentTime, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = currentDate, color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

// Sub Component: DesktopIcon drawer
@Composable
fun DesktopIcon(app: DesktopApp) {
    Column(
        modifier = Modifier
            .width(85.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { app.onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            app.iconColor.copy(alpha = 0.20f),
                            app.iconColor.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                app.iconColor.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    ),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(app.iconColor.copy(alpha = 0.07f))
            )
            Icon(
                imageVector = app.icon,
                contentDescription = app.name,
                tint = app.iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.name,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// Sub Component: Toggle card settings
@Composable
fun SettingToggleCard(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.025f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
      ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(title, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(desc, color = Color.Gray, fontSize = 9.5.sp, lineHeight = 12.sp)
              }
              Switch(
                  checked = checked,
                  onCheckedChange = onCheckedChange,
                  colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.Black,
                      checkedTrackColor = tint,
                      uncheckedThumbColor = Color.Gray.copy(alpha = 0.8f),
                      uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                      uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                  )
              )
          }
      }
}

// Sub Component: Beautiful Text Field for Console Text Input
@Composable
fun BasicTextFieldWithConsole(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onSend: () -> Unit,
    cursorColor: Color
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = label,
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cursorColor)
        )
    }
}

@Composable
fun StartMenu(
    onClose: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    isOwner: Boolean,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToOwner: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp, start = 16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(490.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFA0B0D18),
                            Color(0xFC05070D)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                PrimaryTheme.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .pointerInput(Unit) { detectTapGestures { /* consume */ } }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Profile Card Section with isolation details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar custom container with Dual Neon Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.sweepGradient(
                                            listOf(
                                                PrimaryTheme,
                                                Color.Transparent,
                                                PrimaryTheme
                                            )
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTheme),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Meydi AI User",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            // Level Badge custom capsule shape
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isOwner) Color(0xFFFFD700).copy(alpha = 0.12f)
                                        else PrimaryTheme.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isOwner) Color(0xFFFFD700).copy(alpha = 0.25f)
                                        else PrimaryTheme.copy(alpha = 0.25f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOwner) "SYSTEM OWNER" else if (isAdmin) "SYS ADMIN" else "STANDARD ACCOUNT",
                                    color = if (isOwner) Color(0xFFFFD700) else PrimaryTheme,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                
                // System Tools List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isOwner) {
                        item { StartMenuItem("Owner Panel", Icons.Default.Security, Color(0xFFFFD700), onNavigateToOwner) }
                    }
                    if (isAdmin) {
                        item { StartMenuItem("Admin Control Panel", Icons.Default.AdminPanelSettings, PrimaryTheme, onNavigateToAdmin) }
                        item { StartMenuItem("Security System Logs", Icons.Default.Lock, PrimaryTheme, onNavigateToSecurity) }
                    }
                    item { StartMenuItem("System Settings Center", Icons.Default.Settings, PrimaryTheme, onOpenSettings) }
                    item { StartMenuItem("Terminal Shell System", Icons.Default.Terminal, Color.LightGray, onOpenTerminal) }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                // Footer Start Menu (Power & Developer Credentials)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = PrimaryTheme.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Created by Meydi",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3366).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Shut Down", tint = Color(0xFFFF3366), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StartMenuItem(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text, 
            color = Color.White.copy(alpha = 0.9f), 
            fontSize = 12.5.sp, 
            fontWeight = FontWeight.Medium
        )
    }
}
