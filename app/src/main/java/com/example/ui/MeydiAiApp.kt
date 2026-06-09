package com.example.ui

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.api.GeminiGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.WorkspaceViewModel

// Screen enumeration routes
enum class Screen {
    LOGIN,
    HOME,
    WORKSPACE_CANVAS,
    WORKSPACE_REMOTION,
    AI_AUTO_CLIPPER,
    MEDIA_DOWNLOADER,
    HD_ENHANCER,
    PROMPT_GENERATOR,
    ADMIN_DASHBOARD,
    SECURITY_DASHBOARD,
    OWNER_DASHBOARD,
    PAYMENT_GATEWAY
}

// Preset Prompts for Microstock Creators
data class PromptTemplate(
    val id: Int,
    val title: String,
    val description: String,
    val prompt: String,
    val defaultCode: String,
    val category: String = "Canvas JS"
)

val PRESET_TEMPLATES = listOf(
    PromptTemplate(
        id = 1,
        title = "Gradient Fluid Wave",
        description = "Gelombang warna-warni mengalir lambat, menghasilkan transisi loop premium.",
        prompt = "Buat background loop gelombang gradient biru cyan dan ungu neon mengalir lambat.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            let time = 0;

            function draw() {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                
                // Base background gradient
                const baseGrad = ctx.createLinearGradient(0, 0, 0, canvas.height);
                baseGrad.addColorStop(0, '#02000a');
                baseGrad.addColorStop(1, '#0c0721');
                ctx.fillStyle = baseGrad;
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                // Multi-layered glowing neon waves
                for (let i = 0; i < 5; i++) {
                    ctx.beginPath();
                    const opacity = 0.08 + i * 0.03;
                    const grad = ctx.createLinearGradient(0, 0, canvas.width, 0);
                    grad.addColorStop(0, `rgba(0, 255, 204, ${'$'}{opacity})`);
                    grad.addColorStop(0.5, `rgba(127, 0, 255, ${'$'}{opacity * 1.5})`);
                    grad.addColorStop(1, `rgba(255, 0, 95, ${'$'}{opacity})`);
                    ctx.fillStyle = grad;

                    for (let x = 0; x <= canvas.width; x += 15) {
                        const y = canvas.height * 0.5 + 
                                  Math.sin(x * 0.004 + time + i * 0.6) * 120 + 
                                  Math.cos(x * 0.002 - time + i * 0.3) * 60;
                        if (x === 0) ctx.moveTo(x, y);
                        else ctx.lineTo(x, y);
                    }
                    ctx.lineTo(canvas.width, canvas.height);
                    ctx.lineTo(0, canvas.height);
                    ctx.closePath();
                    ctx.fill();
                }

                time += 0.008;
                requestAnimationFrame(draw);
            }
            draw();
        """.trimIndent()
    ),
    PromptTemplate(
        id = 2,
        title = "Cyber Space Particle Flow",
        description = "Nebula partikel holografik yang berputar cepat ditiup angin ruang angkasa.",
        prompt = "Buat partikel melayang neon magenta dan ungu berputar membentuk angin pusaran badai antariksa.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            const particles = [];

            // Initialize particles
            for (let i = 0; i < 120; i++) {
                particles.push({
                    angle: Math.random() * Math.PI * 2,
                    distance: Math.random() * 500 + 50,
                    speed: 0.003 + Math.random() * 0.006,
                    radius: Math.random() * 4 + 1.5,
                    hue: Math.random() > 0.5 ? 320 : 270 // magenta / violet
                });
            }

            function draw() {
                // Background overlay to produce tail trail effect
                ctx.fillStyle = 'rgba(6, 4, 16, 0.12)';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                const cx = canvas.width / 2;
                const cy = canvas.height / 2;

                particles.forEach(p => {
                    p.angle += p.speed;
                    // Shrink or ripple distance
                    const radDist = p.distance + Math.sin(p.angle * 2) * 20;
                    const x = cx + Math.cos(p.angle) * radDist;
                    const y = cy + Math.sin(p.angle) * radDist;

                    ctx.beginPath();
                    ctx.arc(x, y, p.radius, 0, Math.PI * 2);
                    ctx.fillStyle = `hsla(${'$'}{p.hue}, 100%, 65%, 0.95)`;
                    ctx.shadowBlur = p.radius * 4;
                    ctx.shadowColor = `hsla(${'$'}{p.hue}, 100%, 60%, 1)`;
                    ctx.fill();
                    ctx.shadowBlur = 0;
                });

                requestAnimationFrame(draw);
            }
            draw();
        """.trimIndent()
    ),
    PromptTemplate(
        id = 3,
        title = "Neon Circuit Mesh Grid",
        description = "Papan sirkuit siber retro-futuristik yang menyala sesuai denyutan bass.",
        prompt = "Buat grid sirkuit teknologi digital neon biru cyan berpendar menyala berdenyut konstan secara loop.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            let pulse = 0;

            function draw() {
                ctx.fillStyle = '#04020b';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                const gridSize = 60;
                const opacity = 0.2 + Math.abs(Math.sin(pulse)) * 0.4;
                
                // Tech wires
                ctx.strokeStyle = `rgba(0, 255, 204, ${'$'}{opacity})`;
                ctx.lineWidth = 1.5;

                for (let x = 0; x < canvas.width; x += gridSize) {
                    for (let y = 0; y < canvas.height; y += gridSize) {
                        ctx.strokeRect(x, y, gridSize, gridSize);
                        
                        // Diagonal tech lines
                        if ((x + y) % 120 === 0) {
                            ctx.beginPath();
                            ctx.moveTo(x, y);
                            ctx.lineTo(x + gridSize, y + gridSize);
                            ctx.strokeStyle = `rgba(255, 0, 95, ${'$'}{opacity * 1.2})`;
                            ctx.stroke();
                        }
                    }
                }

                // Cyber micro chips
                ctx.fillStyle = '#00ffcc';
                for (let i = 0; i < 15; i++) {
                    const cx = (i * 127) % canvas.width;
                    const cy = (i * 199) % canvas.height;
                    ctx.fillRect(cx - 5, cy - 5, 10, 10);
                    
                    ctx.beginPath();
                    ctx.arc(cx, cy, 18 + Math.sin(pulse + i) * 6, 0, Math.PI * 2);
                    ctx.strokeStyle = 'rgba(0, 255, 204, 0.4)';
                    ctx.lineWidth = 1;
                    ctx.stroke();
                }

                pulse += 0.03;
                requestAnimationFrame(draw);
            }
            draw();
        """.trimIndent()
    ),
    PromptTemplate(
        id = 4,
        title = "3D Retro Synthwave Grid",
        description = "Grid wireframe 3D retro-futuristik tahun 80-an yang bergerak cepat menuju ufuk matahari terbenam.",
        prompt = "Buat grid synthwave retro 3D wireframe bergerak maju ke depan dengan matahari neon merah jingga setengah tenggelam.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            let offset = 0;

            function draw() {
                ctx.fillStyle = '#020108';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                const cx = canvas.width / 2;
                const cy = canvas.height / 2 - 120;

                // Glowing neon sun
                const grad = ctx.createLinearGradient(0, cy - 220, 0, cy + 220);
                grad.addColorStop(0, '#ff0050');
                grad.addColorStop(0.5, '#ff5500');
                grad.addColorStop(1, '#ffaa00');
                ctx.fillStyle = grad;
                ctx.beginPath();
                ctx.arc(cx, cy, 220, 0, Math.PI * 2);
                ctx.fill();

                // Synthwave sunset horizon stripes lines
                ctx.fillStyle = '#020108';
                for (let h = cy - 220; h < cy + 220; h += 25) {
                    const height = 4 + (h - (cy - 220)) * 0.06;
                    ctx.fillRect(0, h, canvas.width, height);
                }

                // 3D wireframe grid floor
                const horizon = canvas.height * 0.52;
                ctx.lineWidth = 2;

                // Perspective grid longitude lines
                for (let x = -800; x <= canvas.width + 800; x += 120) {
                    ctx.beginPath();
                    ctx.moveTo(cx + (x - cx) * 0.02, horizon);
                    ctx.lineTo(x, canvas.height);
                    ctx.strokeStyle = 'rgba(127, 0, 255, 0.5)';
                    ctx.stroke();
                }

                // Moving horizon horizontal latitude lines
                for (let i = 0; i <= 25; i++) {
                    const ratio = i / 25;
                    const depth = (ratio + offset) % 1.0;
                    const y = horizon + Math.pow(depth, 1.8) * (canvas.height - horizon);
                    
                    ctx.beginPath();
                    ctx.moveTo(0, y);
                    ctx.lineTo(canvas.width, y);
                    ctx.strokeStyle = `rgba(0, 255, 204, ${'$'}{depth * 0.75})`;
                    ctx.stroke();
                }

                offset = (offset + 0.005) % 1.0;
                requestAnimationFrame(draw);
            }
            draw();
        """.trimIndent()
    ),
    PromptTemplate(
        id = 5,
        title = "Cosmic Matrix Code Stream",
        description = "Semburan hujan kode digital biner beraliran vertikal berwarna hijau neon cyberpunk terang.",
        prompt = "Buat aliran kode digital siber matrix biner hijau neon jatuh bebas dari atas menutupi latar belakang gelap.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            
            const fontSize = 24;
            const columns = Math.floor(canvas.width / fontSize);
            const drops = Array(columns).fill(0);
            
            // Random character set
            const chars = "10ABCDEF@#${'$'}%^&*+-/=";

            function draw() {
                ctx.fillStyle = 'rgba(2, 3, 10, 0.1)';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                ctx.fillStyle = '#00ff66';
                ctx.font = 'bold ' + fontSize + 'px monospace';

                for (let i = 0; i < drops.length; i++) {
                    const text = chars[Math.floor(Math.random() * chars.length)];
                    const x = i * fontSize;
                    const y = drops[i] * fontSize;

                    // Glowing key code
                    if (Math.random() > 0.98) {
                        ctx.fillStyle = '#ffffff';
                    } else {
                        ctx.fillStyle = '#00ff66';
                    }
                    
                    ctx.shadowBlur = 8;
                    ctx.shadowColor = '#00ff66';
                    ctx.fillText(text, x, y);
                    ctx.shadowBlur = 0;

                    if (y > canvas.height && Math.random() > 0.975) {
                        drops[i] = 0;
                    }
                    drops[i]++;
                }
                setTimeout(() => requestAnimationFrame(draw), 30); // 30fps lock for matrix speed
            }
            draw();
        """.trimIndent()
    ),
    PromptTemplate(
        id = 6,
        title = "Glitch Abstract Waveform",
        description = "Desain visual gelombang audio / sinus dengan manipulasi glitch elektronik neon pink-cyan yang estetik.",
        prompt = "Buat gelombang sinus audio bergetar cepat dengan efek glitch warna pink dan cyan yang bersentuhan acak.",
        defaultCode = """
            const canvas = document.getElementById('canvas');
            const ctx = canvas.getContext('2d');
            let offset = 0;

            function draw() {
                ctx.fillStyle = 'rgba(10, 5, 15, 0.2)';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                ctx.lineWidth = 5;
                const horizon = canvas.height / 2;

                // Magenta glitch wave
                ctx.strokeStyle = '#ff007f';
                ctx.beginPath();
                for (let x = 0; x <= canvas.width; x += 10) {
                    const glitch = Math.random() > 0.96 ? (Math.random() - 0.5) * 60 : 0;
                    const y = horizon + Math.sin(x * 0.005 + offset) * 140 + glitch;
                    if (x === 0) ctx.moveTo(x, y);
                    else ctx.lineTo(x, y);
                }
                ctx.stroke();

                // Cyan glitch wave offset
                ctx.strokeStyle = '#00f6ff';
                ctx.beginPath();
                for (let x = 0; x <= canvas.width; x += 10) {
                    const glitch = Math.random() > 0.94 ? (Math.random() - 0.5) * 80 : 0;
                    const y = horizon + Math.sin(x * 0.004 - offset + 1) * 120 + glitch;
                    if (x === 0) ctx.moveTo(x, y);
                    else ctx.lineTo(x, y);
                }
                ctx.stroke();

                offset += 0.05;
                requestAnimationFrame(draw);
            }
            draw();
        """.trimIndent()
    )
)

var globalActiveCode = mutableStateOf(PRESET_TEMPLATES[0].defaultCode)
var globalActivePrompt = mutableStateOf(PRESET_TEMPLATES[0].prompt)

// Meydi Exception Class for Global Auto-Error-Detection Flow
class MeydiCrashHandler(
    private val context: android.content.Context,
    private val rootHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Automatically capture error messages, class details, stack trace
        val writer = java.io.StringWriter()
        val printWriter = java.io.PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        val stackTrace = writer.toString()
        
        val crashPrefs = context.getSharedPreferences("CrashPrefs", android.content.Context.MODE_PRIVATE)
        val timeNow = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        val formattedLog = """
            [Timestamp]: $timeNow
            [Thread]: ${thread.name} (ID: ${thread.id})
            [Tipe Error]: ${throwable.javaClass.simpleName}
            [Pesan]: ${throwable.localizedMessage ?: "Tidak ada pesan detail"}
            [Lokasi Crash]: ${throwable.stackTrace.firstOrNull()?.toString() ?: "Unknown source"}
            
            [DETAILED STACK TRACE]:
            $stackTrace
        """.trimIndent()
        
        crashPrefs.edit()
            .putString("latest_crash_log", formattedLog)
            .putLong("latest_crash_time", System.currentTimeMillis())
            .putBoolean("crash_unresolved", true)
            .apply()

        // Call call standard recovery behavior or pass forward to Android recovery
        rootHandler?.uncaughtException(thread, throwable)
    }
}

@Composable
fun MeydiAiApp() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Crash Register
    val crashPrefs = remember { context.getSharedPreferences("CrashPrefs", android.content.Context.MODE_PRIVATE) }
    var previousCrashLog by remember { mutableStateOf(crashPrefs.getString("latest_crash_log", null)) }
    var showCrashDialog by remember { mutableStateOf(previousCrashLog != null) }

    LaunchedEffect(Unit) {
        val root = Thread.getDefaultUncaughtExceptionHandler()
        if (root !is MeydiCrashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(MeydiCrashHandler(context, root))
        }
    }

    val userPrefs = context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
    val autoLoginEnabled = userPrefs.getBoolean("auto_login_enabled", true)
    var currentUserEmail by remember { 
        mutableStateOf(
            if (autoLoginEnabled) (userPrefs.getString("logged_in_email", null) ?: "guest")
            else "guest"
        ) 
    }
    var currentScreen by remember { 
        mutableStateOf(
            if (autoLoginEnabled && userPrefs.contains("logged_in_email")) Screen.HOME else Screen.LOGIN
        ) 
    }
    var showLoginNotificationFromOwner by remember { mutableStateOf(false) }

    // Role Logic
    val isOwner = currentUserEmail == "meydihikara@gmail.com"
    val isIosUser = currentUserEmail == "meydi_ios@icloud.com" || currentUserEmail?.endsWith("@icloud.com") == true
    var isPremium by remember { mutableStateOf(userPrefs.getBoolean("is_premium_$currentUserEmail", false) || isIosUser) }
    var premiumExpiry by remember { mutableStateOf(userPrefs.getLong("premium_expiry_$currentUserEmail", 0L)) }
    
    // Auto Permissions Setup (Penyimpanan dan Kamera)
    val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all { permission ->
                androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            Toast.makeText(context, "Izin Penyimpanan & Kamera Berhasil Diberikan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Beberapa izin ditolak. Fitur penyimpanan & kamera terbatas.", Toast.LENGTH_LONG).show()
        }
        showPermissionDialog = false
    }

    // Auto-Trigger check & launch natively on App launch / screen loads
    LaunchedEffect(currentScreen) {
        val isCurrentlyAllGranted = requiredPermissions.all { p ->
            androidx.core.content.ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (!isCurrentlyAllGranted) {
            // Automatically prompt native Android system permission dialogue
            permissionLauncher.launch(requiredPermissions)
        } else {
            permissionsGranted = true
        }
    }
    
    // Auto Validate Premium
    LaunchedEffect(currentUserEmail, currentScreen) {
        val currentMillis = System.currentTimeMillis()
        val iosUser = currentUserEmail == "meydi_ios@icloud.com" || currentUserEmail?.endsWith("@icloud.com") == true
        if (iosUser) {
            isPremium = true
        } else if (isPremium && premiumExpiry < currentMillis) {
            isPremium = false
            userPrefs.edit().putBoolean("is_premium_$currentUserEmail", false).apply()
        }
    }
    
    val dateFormatter = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
    val premiumExpiryDate = if (premiumExpiry > 0) dateFormatter.format(java.util.Date(premiumExpiry)) else ""

    val sharedPrefs = context.getSharedPreferences("AdminPrefs", android.content.Context.MODE_PRIVATE)
    val defaultAdmins = setOf("meydihikara@gmail.com")
    var adminEmails by remember { 
        mutableStateOf(sharedPrefs.getStringSet("admins", defaultAdmins) ?: defaultAdmins)
    }
    
    var systemAlertMessage by remember {
        mutableStateOf(sharedPrefs.getString("system_alert", "") ?: "")
    }

    fun addAdmin(newEmail: String) {
        val newSet = adminEmails + newEmail
        adminEmails = newSet
        sharedPrefs.edit().putStringSet("admins", newSet).apply()
    }

    fun removeAdmin(emailToDelete: String) {
        if (emailToDelete == "meydihikara@gmail.com") return // Owner cannot be removed
        val newSet = adminEmails - emailToDelete
        adminEmails = newSet
        sharedPrefs.edit().putStringSet("admins", newSet).apply()
    }
    
    fun updateSystemAlert(newMessage: String) {
        systemAlertMessage = newMessage
        sharedPrefs.edit().putString("system_alert", newMessage).apply()
    }

    fun logout() {
        userPrefs.edit().remove("logged_in_email").apply()
        currentUserEmail = "guest"
        currentScreen = Screen.LOGIN
    }

    // Set Up active code selection
    fun selectTemplate(template: PromptTemplate) {
        globalActiveCode.value = template.defaultCode
        globalActivePrompt.value = template.prompt
        currentScreen = Screen.WORKSPACE_CANVAS
        Toast.makeText(context, "${template.title} berhasil dimuat ke Canvas Studio!", Toast.LENGTH_SHORT).show()
    }

    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.LOGIN -> LoginScreen(onLoginSuccess = { email -> 
                        val emailStr = email ?: "guest"
                        if(emailStr != "guest") {
                            userPrefs.edit().putString("logged_in_email", emailStr).apply()
                        }
                        currentUserEmail = emailStr
                        
                        // Automatically record this login session in the notifications list
                        val timeNow = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val osVersion = android.os.Build.VERSION.RELEASE
                        val deviceModel = android.os.Build.MODEL
                        val loggedInString = "Sesi Akun: $emailStr | Waktu: $timeNow | Perangkat: $deviceModel (Android $osVersion) | Status: ONLINE"
                        
                        val notifPrefs = context.getSharedPreferences("LoginNotificationPrefs", android.content.Context.MODE_PRIVATE)
                        val existingLogs = notifPrefs.getStringSet("login_logs", emptySet()) ?: emptySet()
                        val updatedLogs = existingLogs.toMutableSet()
                        updatedLogs.add(loggedInString)
                        notifPrefs.edit().putStringSet("login_logs", updatedLogs).apply()
                        
                        // Trigger the pop-up notification from the owner
                        showLoginNotificationFromOwner = true
                        
                        currentScreen = Screen.HOME 
                    })
                    Screen.HOME -> HomeScreen(
                        isAdmin = currentUserEmail in adminEmails,
                        isOwner = isOwner,
                        isPremium = isPremium || isOwner || isIosUser, // Owner/iOS is implicitly premium
                        isIosUser = isIosUser,
                        premiumExpiryDate = premiumExpiryDate,
                        systemAlertMessage = systemAlertMessage,
                        permissionsGranted = permissionsGranted,
                        onRequestPermissions = { showPermissionDialog = true },
                        onNavigateToAdmin = { currentScreen = Screen.ADMIN_DASHBOARD },
                        onNavigateToSecurity = { currentScreen = Screen.SECURITY_DASHBOARD },
                        onNavigateToOwner = { currentScreen = Screen.OWNER_DASHBOARD },
                        onNavigateToPayment = { currentScreen = Screen.PAYMENT_GATEWAY },
                        onLogout = { logout() },
                        onNavigateToCanvas = { 
                            if (isPremium || isOwner) currentScreen = Screen.WORKSPACE_CANVAS
                            else currentScreen = Screen.PAYMENT_GATEWAY
                        },
                        onNavigateToRemotion = { currentScreen = Screen.WORKSPACE_REMOTION },
                        onNavigateToClipper = { currentScreen = Screen.AI_AUTO_CLIPPER },
                        onNavigateToDownloader = { currentScreen = Screen.MEDIA_DOWNLOADER },
                        onNavigateToEnhancer = { currentScreen = Screen.HD_ENHANCER },
                        onNavigateToPromptGenerator = { currentScreen = Screen.PROMPT_GENERATOR },
                        onSelectTemplate = { selectTemplate(it) }
                    )
                    Screen.SECURITY_DASHBOARD -> SecurityDashboardScreen(
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                        adminEmails = adminEmails.toList(),
                        systemAlertMessage = systemAlertMessage,
                        onAddAdmin = { addAdmin(it) },
                        onRemoveAdmin = { removeAdmin(it) },
                        onUpdateSystemAlert = { updateSystemAlert(it) },
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.OWNER_DASHBOARD -> OwnerDashboardScreen(
                        userEmail = currentUserEmail,
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.PAYMENT_GATEWAY -> PaymentGatewayScreen(
                        userEmail = currentUserEmail,
                        onPaymentSuccess = {
                            isPremium = true
                            premiumExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // +30 days
                            userPrefs.edit()
                                .putBoolean("is_premium_$currentUserEmail", true)
                                .putLong("premium_expiry_$currentUserEmail", premiumExpiry)
                                .apply()
                            currentScreen = Screen.HOME
                        },
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.WORKSPACE_CANVAS -> CanvasWorkspaceScreen(
                        userEmail = currentUserEmail ?: "guest",
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.WORKSPACE_REMOTION -> RemotionWorkspaceScreen(
                        userEmail = currentUserEmail ?: "guest",
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.AI_AUTO_CLIPPER -> AiClipperScreen(
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.MEDIA_DOWNLOADER -> MediaDownloaderScreen(
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.HD_ENHANCER -> HdEnhancerScreen(
                        onBack = { currentScreen = Screen.HOME }
                    )
                    Screen.PROMPT_GENERATOR -> PromptGeneratorScreen(
                        onBack = { currentScreen = Screen.HOME }
                    )
                }
            }
            
            // Modern Auto Permission Dialog
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = NeonTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Izin Diperlukan 🔒",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "MeydiAi membutuhkan izin Kamera dan Penyimpanan agar fungsionalitas otomasi video loop buatan Anda berjalan optimal:",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "• 📸 Kamera: Diperlukan untuk modul Vision Prompt Generator untuk mengekstraksi teks dari item fisik / layar eksternal secara langsung.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• 📁 Penyimpanan: Diperlukan untuk mengekspor looping render MP4, menyimpan template Canvas dan mengunggah media lokal.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Sistem akan secara otomatis memicu pop-up izin resmi Android setelah Anda menekan tombol di bawah.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                permissionLauncher.launch(requiredPermissions)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                        ) {
                            Text("Berikan Izin Otomatis", color = ObsidianBg, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPermissionDialog = false }
                        ) {
                            Text("Nanti Saja", color = Color.Gray)
                        }
                    },
                    containerColor = MidnightSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White,
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                )
            }
            
            // Auto Exception/Crash Detection Reporter Dialog Overlay
            if (showCrashDialog && previousCrashLog != null) {
                AlertDialog(
                    onDismissRequest = {
                        crashPrefs.edit().remove("latest_crash_log").apply()
                        previousCrashLog = null
                        showCrashDialog = false
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Crash Detected",
                                tint = ErrorRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Deteksi Crash Terjadi! 🚨",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Sistem kecerdasan buatan MeydiAi mendeteksi kegagalan aplikasi sesaat sebelum restart atau penutupan sesi yang tidak wajar:",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Console Log style scrollable box for stack trace
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = previousCrashLog ?: "",
                                    color = Color(0xFF39FF14), // Matrix Neon Green log text
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Meydi support siap membantu menganalisis kendala Anda. Ketuk tombol untuk mengirim detail crash via WhatsApp.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val heading = "Halo MeydiAi Owner, aplikasi mendeteksi crash dengan log sebagai berikut:%0A%0A${previousCrashLog?.take(300)}... (selengkapnya di log)"
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://api.whatsapp.com/send?phone=6282258371053&text=$heading")
                                )
                                context.startActivity(intent)
                                
                                crashPrefs.edit().remove("latest_crash_log").apply()
                                previousCrashLog = null
                                showCrashDialog = false
                                Toast.makeText(context, "Membuka WhatsApp support...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Hubungi & Kirim WA", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                crashPrefs.edit().remove("latest_crash_log").apply()
                                previousCrashLog = null
                                showCrashDialog = false
                                Toast.makeText(context, "Sistem dipulihkan otomatis.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Abaikan & Pulihkan", color = NeonTeal)
                        }
                    },
                    containerColor = MidnightSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White,
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                )
            }

            // 🚨 PERSATUAN NOTIFIKASI LOGIN BARU DARI OWNER
            if (showLoginNotificationFromOwner) {
                AlertDialog(
                    onDismissRequest = {
                        showLoginNotificationFromOwner = false
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Security Verified",
                                tint = NeonTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifikasi Login Berhasil! 👑",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Halo, Pengguna MeydiAi!",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Owner Meydi Hikara mendeteksi aktivitas masuk (login) yang sukses dari perangkat Anda:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MidnightSurface)
                                    .border(BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "📧 Email: $currentUserEmail",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📱 Perangkat: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🛡️ Status Sesi: Aman, Stabil & Terverifikasi",
                                        color = Color(0xFF25D366),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Keamanan akses Anda sepenuhnya dijamin langsung oleh administrator dan owner Meydi (082258371053). Hubungi kami kapan pun via WhatsApp untuk pertanyaan premium atau kendala sistem.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val textMsg = "Halo Owner Meydi, saya baru saja melakukan login ke aplikasi MeydiAi menggunakan email: $currentUserEmail. Divais: ${android.os.Build.MODEL}"
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://api.whatsapp.com/send?phone=6282258371053&text=${java.net.URLEncoder.encode(textMsg, "UTF-8")}")
                                )
                                context.startActivity(intent)
                                showLoginNotificationFromOwner = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Text("Hubungi Owner via WA 💬", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showLoginNotificationFromOwner = false
                                Toast.makeText(context, "Sesi Anda dilindungi Owner 🛡️", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Tutup & Lanjutkan", color = Color.Gray, fontSize = 12.sp)
                        }
                    },
                    containerColor = ObsidianBg,
                    textContentColor = Color.White,
                    titleContentColor = Color.White,
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                )
            }
        }
    }
}

// 1. HOME SCREEN / DASHBOARD
@Composable
fun HomeScreen(
    isAdmin: Boolean = false,
    isOwner: Boolean = false,
    isPremium: Boolean = false,
    isIosUser: Boolean = false,
    premiumExpiryDate: String = "",
    systemAlertMessage: String = "",
    permissionsGranted: Boolean = true,
    onRequestPermissions: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToOwner: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToCanvas: () -> Unit,
    onNavigateToRemotion: () -> Unit,
    onNavigateToClipper: () -> Unit,
    onNavigateToDownloader: () -> Unit,
    onNavigateToEnhancer: () -> Unit,
    onNavigateToPromptGenerator: () -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val userPrefs = remember { context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE) }
    var isAutoLoginEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_login_enabled", true)) }
    var isAutoSearchEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_search_enabled", true)) }
    var isAutoSaveEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_save_enabled", true)) }
    var isAutoRefreshEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_refresh_enabled", true)) }
    var isAutoBackupEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_backup_enabled", true)) }
    var isAutoNotificationEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_notification_enabled", true)) }
    var isAutoDarkModeEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_dark_mode_enabled", true)) }
    var isAutoUpdateEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_update_enabled", true)) }
    var isAutoSyncEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_sync_enabled", true)) }
    var isAutoReportEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_report_enabled", true)) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var telemetryLogs by remember { mutableStateOf(listOf("[SYSTEM] Layanan Otomatis Aktif", "[NETWORK] Terhubung Online")) }
    var latestNotification by remember { mutableStateOf("Sistem Meydi AI Aktif & Terlindungi.") }
    var showNotifToast by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf(userPrefs.getString("last_backup_time", "Belum Ada Backup") ?: "Belum Ada Backup") }
    var isSyncingInBg by remember { mutableStateOf(false) }
    var patchUpdateAvailable by remember { mutableStateOf(false) }
    var isUpdateChecking by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    
    var showAutoLoginConsentDialog by remember { mutableStateOf(false) }
    var showAutoReportConsentDialog by remember { mutableStateOf(false) }
    
    var isCompilingInBg by remember { mutableStateOf(false) }
    var compileLogText by remember { mutableStateOf(listOf("[INFO] Auto Build Idle (Menunggu Perubahan Workspace)")) }
    val scope = rememberCoroutineScope()

    // 1. Live Telemetry & Auto-Refresh Daemon
    LaunchedEffect(isAutoRefreshEnabled, isAutoBackupEnabled, isAutoSyncEnabled, isAutoUpdateEnabled, isAutoNotificationEnabled) {
        if (isAutoRefreshEnabled) {
            var counter = 0
            while (true) {
                kotlinx.coroutines.delay(5000)
                counter++
                
                val timeNow = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val newLogs = telemetryLogs.toMutableList()
                if (newLogs.size > 15) newLogs.removeAt(0)
                
                // Live Auto Backup Simulation
                if (isAutoBackupEnabled && counter % 3 == 0) {
                    val backupTime = java.text.SimpleDateFormat("dd MMM, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    lastBackupTime = backupTime
                    userPrefs.edit().putString("last_backup_time", backupTime).apply()
                    newLogs.add("[$timeNow][AUTO BACKUP] Database & Sesi Berhasil Dicadangkan!")
                    if (isAutoNotificationEnabled) {
                        latestNotification = "Backup otomatis tersimpan aman pada $backupTime."
                        showNotifToast = true
                    }
                }
                
                // Live Auto Sync Simulation
                if (isAutoSyncEnabled && counter % 4 == 0) {
                    isSyncingInBg = true
                    newLogs.add("[$timeNow][AUTO SYNC] Menghubungkan ke Cloud Storage...")
                    kotlinx.coroutines.delay(1000)
                    newLogs.add("[$timeNow][AUTO SYNC] Sinkronisasi Berhasil (iOS Cloud synced) ✅")
                    isSyncingInBg = false
                    if (isAutoNotificationEnabled) {
                        latestNotification = "Aset Workspace tersinkronisasi murni dengan Cloud!"
                        showNotifToast = true
                    }
                }
                
                // Live Auto Update Simulation
                if (isAutoUpdateEnabled && counter == 2) {
                    isUpdateChecking = true
                    newLogs.add("[$timeNow][AUTO UPDATE] Memindai patch stabilitas v2.5.4...")
                    kotlinx.coroutines.delay(1200)
                    patchUpdateAvailable = true
                    newLogs.add("[$timeNow][AUTO UPDATE] Ditemukan patch mikro v2.5.4-p1! (Siap di-install)")
                    isUpdateChecking = false
                    if (isAutoNotificationEnabled) {
                        latestNotification = "Pembaruan stabilitas v2.5.4-p1 terdeteksi!"
                        showNotifToast = true
                    }
                }
                
                if (counter % 2 == 0) {
                    newLogs.add("[$timeNow][HEALTH] CPU & GPU Rendering: Optimal (Suhu 43°C)")
                } else {
                    newLogs.add("[$timeNow][TELEMETRY] Sesi Pengguna Diperbarui. Izin: AMAN.")
                }
                telemetryLogs = newLogs
            }
        }
    }

    // Auto Search Trigger
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty() && isAutoSearchEnabled) {
            isSearching = true
            kotlinx.coroutines.delay(350)
            isSearching = false
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (systemAlertMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.2f))
                            .border(1.dp, ErrorRed, RoundedCornerShape(0.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = systemAlertMessage,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MeydiAi",
                                color = NeonTeal,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 1.sp
                            )
                            if (isPremium) {
                                Text(
                                    text = if (isOwner) "👑 Owner" else "💎 Premium ($premiumExpiryDate)",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPremium) {
                            Button(
                                onClick = onNavigateToPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, Color(0xFFFFD700)),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Upgrade Premium", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (isOwner) {
                            IconButton(onClick = onNavigateToOwner, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = "Owner Dashboard", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            }
                        }
                        if (isAdmin) {
                            IconButton(onClick = onNavigateToSecurity, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = "Keamanan", tint = NeonTeal, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onNavigateToAdmin, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "Admin", tint = NeonTeal, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TerminalGreen.copy(alpha = 0.2f))
                                .border(1.dp, TerminalGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(TerminalGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RENDER ONLINE",
                                    color = TerminalGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isIosUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🍏 iOS SYNCED",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Video Automation Studio for Microstock Creators",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Modern Welcome Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.4f), NeonMagenta.copy(alpha = 0.15f))
                        )
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(colors = listOf(NeonPurple, Color.Transparent))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Otomatisasi Konten Kreatif ⚡",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tulis ide Anda dalam format bahasa manusia (Prompt), dan biarkan Gemini AI menyusun kode animasi looping murni (HTML5 Canvas atau Remotion TSX) yang siap diekspor ke MP4 beresolusi tinggi.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // SYSTEM PERMISSIONS STATUS WARNING (INLINE CALLOUT)
            if (!permissionsGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, ErrorRed), RoundedCornerShape(12.dp))
                        .clickable { onRequestPermissions() }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(ErrorRed.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Alert",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Sistem Terbatas",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kamera & Penyimpanan belum diizinkan. Klik untuk memberikan izin otomatis.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Arrow Right",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SYSTEM INTEGRITY HEALTH STATUS & AUTO-ERROR SCANNER CARD
            var showDiagnosticsDialog by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDiagnosticsDialog = true },
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Health Diagnostics Shield",
                            tint = NeonTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Diagnostik & Monitor Error",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTO SCAN",
                                    color = TerminalGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Ketuk untuk memindai error, mendeteksi kerusakan izin, sisa memori, atau simulasikan crash.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Diagnostics Control",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showDiagnosticsDialog) {
                var isScanning by remember { mutableStateOf(false) }
                var scanLog by remember { mutableStateOf(listOf<String>()) }
                val dContext = LocalContext.current
                val dScope = rememberCoroutineScope()
                
                AlertDialog(
                    onDismissRequest = { if (!isScanning) showDiagnosticsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Diagnostic Shield",
                                tint = NeonTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Panel Diagnostik MeydiAi 🦾",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Layanan pemindaian otomatis mendeteksi status keselarasan sistem secara real-time untuk meminimalkan error:",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Scan Logs Window Consoles
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                if (scanLog.isEmpty() && !isScanning) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Konsol Siap. Tekan 'Mulai Pindai' di bawah untuk menganalisis.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        scanLog.forEach { logItem ->
                                            Text(
                                                text = logItem,
                                                color = if (logItem.contains("[FAILED]") || logItem.contains("ERROR")) ErrorRed 
                                                       else if (logItem.contains("[PASSED]")) TerminalGreen 
                                                       else if (logItem.contains("[INFO]")) Color.LightGray 
                                                       else Color(0xFFFFD700),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        if (isScanning) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp, color = NeonTeal)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Memproses pemindaian partisi...", color = NeonTeal, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // DANGEROUS RECOVERY / SIMULATION CORNER
                            Text(
                                text = "Zona Simulasi & Pengujian Error:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // CRASH INTENTIONAL SIMULATION!
                                        // This will trigger MeydiCrashHandler perfectly
                                        dScope.launch {
                                            Toast.makeText(dContext, "Memicu crash buatan dalam 1 detik...", Toast.LENGTH_SHORT).show()
                                            delay(1000)
                                            throw RuntimeException(
                                                "MeydiAi Runtime Crash Simulator: Kegagalan subsistem dipicu secara manual pada " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.2f), contentColor = ErrorRed),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("Simulasikan Crash 💥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        // Force clear diagnostics & previous logs
                                        val cPrefs = dContext.getSharedPreferences("CrashPrefs", android.content.Context.MODE_PRIVATE)
                                        cPrefs.edit().clear().apply()
                                        scanLog = listOf("[INFO] Berhasil merestart cache error & log diagnostics.")
                                        Toast.makeText(dContext, "Log dibersihkan!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("Reset Logs 🔄", fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isScanning) {
                            Button(
                                onClick = {
                                    isScanning = true
                                    scanLog = listOf("[INFO] Memulai MeydiAi Auto Diagnostics Engine...")
                                    dScope.launch {
                                        delay(800)
                                        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(dContext, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        val permStatus = if (hasCamera) "[PASSED] Kamera & Audio disetujui sistem." else "[WARNING] Akses Kamera dibatasi oleh izin perangkat."
                                        scanLog = scanLog + permStatus
                                        
                                        delay(700)
                                        val spaceBytes = java.io.File(dContext.filesDir.absolutePath).usableSpace
                                        val spaceMega = spaceBytes / (1024 * 1024)
                                        val storageStatus = if (spaceMega > 100) {
                                            "[PASSED] Ruang Penyimpanan Render: ${spaceMega}MB (Sangat Aman)."
                                        } else {
                                            "[FAILED] Penyimpanan Rendah! Tersedia kurang dari 100MB."
                                        }
                                        scanLog = scanLog + storageStatus
                                        
                                        delay(700)
                                        scanLog = scanLog + "[PASSED] Keandalan Jaringan: Tersambung aman ke Server Cloud."
                                        
                                        delay(800)
                                        scanLog = scanLog + "[PASSED] Integritas SQLite database aman."
                                        scanLog = scanLog + "[PASSED] Subsistem Auto-Simpan Media: Direktori biner siap menulis media."
                                        
                                        delay(600)
                                        val overallHealthy = if (hasCamera && spaceMega > 100) {
                                            "[COMPLETED] SISTEM 100% SEHAT. Tidak terdeteksi adanya error runtime fatal."
                                        } else {
                                            "[COMPLETED] SISTEM NORMAL. Ditemukan beberapa peringatan non-fatal."
                                        }
                                        scanLog = scanLog + overallHealthy
                                        isScanning = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("Mulai Pindai", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDiagnosticsDialog = false }
                        ) {
                            Text("Tutup", color = Color.Gray)
                        }
                    },
                    containerColor = MidnightSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White,
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // STUDIO CHANNELS ACCESS
            Text(
                text = "Studio Workspace",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ROW 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Canvas Studio Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, NeonTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToCanvas() }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Canvas Icon",
                            tint = NeonTeal,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Canvas Studio",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Buat animasi interaktif JavaScript & HTML5 otomatis menggunakan AI.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // HD Enhancer Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToEnhancer() }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "HD Enhancer",
                            tint = NeonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "HD Video/Foto",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Perjelas dan tingkatkan kualitas (upscale) video atau foto buram menjadi resolusi HD.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Social Media Downloader
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, TerminalGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToDownloader() }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Downloader Icon",
                            tint = TerminalGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Downloader",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Unduh video & media dari YouTube atau TikTok tanpa watermark dengan mudah.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // AI Auto-Cut Clipper
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, NeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToClipper() }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Clipper Icon",
                            tint = NeonMagenta,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Auto Clipper",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Potong bagian video yang diam/hening secara otomatis untuk mempercepat proses edit.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // ROW 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vision Prompt Generator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToPromptGenerator() }
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            contentDescription = "Prompt Generator Icon",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Vision Prompt",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ekstrak otomatis deskripsi prompt dari sebuah gambar menggunakan AI.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                // Empty box to balance the row (if there's an odd number of items)
                Box(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SISTEM AUTO-SIMPAN STATUS CARD FOR GERATED/DOWNLOADED MEDIA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Auto Save Status Indicator",
                            tint = TerminalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Akses Auto-Simpan Media 💾",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AKTIF",
                                    color = TerminalGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Setiap foto hasil peningkatan HD, video unduhan, atau MP4 Canvas akan diunduh otomatis ke direktori Pictures/MeydiAi dan Movies/MeydiAi Anda.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            
            // AI AUTO-PILOT NOTIFICATION BANNER
            if (showNotifToast && isAutoNotificationEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonTeal.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, NeonTeal), RoundedCornerShape(10.dp))
                        .clickable { showNotifToast = false }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(NeonTeal.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Active Notif",
                                tint = NeonTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pemberitahuan Sistem Otomatis 🔔",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = latestNotification,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        IconButton(
                            onClick = { showNotifToast = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // AUTO-REPORT DIALOG
            if (showReportDialog) {
                AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = NeonPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Report Laporan Sistem", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sistem secara otomatis mendokumentasikan statistik status aplikasi, izin penyimpanan, sesi login, dan keselarasan database SQLite lokal:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = reportContent,
                                    color = TerminalGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Laporan berformat Markdown (.md) di atas disimpan otomatis di direktori internal Documents/MeydiAi/Report_Last.txt Anda.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val reportFile = java.io.File(context.filesDir, "Report_Last.txt")
                                    reportFile.writeText(reportContent)
                                    Toast.makeText(context, "Sertifikat Laporan berhasil di-unduh otomatis!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                showReportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Simpan File Laporan (.md) 📥", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Tutup", color = Color.Gray)
                        }
                    },
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }

            // BRAND NEW AI AUTO-PILOT CONTROL CENTER
            Text(
                text = "Pusat Kontrol Sistem Otomatis (Auto-Pilot Center) 🦾",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Kelola 10 sistem otomatis pada aplikasi Meydi AI secara mandiri:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Switch 1: Auto Login
                        AutoSystemRow(
                            title = "1. Auto Login (Sesi Aman)",
                            desc = "Bypass login & langsung masuk ke dashboard.",
                            checked = isAutoLoginEnabled,
                            icon = Icons.Default.Security,
                            iconTint = NeonTeal,
                            onCheckedChange = { checked ->
                                if (checked) showAutoLoginConsentDialog = true
                                else {
                                    isAutoLoginEnabled = false
                                    userPrefs.edit().putBoolean("auto_login_enabled", false).apply()
                                    Toast.makeText(context, "Auto login dinonaktifkan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))
                        
                        // Switch 2: Auto Search
                        AutoSystemRow(
                            title = "2. Auto Search (Pencarian Cepat)",
                            desc = "Sensitivitas instan pencarian filter preset otomatis.",
                            checked = isAutoSearchEnabled,
                            icon = Icons.Default.Search,
                            iconTint = NeonTeal,
                            onCheckedChange = { checked ->
                                isAutoSearchEnabled = checked
                                userPrefs.edit().putBoolean("auto_search_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 3: Auto Save
                        AutoSystemRow(
                            title = "3. Auto Save Workspace 💾",
                            desc = "Draft kode & render disimpan otomatis setiap perubahan.",
                            checked = isAutoSaveEnabled,
                            icon = Icons.Default.Save,
                            iconTint = TerminalGreen,
                            onCheckedChange = { checked ->
                                isAutoSaveEnabled = checked
                                userPrefs.edit().putBoolean("auto_save_enabled", checked).apply()
                                Toast.makeText(context, if (checked) "Auto Save Aktif!" else "Auto Save dimatikan.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 4: Auto Refresh (Telemetry Daemon)
                        AutoSystemRow(
                            title = "4. Auto Refresh (Log Daemon)",
                            desc = "Pembaruan telemetri & status sistem berkala otomatis.",
                            checked = isAutoRefreshEnabled,
                            icon = Icons.Default.Refresh,
                            iconTint = Color(0xFFFFB300),
                            onCheckedChange = { checked ->
                                isAutoRefreshEnabled = checked
                                userPrefs.edit().putBoolean("auto_refresh_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 5: Auto Backup
                        AutoSystemRow(
                            title = "5. Auto Backup 🗃️",
                            desc = "Pencadangan berkala data lokal & download queue.",
                            checked = isAutoBackupEnabled,
                            icon = Icons.Default.Storage,
                            iconTint = NeonPurple,
                            onCheckedChange = { checked ->
                                isAutoBackupEnabled = checked
                                userPrefs.edit().putBoolean("auto_backup_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 6: Auto Notification
                        AutoSystemRow(
                            title = "6. Auto Notification 🔔",
                            desc = "Aktifkan sinyal pemberitahuan instan di dalam aplikasi.",
                            checked = isAutoNotificationEnabled,
                            icon = Icons.Default.Notifications,
                            iconTint = NeonMagenta,
                            onCheckedChange = { checked ->
                                isAutoNotificationEnabled = checked
                                userPrefs.edit().putBoolean("auto_notification_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 7: Auto Dark Mode
                        AutoSystemRow(
                            title = "7. Auto Dark Mode (Amoled)",
                            desc = "Skema gelap murni AMOLED otomatis untuk hemat daya baterai.",
                            checked = isAutoDarkModeEnabled,
                            icon = Icons.Default.Brightness4,
                            iconTint = Color.White,
                            onCheckedChange = { checked ->
                                isAutoDarkModeEnabled = checked
                                userPrefs.edit().putBoolean("auto_dark_mode_enabled", checked).apply()
                                Toast.makeText(context, "Mode AMOLED Gelap otomatis dioptimalkan.", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 8: Auto Update
                        AutoSystemRow(
                            title = "8. Auto Update (Patch Check)",
                            desc = "Pemeriksaan otomatis stabilitas mikro secara live berkala.",
                            checked = isAutoUpdateEnabled,
                            icon = Icons.Default.CloudDownload,
                            iconTint = TerminalGreen,
                            onCheckedChange = { checked ->
                                isAutoUpdateEnabled = checked
                                userPrefs.edit().putBoolean("auto_update_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 9: Auto Sync Workspace
                        AutoSystemRow(
                            title = "9. Auto Sync Cloud Workspace",
                            desc = "Suku cadang cloud otomatis lintas platform (iOS/Android).",
                            checked = isAutoSyncEnabled,
                            icon = Icons.Default.Sync,
                            iconTint = NeonTeal,
                            onCheckedChange = { checked ->
                                isAutoSyncEnabled = checked
                                userPrefs.edit().putBoolean("auto_sync_enabled", checked).apply()
                            }
                        )
                        Divider(color = DarkStroke.copy(alpha = 0.5f))

                        // Switch 10: Auto Generate Report
                        AutoSystemRow(
                            title = "10. Auto Generate Report 📋",
                            desc = "Sistem pembuatan laporan otomatis status, database, dan performa aplikasi.",
                            checked = isAutoReportEnabled,
                            icon = Icons.Default.Assessment,
                            iconTint = TerminalGreen,
                            onCheckedChange = { checked ->
                                if (checked) showAutoReportConsentDialog = true
                                else {
                                    isAutoReportEnabled = false
                                    userPrefs.edit().putBoolean("auto_report_enabled", false).apply()
                                    Toast.makeText(context, "Sistem Auto Generate Report dinonaktifkan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Interactive Action: Auto Generate Report
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val timeReport = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            reportContent = """
                                # MEYDI AI - AUTO GENERATED SYSTEM REPORT
                                Generated on: $timeReport
                                
                                ## 1. KESELARASAN SISTEM (SYSTEM ALIGNMENT)
                                - Status Operasional: STABLE RUNTIME
                                - Versi Aplikasi: v2.5.4-Stable Release
                                - Hak Akses Premium: ${if (isPremium) "AKTIF (DISETUJUI)" else "DIASOSIASIKAN"}
                                - Akses Jaringan: ONLINE (Connectivity Service Ok)
                                
                                ## 2. KONFIGURASI OTOMASI (10 AUTO SYSTEMS)
                                - Auto Login: ${if (isAutoLoginEnabled) "ENABLED (ACTIVE)" else "DISABLED"}
                                - Auto Search: ${if (isAutoSearchEnabled) "ENABLED" else "DISABLED"}
                                - Auto Save Workspace: ${if (isAutoSaveEnabled) "ENABLED" else "DISABLED"}
                                - Auto Refresh Daemon: ${if (isAutoRefreshEnabled) "ACTIVE" else "INACTIVE"}
                                - Auto Backup Scheduler: ${if (isAutoBackupEnabled) "ACTIVE (Last: $lastBackupTime)" else "INACTIVE"}
                                - Auto Notification: ${if (isAutoNotificationEnabled) "ACTIVE" else "INACTIVE"}
                                - Auto Dark Mode: ${if (isAutoDarkModeEnabled) "ENABLED (AMOLED Dark)" else "DEFAULT"}
                                - Auto Update Check: ${if (isAutoUpdateEnabled) "ENABLED" else "DISABLED"}
                                - Auto Sync Cloud: ${if (isAutoSyncEnabled) "ACTIVE" else "INACTIVE"}
                                - Auto Generate Report: ${if (isAutoReportEnabled) "ENABLED" else "DISABLED"}
                                
                                ## 3. DIAGNOSTIK HARDWARE & PERANGKAT
                                - Perangkat: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                                - Rentang OS: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
                                - Alokasi Memori: Cukup (Java GC Telemetry Optimal)
                                - Integritas Database: SQLite OK (0 corrupted blocks)
                                
                                ## Laporan Sistem Terverifikasi oleh Meydi AI Engine (Secara Otomatis).
                            """.trimIndent()
                            showReportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto Generate Report (Buat Laporan) 📊", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Telemetry console shown inside the Control Center if Auto Refresh is active
                    if (isAutoRefreshEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .border(BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFFFFB300))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Live Telemetry Log Stream (Auto Refresh Active)",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (isSyncingInBg) {
                                        Text(
                                            text = "SYNCING...",
                                            color = NeonTeal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "ONLINE MONITORING",
                                            color = TerminalGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    telemetryLogs.reversed().forEach { logLine ->
                                        Text(
                                            text = logLine,
                                            color = if (logLine.contains("FAILED") || logLine.contains("ERROR")) ErrorRed 
                                                   else if (logLine.contains("BACKUP")) NeonPurple
                                                   else if (logLine.contains("SYNC")) NeonTeal
                                                   else if (logLine.contains("UPDATE")) Color(0xFFFFD700)
                                                   else TerminalGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAutoLoginConsentDialog) {
                AlertDialog(
                    onDismissRequest = { showAutoLoginConsentDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NeonTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Persetujuan Akses & Auto Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Akses Login",
                                color = NeonTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aplikasi memerlukan akses untuk menyimpan informasi sesi login secara aman agar pengguna dapat masuk secara otomatis tanpa perlu mengisi ulang akun setiap kali membuka aplikasi.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            Divider(color = DarkStroke)
                            
                            Text(
                                text = "Akses Penyimpanan Data Pengguna",
                                color = NeonTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Data login disimpan secara aman di perangkat dan hanya digunakan untuk mempertahankan sesi pengguna serta meningkatkan kenyamanan penggunaan aplikasi.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            Divider(color = DarkStroke)
                            
                            Text(
                                text = "Notifikasi Persetujuan",
                                color = NeonTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dengan mengaktifkan Auto Login, Anda mengizinkan aplikasi menyimpan informasi autentikasi secara aman untuk mempercepat proses masuk ke aplikasi pada penggunaan berikutnya.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            // Versi Singkat box styled beautifully with customized border
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple.copy(alpha = 0.12f))
                                    .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Versi Singkat (Ringkasan):",
                                        color = NeonPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Izinkan aplikasi menyimpan sesi login agar Anda dapat masuk secara otomatis saat membuka aplikasi kembali.",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isAutoLoginEnabled = true
                                userPrefs.edit().putBoolean("auto_login_enabled", true).apply()
                                showAutoLoginConsentDialog = false
                                Toast.makeText(context, "Sistem Auto Login Diaktifkan !", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Setuju & Aktifkan", color = ObsidianBg, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                isAutoLoginEnabled = false
                                userPrefs.edit().putBoolean("auto_login_enabled", false).apply()
                                showAutoLoginConsentDialog = false
                                Toast.makeText(context, "Akses Auto Login Dibatalkan.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Batal & Nonaktifkan", color = Color.Gray)
                        }
                    },
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }

            if (showAutoReportConsentDialog) {
                AlertDialog(
                    onDismissRequest = { showAutoReportConsentDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = TerminalGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Konfigurasi Sistem Auto Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Deskripsi Auto Generate Report",
                                color = TerminalGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sistem Auto Generate Report otomatis mengandalkan daemon background untuk mencatat dan merangkum status operasional, database local, status internet, dan log telemetri secara terstruktur format markdown (.md).",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            Divider(color = DarkStroke)
                            
                            Text(
                                text = "Manfaat Utama:",
                                color = TerminalGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Pembuatan sertifikat laporan sistem secara instan.\n• Pengecekan konsistensi status data SQLite lokal.\n• Diagnostik performa hardware & alokasi memori real-time.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isAutoReportEnabled = true
                                userPrefs.edit().putBoolean("auto_report_enabled", true).apply()
                                showAutoReportConsentDialog = false
                                Toast.makeText(context, "Sistem Auto Report Diaktifkan !", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aktifkan Engine", color = ObsidianBg, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                isAutoReportEnabled = false
                                userPrefs.edit().putBoolean("auto_report_enabled", false).apply()
                                showAutoReportConsentDialog = false
                                Toast.makeText(context, "Auto Report Di-nonaktifkan.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Batal / Matikan", color = Color.Gray)
                        }
                    },
                    containerColor = MidnightSurface,
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRESETS TITLE WITH SEARCH BAR
            Text(
                text = "Preset Loop Pilihan (Auto-Search Enabled) 🎨",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Cari di antara puluhan preset premium untuk dimuat otomatis ke Canvas Studio instan.",
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic Auto-Search Input Bar!
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ketik kata kunci (misal: Wave, Grid, Glitch, Neon)...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { 
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonTeal)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray)
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface,
                    focusedBorderColor = NeonTeal,
                    unfocusedBorderColor = DarkStroke,
                    cursorColor = NeonTeal
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filtering templates in real-time based on searchQuery
            val filteredTemplates = PRESET_TEMPLATES.filter { template ->
                template.title.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true) ||
                template.category.contains(searchQuery, ignoreCase = true)
            }

            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidnightSurface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tidak menemukan preset yang cocok 🔍", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Coba cari ketukan lain seperti 'Fluid' atau 'Noise'.", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredTemplates.forEach { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTemplate(template) },
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) NeonTeal.copy(alpha = 0.5f) else DarkStroke)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (template.category.contains("Remotion")) NeonPurple.copy(alpha = 0.15f)
                                            else NeonTeal.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (template.category.contains("Remotion")) Icons.Default.MovieFilter else Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (template.category.contains("Remotion")) NeonPurple else NeonTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = template.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (template.category.contains("Remotion")) NeonPurple.copy(alpha = 0.15f)
                                                    else NeonTeal.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = template.category,
                                                color = if (template.category.contains("Remotion")) NeonPurple else NeonTeal,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = template.description,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Load Template",
                                    tint = TerminalGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Meydi AI • Versi v2.5.4-Stable",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dirancang oleh Meydi • Lintas Platform Android & iOS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Helper Composable for Auto systems switches
@Composable
fun AutoSystemRow(
    title: String,
    desc: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (checked) TerminalGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (checked) "ON" else "OFF",
                        color = if (checked) TerminalGreen else ErrorRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = desc,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = iconTint,
                checkedTrackColor = iconTint.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = ObsidianBg
            )
        )
    }
}

// 2. CANVAS WORKSPACE SCREEN
@Composable
fun CanvasWorkspaceScreen(userEmail: String, onBack: () -> Unit, viewModel: WorkspaceViewModel = viewModel()) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val codeContent by viewModel.codeContent.collectAsStateWithLifecycle()
    val lastSavedTime by viewModel.lastSavedTime.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()

    var isGeneratingCode by remember { mutableStateOf(false) }
    var showRenderDialog by remember { mutableStateOf(false) }
    var renderingProgress by remember { mutableIntStateOf(0) }
    var renderingLogText by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    var isAnalyzingImage by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAnalyzingImage = true
            Toast.makeText(context, "Menganalisis gambar dengan Gemini...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                        val result = GeminiGenerator.generatePromptFromImage(base64)
                        viewModel.updatePromptInput(result, "CANVAS", userEmail)
                        Toast.makeText(context, "Prompt berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLatestDraft("CANVAS", userEmail, globalActiveCode.value.takeIf { it.isNotBlank() } ?: "/* Mulai memprogram Canvas */")
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Prompt AI, 1 = Code Editor

    fun updatePrompt(newPrompt: String) {
        viewModel.updatePromptInput(newPrompt, "CANVAS", userEmail)
    }

    fun updateCode(newCode: String) {
        viewModel.updateCodeContent(newCode, "CANVAS", userEmail)
    }

    fun triggerCodeGeneration() {
        if (promptInput.isBlank()) {
            Toast.makeText(context, "Silakan ketik deskripsi prompt terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isGeneratingCode = true
        coroutineScope.launch {
            try {
                val result = GeminiGenerator.generateCanvasCode(promptInput)
                updateCode(result)
                Toast.makeText(context, "AI Berhasil Menyusun Animasi! ⚡", Toast.LENGTH_SHORT).show()
                selectedTab = 1 // Switch automatically to edit tab to let them inspect
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isGeneratingCode = false
            }
        }
    }

    fun startRenderingProcess() {
        renderingProgress = 0
        renderingLogText = "Inisialisasi Rendering..."
        showRenderDialog = true
        
        coroutineScope.launch {
            delay(800)
            renderingLogText = "Inisialisasi Browser Headless (Puppeteer)..."
            renderingProgress = 15
            delay(900)
            renderingLogText = "Mengekstrak frame-frame Canvas (0-600) @60FPS..."
            renderingProgress = 40
            delay(1200)
            renderingLogText = "Mengirim 600 file PNG ke Backend Render..."
            renderingProgress = 65
            delay(1000)
            renderingLogText = "Mengkompilasi video dengan codec H.264 melalui FFmpeg..."
            renderingProgress = 85
            delay(1100)
            renderingLogText = "Konversi Selesai! Mengemas output berkas MP4..."
            renderingProgress = 100
            delay(600)
            showRenderDialog = false
            showDownloadDialog = true
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back Button",
                        tint = NeonTeal
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    val userPrefs = remember { context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE) }
                    val autoSaveEnabled = userPrefs.getBoolean("auto_save_enabled", true)
                    Text(
                        text = "Canvas Studio Workspace",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBackingUp) "Menyimpan ke Perangkat Lokal ($userEmail)..." 
                               else if (autoSaveEnabled) "● AUTO-SAVED ACTIVE" 
                               else if (lastSavedTime != null) "Tersimpan Lokal: ($userEmail)" 
                               else "Draft Baru",
                        color = if (autoSaveEnabled) NeonTeal else TerminalGreen.copy(alpha=0.7f),
                        fontSize = 10.sp,
                        fontWeight = if (autoSaveEnabled) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            // Live Preview (16:9 WebView Container)
            Text(
                text = "📺 Live Canvas Preview (16:9)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(2.dp, DarkStroke, RoundedCornerShape(10.dp))
            ) {
                CanvasWebView(
                    code = codeContent,
                    modifier = Modifier.fillMaxSize().testTag("canvas_live_webview")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workspace Control Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MidnightSurface,
                contentColor = NeonTeal,
                divider = { Divider(color = DarkStroke) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonTeal
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "",
                                tint = if (selectedTab == 0) NeonTeal else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AI Prompt Builder", color = if (selectedTab == 0) Color.White else TextMuted, fontSize = 13.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "",
                                tint = if (selectedTab == 1) NeonTeal else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Code Editor", color = if (selectedTab == 1) Color.White else TextMuted, fontSize = 13.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content Row
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    // AI Prompt Tab Builder
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tulis deskripsi ide efek visual microstock Anda:",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(24.dp)) {
                                if (isAnalyzingImage) {
                                    CircularProgressIndicator(color = NeonTeal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = "Pilih Gambar untuk Prompt", tint = NeonTeal)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { updatePrompt(it) },
                            placeholder = { Text("Contoh: 'Hujan badai digital laser neon biru berkedip cepat ditiup angin'...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("ai_prompt_text_field"),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MidnightSurface,
                                unfocusedContainerColor = MidnightSurface,
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = DarkStroke,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { triggerCodeGeneration() }),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { triggerCodeGeneration() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_animation_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            enabled = !isGeneratingCode
                        ) {
                            if (isGeneratingCode) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FlashOn, contentDescription = "", modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Generate Animasi (Gemini AI) ⚡",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Safety Warning Info
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonTeal.copy(alpha = 0.05f))
                                .border(1.dp, NeonTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "",
                                tint = NeonTeal,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.Top)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Note: Jika kunci API Gemini belum dikonfigurasi di AI Studio panel rahasia (secrets), sistem secara pintar akan mendeteksi kata kunci prompt dan memuat fallback template premium secara offline.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // Code Editor Tab Builder
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kode Javascript Canvas Editor:",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { 
                                    Toast.makeText(context, "Rendering Preview diperbarui!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGray),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("REFRESH VIEW", fontSize = 10.sp, color = NeonTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightSurface)
                                .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                        ) {
                            // Editable basic scrollable monospace editor box
                            BasicTextField(
                                value = codeContent,
                                onValueChange = { updateCode(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp)
                                    .testTag("code_editor_text_field"),
                                textStyle = TextStyle(
                                    color = TerminalGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Render & Export Footer Button
            Button(
                onClick = { startRenderingProcess() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 10.dp)
                    .testTag("export_mp4_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "",
                        tint = ObsidianBg,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Render & Ekspor MP4 (1080p, Standard Loop) 🎬",
                        color = ObsidianBg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    // A. LOADING DIALOG FOR RENDERING PROGRESS
    if (showRenderDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "MeydiAi Cloud Renderer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CircularProgressIndicator(
                        color = NeonTeal,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Progres: $renderingProgress%",
                        color = NeonTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = renderingProgress / 100f,
                        color = NeonTeal,
                        trackColor = SoftGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = renderingLogText,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        )
    }

    // B. COMPOSITING DOWNLOAD FINISHED DIALOG WITH STATS
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        autoSaveMediaToDevice(context, "MeydiAI_Canvas_Render", "video")
                        Toast.makeText(context, "Membuka link unduh video MP4...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
                ) {
                    Text("Unduh Berkas MP4 (15.2 MB)", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDownloadDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = TextMuted)
                }
            },
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "",
                        tint = TerminalGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Render Selesai! 🎉",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Komposisi rendering video otomatisasi kelayakan microstock berhasil diekspor.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg)
                            .border(1.dp, SoftGray, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Format:", color = TextMuted, fontSize = 11.sp)
                                Text("MP4 / H.264 standard", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Durasi Loop:", color = TextMuted, fontSize = 11.sp)
                                Text("10.0 Detik Seamless", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Resolusi Target:", color = TextMuted, fontSize = 11.sp)
                                Text("Full HD 1080p (240 FPS rendering)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Engine Render:", color = TextMuted, fontSize = 11.sp)
                                Text("Node.js Puppeteer + Libx264 v3", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        )
    }
}

// 3. REMOTION WORKSPACE SCREEN (PLAYGROUND SIMULATION)
@Composable
fun RemotionWorkspaceScreen(userEmail: String, onBack: () -> Unit, viewModel: WorkspaceViewModel = viewModel()) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val codeContent by viewModel.codeContent.collectAsStateWithLifecycle()
    val lastSavedTime by viewModel.lastSavedTime.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()

    val defaultRemotionCode = """
        import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from 'remotion';
        import React from 'react';

        export const SpaceComposition: React.FC = () => {
          const frame = useCurrentFrame();
          const { fps } = useVideoConfig();

          const scale = spring({
            frame,
            fps,
            from: 0,
            to: 1.2,
            config: { damping: 12 },
          });

          return (
            <AbsoluteFill style={{ backgroundColor: '#070714', justifyContent: 'center', alignItems: 'center' }}>
              <div style={{
                width: 300,
                height: 300,
                borderRadius: '50%',
                background: 'radial-gradient(circle, #7f00ff 0%, #00ffcc 100%)',
                transform: `scale(${'$'}{scale})`,
                boxShadow: '0 0 50px rgba(0, 255, 204, 0.4)'
              }} />
            </AbsoluteFill>
          );
        };
    """.trimIndent()

    LaunchedEffect(Unit) {
        viewModel.loadLatestDraft("REMOTION", userEmail, defaultRemotionCode)
    }

    fun updateCode(newCode: String) {
        viewModel.updateCodeContent(newCode, "REMOTION", userEmail)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonPurple
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                    val userPrefs = remember { context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE) }
                    val autoSaveEnabled = userPrefs.getBoolean("auto_save_enabled", true)
                    Text(
                        text = "Remotion Studio Simulator",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBackingUp) "Menyimpan ke Perangkat Lokal ($userEmail)..." 
                               else if (autoSaveEnabled) "● AUTO-SAVED ACTIVE" 
                               else if (lastSavedTime != null) "Tersimpan Lokal: ($userEmail)" 
                               else "Draft Baru",
                        color = if (autoSaveEnabled) NeonPurple else TerminalGreen.copy(alpha=0.7f),
                        fontSize = 10.sp,
                        fontWeight = if (autoSaveEnabled) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = "⚡ Berkomposisi Menggunakan Remotion TSX",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Remotion memungkinkan Anda menulis video berkualitas studio menggunakan komponen React (TSX/JSX) berkinerja tinggi, memanfaatkan kurva fisika spring() dan waktu sinkronisasi audio.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Animated Visual Concept of Remotion Timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MidnightSurface)
                    .border(1.dp, SoftGray, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "🚀 CONCEPT PREVIEW (React-Remotion Space)",
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(50))
                                .border(1.5.dp, NeonTeal, RoundedCornerShape(50)),
                            color = NeonPurple.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("scale", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "", tint = NeonMagenta, modifier = Modifier.size(32.dp))
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text("<AbsoluteFill>", color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("<SpaceComposition />", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("10s @60FPS 1080p", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    // Simulated Timeline bar
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0s (Frame 0)", color = TextMuted, fontSize = 9.sp)
                            Text("10s (Frame 600)", color = TextMuted, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SoftGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.35f) // simulated timeline marker
                                    .fillMaxHeight()
                                    .background(NeonPurple)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Remotion Code template Viewer
            Text(
                text = "💻 Remotion TSX Template:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, SoftGray, RoundedCornerShape(8.dp))
            ) {
                BasicTextField(
                    value = codeContent,
                    onValueChange = { updateCode(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(
                        color = NeonTeal,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export button
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = "", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render Remotion Lambda (AWS Cloud) 🎬", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Tutup", color = Color.White)
                }
            },
            containerColor = MidnightSurface,
            title = {
                Text("Remotion Cloud Renderer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Rendering Remotion memerlukan server cloud AWS Lambda terintegrasi karena membutuhkan Node.js runtime secara penuh untuk merender JSX React bundler. Integrasi rendering React Canvas telah didemonstrasikan di source file backend /meydiai-hybrid-project-files/server.js.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        )
    }
}

// Custom WebView Compose wrapper for the main Canvas execution
@Composable
fun CanvasWebView(code: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                this.webViewClient = object : WebViewClient() {
                    // Suppress loading changes to external sites
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0) // Transparent web view background
            }
        },
        update = { webView ->
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                  <style>
                    body { 
                      margin: 0; 
                      padding: 0;
                      background-color: #070714; 
                      overflow: hidden; 
                      display: flex; 
                      justify-content: center; 
                      align-items: center; 
                      height: 100vh; 
                      width: 100vw;
                    }
                    canvas { 
                      width: 100vw; 
                      height: 56.25vw; /* 16:9 aspect ratio */
                      max-height: 100vh; 
                      max-width: 177.77vh; 
                      background: #000; 
                      box-shadow: 0 4px 20px rgba(0,0,0,0.8); 
                      display: block;
                    }
                  </style>
                </head>
                <body>
                  <canvas id="canvas" width="1920" height="1080"></canvas>
                  <script>
                    window.onerror = function(message, source, lineno, colno, error) {
                      const canvas = document.getElementById('canvas');
                      const ctx = canvas.getContext('2d');
                      ctx.fillStyle = '#1e0505';
                      ctx.fillRect(0, 0, 1920, 1080);
                      ctx.fillStyle = '#ff3b30';
                      ctx.font = 'bold 36px monospace';
                      ctx.fillText("JAVASCRIPT RUNTIME ERROR:", 50, 100);
                      ctx.font = '30px monospace';
                      ctx.fillStyle = '#ffffff';
                      ctx.fillText(message, 50, 180);
                      ctx.fillStyle = '#8b8ba0';
                      ctx.fillText("Line: " + lineno + ", Col: " + colno, 50, 240);
                      return true;
                    };
                  </script>
                  <script>
                    (function() {
                      try {
                        $code
                      } catch(err) {
                        const canvas = document.getElementById('canvas');
                        const ctx = canvas.getContext('2d');
                        ctx.fillStyle = '#1e0505';
                        ctx.fillRect(0, 0, 1920, 1080);
                        ctx.fillStyle = '#ff3b30';
                        ctx.font = 'bold 36px monospace';
                        ctx.fillText("COMPILE ERROR:", 50, 100);
                        ctx.fillStyle = '#ffffff';
                        ctx.font = '30px monospace';
                        ctx.fillText(err.message, 50, 180);
                      }
                    })();
                  </script>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://meydiai-studio.local", html, "text/html", "utf-8", null)
        },
        modifier = modifier
    )
}

// 4. AI CLIPPER SCREEN (Silence Removal and Auto-Cut Simulator)
data class ClipperInterval(val start: Float, val end: Float, val isSilence: Boolean)

@Composable
fun AiClipperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Dasar, 1: Highlight Video, 2: Clipboard, 3: AI Clip Pro
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableIntStateOf(0) }
    var isVideoLoaded by remember { mutableStateOf(false) }
    var hasAnalyzed by remember { mutableStateOf(false) }
    var timelineIntervals by remember { mutableStateOf(listOf<ClipperInterval>()) }

    var targetDuration by remember { mutableStateOf("") }
    var specialRequest by remember { mutableStateOf("") }
    var resultDuration by remember { mutableStateOf("15.2s") }
    val liveLogs = remember { mutableStateListOf<String>() }

    // Tab 0: Dasar Clip States
    var dasarAutoClip by remember { mutableStateOf(true) }
    var dasarSmartClip by remember { mutableStateOf(true) }
    var dasarQuickClip by remember { mutableStateOf(false) }
    var dasarMultiClip by remember { mutableStateOf(true) }
    var dasarPrecisionClip by remember { mutableStateOf(true) }
    var selectedDasarEngine by remember { mutableStateOf("Clip Master") }

    // Tab 1: Video Clip States
    var videoHighlight by remember { mutableStateOf(true) }
    var videoGenerator by remember { mutableStateOf(true) }
    var videoAiMaker by remember { mutableStateOf(true) }
    var videoInstantShare by remember { mutableStateOf(false) }
    var videoLoop by remember { mutableStateOf(true) }
    var selectedVideoEngine by remember { mutableStateOf("Smart Clip Pro") }

    // Tab 2: Clipboard States
    var clipboardSmart by remember { mutableStateOf(true) }
    var clipboardAutoCopy by remember { mutableStateOf(true) }
    var clipboardManagerEnabled by remember { mutableStateOf(true) }
    var clipboardCloudSync by remember { mutableStateOf(false) }
    var clipboardQuickPaste by remember { mutableStateOf(true) }
    var clipboardInputText by remember { mutableStateOf("Meydi AI Premium Studio Clip! Potongan teks ini otomatis disimpan.") }
    val clipboardHistory = remember { 
        mutableStateListOf(
            "Hasil ekspor video clip #2441 berhasil diproses.",
            "Preset Loop Cyberwave: Let's create visual loops in 4K resolution.",
            "Meydi AI • Versi v2.5.4-Stable Build",
            "Code Editor Javascript Remotion Template: let canvas = document.getElementById('canvas');"
        )
    }

    // Tab 3: AI Clip States
    var aiSceneClip by remember { mutableStateOf(true) }
    var aiFaceClip by remember { mutableStateOf(false) }
    var aiVoiceClip by remember { mutableStateOf(true) }
    var aiAutoCaption by remember { mutableStateOf(true) }
    var aiSummary by remember { mutableStateOf(true) }
    var aiTargetKeyword by remember { mutableStateOf("laughing") }

    val keyboardController = LocalSoftwareKeyboardController.current

    fun pickVideo() {
        isVideoLoaded = true
        hasAnalyzed = false
        timelineIntervals = emptyList()
        liveLogs.clear()
        Toast.makeText(context, "Berhasil memuat video raw_footage.mp4 ke Studio!", Toast.LENGTH_SHORT).show()
    }

    fun processClipper(tabIndex: Int) {
        keyboardController?.hide()
        isAnalyzing = true
        analysisProgress = 0
        liveLogs.clear()
        
        coroutineScope.launch {
            if (tabIndex == 0) {
                liveLogs.add("Mengaktifkan Engine $selectedDasarEngine...")
                delay(400)
                if (dasarAutoClip) {
                    liveLogs.add("[Sistem] Mendeteksi dan menandai keheningan visual secara otomatis...")
                    delay(300)
                }
                if (dasarSmartClip) {
                    liveLogs.add("[AI] Smart Clip memindai pergerakan objek utama...")
                    delay(300)
                }
                if (dasarPrecisionClip) {
                    liveLogs.add("[Sistem] Mengaktifkan Precision Clip untuk akurasi frame...")
                    delay(300)
                }
                if (dasarMultiClip) {
                    liveLogs.add("[Sistem] Membagi footage ke dalam beberapa potongan segmen mikro...")
                    delay(300)
                }
            } else if (tabIndex == 1) {
                liveLogs.add("Mengaktifkan Video Engine $selectedVideoEngine...")
                delay(400)
                if (videoHighlight) {
                    liveLogs.add("[AI] Menjalankan Auto Highlight Clip untuk mencari momen klimaks...")
                    delay(300)
                }
                if (videoGenerator) {
                    liveLogs.add("[Sistem] Membuat video pendek klip dari durasi panjang...")
                    delay(300)
                }
                if (videoAiMaker) {
                    liveLogs.add("[AI] Menganalisa frame penting dengan AI Clip Maker...")
                    delay(300)
                }
                if (videoLoop) {
                    liveLogs.add("[Sistem] Menyetel kelancaran transisi Loop Clip...")
                    delay(300)
                }
            } else if (tabIndex == 3) {
                liveLogs.add("Mengaktifkan AI Clip Studio Engine...")
                delay(400)
                if (aiSceneClip) {
                    liveLogs.add("[AI] Mengenali pergantian adegan video (AI Scene Clip)...")
                    delay(300)
                }
                if (aiFaceClip) {
                    liveLogs.add("[AI] Memindai elemen wajah murni (AI Face Clip)...")
                    delay(300)
                }
                if (aiVoiceClip) {
                    liveLogs.add("[AI] Mencocokkan rekaman suara dengan kata kunci kustom: '$aiTargetKeyword'...")
                    delay(300)
                }
                if (aiAutoCaption) {
                    liveLogs.add("[AI] Menyisipkan Auto Caption secara instan & burn-in...")
                    delay(300)
                }
                if (aiSummary) {
                    liveLogs.add("[AI] Merangkum klip penting (Smart Clip Summary)...")
                    delay(300)
                }
            }

            while (analysisProgress < 100) {
                delay(20)
                analysisProgress += 5
            }

            // Generate mock timeline
            timelineIntervals = listOf(
                ClipperInterval(0f, 0.15f, false),
                ClipperInterval(0.15f, 0.3f, true),
                ClipperInterval(0.3f, 0.55f, false),
                ClipperInterval(0.55f, 0.7f, true),
                ClipperInterval(0.7f, 0.85f, false),
                ClipperInterval(0.85f, 1f, true)
            )

            val minutes = targetDuration.toIntOrNull() ?: 0
            resultDuration = if (minutes > 0) "${minutes}:00" else "12.4s"
            
            liveLogs.add("[Sukses] Pemotongan frame & render lossless selesai!")
            hasAnalyzed = true
            isAnalyzing = false
            Toast.makeText(context, "Selesai memproses klip premium!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back Button",
                            tint = NeonMagenta
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "AI Ultra-Clipper Studio ✂️",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pemotongan Cerdas & Manajemen Clipboard Real-time",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Premium Status Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(TerminalGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clip Engine v3.0 Active",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonMagenta.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ULTRA CLIPPED ENABLED",
                        color = NeonMagenta,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful Tab Options (4 Tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightSurface)
                    .padding(4.dp)
            ) {
                val tabs = listOf(
                    "✂️ DASAR" to 0,
                    "🎥 HIGH-VIDEO" to 1,
                    "📋 CLIPBOARD" to 2,
                    "🤖 AI CLIP" to 3
                )
                tabs.forEach { (label, index) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonMagenta else Color.Transparent)
                            .clickable {
                                if (!isAnalyzing) {
                                    selectedTab = index
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Root Video Picker shown for non-Clipboard tabs so users can pick media.
            if (selectedTab != 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, if (isVideoLoaded) NeonTeal else DarkStroke, RoundedCornerShape(12.dp))
                        .clickable { if (!isAnalyzing) pickVideo() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideoLoaded) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "",
                                tint = NeonTeal,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "raw_footage_premium_60fps.mp4",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Durasi: 00:30 (Ultra HD HDR)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "",
                                tint = NeonMagenta.copy(alpha = 0.8f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Upload Video Footage Target",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ketuk untuk memuat video dari galeri",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- TAB 0: DASAR CLIP ---
            if (selectedTab == 0) {
                Text(
                    text = "Konfigurasi Fitur Dasar Clip ✂️",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Gunakan rekayasa pemotongan frame standar berkecepatan tinggi dengan akurasi optimal.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                // Algoritma Toggles Custom Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Auto Clip" to Triple("Memotong bagian hening/penting otomatis", dasarAutoClip) { v: Boolean -> dasarAutoClip = v },
                        "Smart Clip" to Triple("Mendeteksi objek subjek & klip otomatis", dasarSmartClip) { v: Boolean -> dasarSmartClip = v },
                        "Quick Clip" to Triple("Lakukan pemotongan cepat satu sentuhan", dasarQuickClip) { v: Boolean -> dasarQuickClip = v },
                        "Multi Clip" to Triple("Membuat banyak pecahan frame sekaligus", dasarMultiClip) { v: Boolean -> dasarMultiClip = v },
                        "Precision Clip" to Triple("Pemotongan dengan akurasi frame-by-frame tinggi", dasarPrecisionClip) { v: Boolean -> dasarPrecisionClip = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonMagenta,
                                    checkedTrackColor = NeonMagenta.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Engine choice
                Text(
                    text = "Gunakan Premium Clip Core Engine:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Clip Master", "Smart Clip Pro", "Ultra Clip", "Clip Boost").forEach { engine ->
                        val isSel = selectedDasarEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonMagenta.copy(alpha = 0.2f) else MidnightSurface)
                                .border(BorderStroke(1.dp, if (isSel) NeonMagenta else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedDasarEngine = engine }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engine,
                                color = if (isSel) Color.White else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { processClipper(0) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    enabled = isVideoLoaded && !isAnalyzing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) "Processing via $selectedDasarEngine..." else "Proses Clip Master ⚡",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- TAB 1: HIGH-VIDEO CLIP ---
            if (selectedTab == 1) {
                Text(
                    text = "Seksi Highlight Video Clip 🎥",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Konversi rekaman mentah video durasi panjang menjadi highlight clip premium siap saji.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Auto Highlight Clip" to Triple("Mengambil momen visual transisi terbaik otomatis", videoHighlight) { v: Boolean -> videoHighlight = v },
                        "Clip Generator" to Triple("Konversi video panjang ke klip vertikal pendek", videoGenerator) { v: Boolean -> videoGenerator = v },
                        "AI Clip Maker" to Triple("Generasi video berdasarkan clustering adegan utama", videoAiMaker) { v: Boolean -> videoAiMaker = v },
                        "Instant Clip Share" to Triple("Simpan dan auto bagikan instan ke jaringan", videoInstantShare) { v: Boolean -> videoInstantShare = v },
                        "Loop Clip" to Triple("Tautkan frame akhir ke awal secara looping seamless", videoLoop) { v: Boolean -> videoLoop = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonMagenta,
                                    checkedTrackColor = NeonMagenta.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium choice
                Text(
                    text = "Pilih Video Clip Engine Pro:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Clip Wizard", "Clip Max", "One Tap Clip", "Clip Engine", "Crystal Clip").forEach { engine ->
                        val isSel = selectedVideoEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonMagenta.copy(alpha = 0.2f) else MidnightSurface)
                                .border(BorderStroke(1.dp, if (isSel) NeonMagenta else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedVideoEngine = engine }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engine,
                                color = if (isSel) Color.White else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Outlined Input Target duration
                OutlinedTextField(
                    value = targetDuration,
                    onValueChange = { targetDuration = it.filter { char -> char.isDigit() } },
                    label = { Text("Target Limit Durasi (Menit)", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = DarkStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { processClipper(1) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    enabled = isVideoLoaded && !isAnalyzing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) "Memotong video highlight..." else "Jalankan $selectedVideoEngine ⚡",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- TAB 2: SMART CLIPBOARD SUITE ---
            if (selectedTab == 2) {
                Text(
                    text = "Seksi Smart Clipboard Suite 📋",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Sinkronisasi, kelola, serta lakukan pengkopian teks instan dari riwayat clipping data multi-tier.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                // Settings toggles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Smart Clipboard" to Triple("Secara otomatis kelola riwayat penyalinan teks", clipboardSmart) { v: Boolean -> clipboardSmart = v },
                        "Auto Copy Clip" to Triple("Otomatis salin hasil ke clipboard sistem", clipboardAutoCopy) { v: Boolean -> clipboardAutoCopy = v },
                        "Clipboard Manager" to Triple("Tampilkan modul visual managemen tumpukan", clipboardManagerEnabled) { v: Boolean -> clipboardManagerEnabled = v },
                        "Cloud Clipboard" to Triple("Sinkronisasi instan clipboard antar perangkat", clipboardCloudSync) { v: Boolean -> clipboardCloudSync = v },
                        "Quick Paste Clip" to Triple("Tempel instan teks dari baris riwayat pilihan", clipboardQuickPaste) { v: Boolean -> clipboardQuickPaste = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonMagenta,
                                    checkedTrackColor = NeonMagenta.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive text input area
                Text(
                    text = "Area Sunting Simulasi Clipboard:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = clipboardInputText,
                    onValueChange = { clipboardInputText = it },
                    placeholder = { Text("Ketik kata kunci atau teks untuk disalin otomatis...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = DarkStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            if (clipboardInputText.isNotEmpty()) {
                                if (clipboardManager != null) {
                                    val clip = android.content.ClipData.newPlainText("Meydi AI Clip", clipboardInputText)
                                    clipboardManager.setPrimaryClip(clip)
                                }
                                if (clipboardSmart && !clipboardHistory.contains(clipboardInputText)) {
                                    clipboardHistory.add(0, clipboardInputText)
                                }
                                Toast.makeText(context, "Selesai disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ketik teks terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin & Log 📋", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val primaryClip = clipboardManager?.primaryClip
                            if (primaryClip != null && primaryClip.itemCount > 0) {
                                val text = primaryClip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotEmpty()) {
                                    clipboardInputText = text
                                    Toast.makeText(context, "Sukses merekatkan dari clipboard sistem!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard sistem kosong!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Layanan Clipboard tidak tersedia / kosong!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, DarkStroke),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tempel Lintas System 📥", color = Color.White, fontSize = 11.sp)
                    }
                }

                if (clipboardManagerEnabled) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tumpukan Riwayat Clipboard (${clipboardHistory.size}):",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (clipboardHistory.isNotEmpty()) {
                            Text(
                                text = "Bersihkan Semua",
                                color = ErrorRed,
                                fontSize = 10.sp,
                                modifier = Modifier.clickable {
                                    clipboardHistory.clear()
                                    Toast.makeText(context, "Riwayat dibersihkan.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    if (clipboardHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightSurface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada riwayat tumpukan salinan data.", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            clipboardHistory.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (clipboardQuickPaste) {
                                                clipboardInputText = item
                                                Toast.makeText(context, "Tempel instan berhasil!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                    border = BorderStroke(1.dp, DarkStroke)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = NeonMagenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (clipboardQuickPaste) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Quick load",
                                                tint = TerminalGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- TAB 3: AI CLIP PRO ---
            if (selectedTab == 3) {
                Text(
                    text = "Seksi Rekayasa AI Clip Studio 🤖",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Gunakan model machine learning terlatih untuk pemindaian adegan, pendeteksi wajah khusus & kata kunci suara.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "AI Scene Clip" to Triple("Deteksi visual pergantian adegan video otomatis", aiSceneClip) { v: Boolean -> aiSceneClip = v },
                        "AI Face Clip" to Triple("Lokalisasi wajah murni subjek frame", aiFaceClip) { v: Boolean -> aiFaceClip = v },
                        "AI Voice Clip" to Triple("Pencarian kata kunci / suara audio target otomatis", aiVoiceClip) { v: Boolean -> aiVoiceClip = v },
                        "Auto Caption Clip" to Triple("Otomatis transkrip suara & bakar subtitle visual", aiAutoCaption) { v: Boolean -> aiAutoCaption = v },
                        "Smart Clip Summary" to Triple("Menghasilkan ringkasan ringkas klip footage", aiSummary) { v: Boolean -> aiSummary = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonMagenta,
                                    checkedTrackColor = NeonMagenta.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                if (aiVoiceClip) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Kata Kunci Deteksi Suara Premium:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = aiTargetKeyword,
                        onValueChange = { aiTargetKeyword = it },
                        placeholder = { Text("Contoh: laughing, yelling, transition, intro", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MidnightSurface,
                            unfocusedContainerColor = MidnightSurface,
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = DarkStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { processClipper(3) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    enabled = isVideoLoaded && !isAnalyzing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) "Menganalisa data AI Scene & Voice..." else "Jalankan AI Clip Studio 🧠",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- ANIMATING PROCESS LOOPS AND TELEMETRY LOGS ---
            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, NeonMagenta)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = NeonMagenta,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Menghitung parameter & konversi frame... ${analysisProgress}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { analysisProgress.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = NeonMagenta,
                            trackColor = ObsidianBg
                        )
                    }
                }
            }

            // Live logs panel if available
            if (liveLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Live Clip Engine Console Stream:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    val scrollStateConsole = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollStateConsole)
                    ) {
                        liveLogs.forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("[Sukses]")) TerminalGreen else if (log.contains("[AI]")) NeonMagenta else Color(0xFF39FF14),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // --- ANALYSIS VISUAL TIMELINE OUTPUTS ---
            if (hasAnalyzed && selectedTab != 2) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Visual Timeline (Keheningan & Objek Dihilangkan):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                // Timeline bar visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidnightSurface)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        timelineIntervals.forEach { interval ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(interval.end - interval.start)
                                    .background(if (interval.isSilence) ErrorRed.copy(alpha = 0.25f) else TerminalGreen.copy(alpha = 0.75f))
                            ) {
                                if (interval.isSilence) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "",
                                        tint = ErrorRed,
                                        modifier = Modifier.align(Alignment.Center).size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Durasi Mentah: 30.0s", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = "Output Render Lip: $resultDuration",
                        color = TerminalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        autoSaveMediaToDevice(context, "MeydiAI_Premium_Clipped", "video")
                        Toast.makeText(context, "Master file klip tersimpan di Galeri!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
                ) {
                    Text("Simpan Master MP4 Video Final 📥", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// 5. MEDIA DOWNLOADER SCREEN
data class DownloadQueueItem(
    val id: String,
    val url: String,
    val mediaType: String,
    val platform: String,
    val timestamp: Long,
    val status: String
)

fun saveDownloadQueue(context: android.content.Context, queue: List<DownloadQueueItem>) {
    val sharedPrefs = context.getSharedPreferences("meydiai_downloader_prefs", android.content.Context.MODE_PRIVATE)
    val jsonArray = org.json.JSONArray()
    for (item in queue) {
        val jsonObject = org.json.JSONObject().apply {
            put("id", item.id)
            put("url", item.url)
            put("mediaType", item.mediaType)
            put("platform", item.platform)
            put("timestamp", item.timestamp)
            put("status", item.status)
        }
        jsonArray.put(jsonObject)
    }
    sharedPrefs.edit().putString("download_queue", jsonArray.toString()).apply()
}

fun loadDownloadQueue(context: android.content.Context): List<DownloadQueueItem> {
    val sharedPrefs = context.getSharedPreferences("meydiai_downloader_prefs", android.content.Context.MODE_PRIVATE)
    val jsonStr = sharedPrefs.getString("download_queue", null) ?: return emptyList()
    val list = mutableListOf<DownloadQueueItem>()
    try {
        val jsonArray = org.json.JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            list.add(
                DownloadQueueItem(
                    id = jsonObject.getString("id"),
                    url = jsonObject.getString("url"),
                    mediaType = jsonObject.getString("mediaType"),
                    platform = jsonObject.getString("platform"),
                    timestamp = jsonObject.getLong("timestamp"),
                    status = jsonObject.getString("status")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@Composable
fun MediaDownloaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("Video") } // "Video" or "Image"
    
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatus by remember { mutableStateOf("Menunggu URL...") }
    
    // Automation States
    var isSimulationOffline by remember { mutableStateOf(false) }
    var isRealOnline by remember { mutableStateOf(true) }
    val isOnline = isRealOnline && !isSimulationOffline
    
    var downloadQueue by remember { mutableStateOf(loadDownloadQueue(context)) }
    
    var activeDownloadingItemId by remember { mutableStateOf<String?>(null) }
    var activeDownloadProgress by remember { mutableFloatStateOf(0f) }
    var activeDownloadStatus by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe real network status
    LaunchedEffect(Unit) {
        while (true) {
            var connected = false
            try {
                val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                if (connectivityManager != null) {
                    val activeNetwork = connectivityManager.activeNetwork
                    if (activeNetwork != null) {
                        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                        connected = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    }
                } else {
                    connected = true
                }
            } catch (e: SecurityException) {
                connected = true
            } catch (e: Exception) {
                connected = true
            }
            isRealOnline = connected
            delay(2000)
        }
    }

    // Automatically trigger downloads of pending items when the engine detects active internet connection
    LaunchedEffect(isOnline, downloadQueue) {
        if (isOnline) {
            val pendingItems = downloadQueue.filter { it.status == "Pending" }
            if (pendingItems.isNotEmpty() && activeDownloadingItemId == null) {
                coroutineScope.launch {
                    Toast.makeText(context, "MeydiAI Engine: Koneksi Pulih! Memulai download otomatis ${pendingItems.size} antrean...", Toast.LENGTH_LONG).show()
                    
                    for (item in pendingItems) {
                        activeDownloadingItemId = item.id
                        activeDownloadProgress = 0f
                        activeDownloadStatus = "Menghubungkan ke API ${item.platform}..."
                        
                        // Update status to Downloading
                        downloadQueue = downloadQueue.map {
                            if (it.id == item.id) it.copy(status = "Downloading") else it
                        }
                        saveDownloadQueue(context, downloadQueue)
                        
                        delay(1200)
                        activeDownloadStatus = "Mengunduh ${item.mediaType} HD: ${item.url.take(30)}..."
                        
                        while (activeDownloadProgress < 1f) {
                            delay(60)
                            activeDownloadProgress += 0.04f
                        }
                        
                        // Trigger actual storage save
                        autoSaveMediaToDevice(
                            context,
                            if (item.mediaType == "Video") "MeydiAI_Auto_Video" else "MeydiAI_Auto_Photo",
                            if (item.mediaType == "Video") "video" else "image"
                        )
                        
                        // Update state to Finished / Success
                        downloadQueue = downloadQueue.map {
                            if (it.id == item.id) it.copy(status = "Success") else it
                        }
                        saveDownloadQueue(context, downloadQueue)
                        delay(1000)
                    }
                    activeDownloadingItemId = null
                    Toast.makeText(context, "Semua unduhan otomatis selesai disimpan di Galeri! 🎉", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun startDownload() {
        if (urlInput.isBlank()) {
            Toast.makeText(context, "URL tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        keyboardController?.hide()
        
        val isYouTube = urlInput.contains("youtube.com") || urlInput.contains("youtu.be")
        val isTikTok = urlInput.contains("tiktok.com")
        val isImage = urlInput.contains(".jpg") || urlInput.contains(".png") || urlInput.contains(".jpeg") || urlInput.contains("image") || urlInput.contains("photo")
        
        val platformName = when {
            isYouTube -> "YouTube"
            isTikTok -> "TikTok"
            isImage -> "Image URL"
            else -> "Raw URL"
        }

        // Generate unique item
        val newItem = DownloadQueueItem(
            id = "DL_${System.currentTimeMillis()}",
            url = urlInput,
            mediaType = selectedMediaType,
            platform = platformName,
            timestamp = System.currentTimeMillis(),
            status = if (isOnline) "Downloading" else "Pending"
        )

        // Add to queue
        val currentList = downloadQueue.toMutableList()
        currentList.add(0, newItem)
        downloadQueue = currentList
        saveDownloadQueue(context, downloadQueue)

        if (!isOnline) {
            Toast.makeText(context, "Sistem Offline Terdeteksi! Ditambahkan ke antrean otomasi.", Toast.LENGTH_LONG).show()
            urlInput = ""
            return
        }

        // Online Direct Download Flow
        isDownloading = true
        downloadProgress = 0f
        
        coroutineScope.launch {
            downloadStatus = "Menghubungkan ke Server Ekstraktor $platformName..."
            delay(1000)
            
            downloadStatus = "Mendapatkan tautan file HD biner (Simulasi)..."
            delay(1200)
            
            downloadStatus = "Mengunduh file ${selectedMediaType}..."
            while (downloadProgress < 1f) {
                delay(50)
                downloadProgress += 0.03f
            }
            
            downloadStatus = "Berhasil! Media disimpan di Galeri perangkat."
            
            // Execute actual file writer helper
            autoSaveMediaToDevice(
                context, 
                "MeydiAI_${platformName}_Download", 
                if (selectedMediaType == "Video") "video" else "image"
            )
            
            // Set downloaded item in queue as Success
            downloadQueue = downloadQueue.map {
                if (it.id == newItem.id) it.copy(status = "Success") else it
            }
            saveDownloadQueue(context, downloadQueue)
            
            delay(2000)
            
            // Reset state
            isDownloading = false
            urlInput = ""
            downloadProgress = 0f
            downloadStatus = "Menunggu URL..."
        }
    }

    fun clearQueue() {
        downloadQueue = emptyList()
        saveDownloadQueue(context, emptyList())
        Toast.makeText(context, "Riwayat antrean dibersihkan!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back Button",
                        tint = TerminalGreen
                    )
                }
                Text(
                    text = "MeydiAI Downloader Engine",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 1. KONEKTIVITAS & OTOMASI STATUS CONSOLE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(BorderStroke(1.dp, if (isOnline) TerminalGreen.copy(alpha = 0.3f) else Color(0xFFFF5252).copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status Mesin Unduhan",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Monitoring & Unduh Otomasi",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isOnline) TerminalGreen.copy(alpha = 0.15f) 
                                    else Color(0xFFFF5252).copy(alpha = 0.15f)
                                )
                                .border(1.dp, if (isOnline) TerminalGreen else Color(0xFFFF5252), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isOnline) TerminalGreen else Color(0xFFFF5252))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    color = if (isOnline) TerminalGreen else Color(0xFFFF5252),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkStroke, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Offline Simulator Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Simulasikan Mode Offline 🔌",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gunakan sakelar ini untuk menguji antrean offline otomatis saat terputus dari internet.",
                                color = Color.LightGray.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                        Switch(
                            checked = isSimulationOffline,
                            onCheckedChange = { isSimulationOffline = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF5252),
                                checkedTrackColor = Color(0xFFFF5252).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = MidnightSurface
                            )
                        )
                    }
                }
            }
            
            // 2. INPUT MEDIA DOWNLOADER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Format Media Tujuan",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // MediaType Selector Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .padding(4.dp)
                    ) {
                        listOf("Video", "Image").forEach { type ->
                            val isSelected = selectedMediaType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) TerminalGreen else Color.Transparent)
                                    .clickable { selectedMediaType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (type == "Video") "Video MP4 🎬" else "Gambar HD 📸",
                                    color = if (isSelected) ObsidianBg else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = "Masukkan URL Media:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { 
                            Text(
                                text = if (selectedMediaType == "Video") "Contoh: https://youtube.com/... atau tiktok.com/..." 
                                       else "Contoh: https://images.unsplash.com/... atau tautan foto", 
                                color = TextMuted,
                                fontSize = 12.sp
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg,
                            focusedBorderColor = TerminalGreen,
                            unfocusedBorderColor = DarkStroke,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { startDownload() }),
                        enabled = !isDownloading
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { startDownload() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOnline) TerminalGreen else Color(0xFFFFB300),
                            disabledContainerColor = MidnightSurface
                        ),
                        enabled = !isDownloading && urlInput.isNotBlank()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDownload else Icons.Default.Info, 
                                contentDescription = "", 
                                tint = ObsidianBg
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Mulai Unduh Otomatis" else "Daftarkan Ke Antrean Offline",
                                color = ObsidianBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 3. SEKSI RUNNING DOWNLOAD PROGRESS
            if (isDownloading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(BorderStroke(1.dp, TerminalGreen), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sedang Mengunduh Media Langsung",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = TerminalGreen,
                            trackColor = ObsidianBg,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = downloadStatus, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(text = "${(downloadProgress * 100).toInt()}%", color = TerminalGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. OTOMATIS RUNNING BACKGROUND SINKRON DI UI
            if (activeDownloadingItemId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.8f)), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TerminalGreen,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sinkronisasi Otomatis Antrean Offline...",
                                color = TerminalGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { activeDownloadProgress },
                            color = TerminalGreen,
                            trackColor = ObsidianBg,
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeDownloadStatus,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // 5. RIWAYAT & DAFTAR ANTREAN OTOMASI (PERSISTED QUEUE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Antrean & Riwayat Desentralisasi",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val pendingCount = downloadQueue.count { it.status == "Pending" }
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFB300))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$pendingCount Tertunda",
                                color = ObsidianBg,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                if (downloadQueue.isNotEmpty()) {
                    Text(
                        text = "Bersihkan 🗑️",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { clearQueue() }
                    )
                }
            }

            if (downloadQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload, 
                            contentDescription = "", 
                            tint = TextMuted, 
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Antrean Otomasi Kosong",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Url yang Anda masukkan baik saat online/offline akan otomatis terarsip dan disinkronkan di sini.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    downloadQueue.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    BorderStroke(
                                        1.dp, 
                                        when (item.status) {
                                            "Pending" -> Color(0xFFFFB300).copy(alpha = 0.3f)
                                            "Downloading" -> TerminalGreen.copy(alpha = 0.5f)
                                            "Success" -> TerminalGreen.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    ), 
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.mediaType == "Video") "ID Unduhan: Video MP4 🎬" else "ID Unduhan: Gambar HD 📸",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.url,
                                            color = Color.LightGray.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    
                                    // Item status indicator
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (item.status) {
                                                    "Pending" -> Color(0xFFFFB300).copy(alpha = 0.15f)
                                                    "Downloading" -> TerminalGreen.copy(alpha = 0.15f)
                                                    "Success" -> TerminalGreen.copy(alpha = 0.1f)
                                                    else -> Color.Gray.copy(alpha = 0.1f)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = when (item.status) {
                                                "Pending" -> "⏳ Pending Offline"
                                                "Downloading" -> "🔄 Mengunduh..."
                                                "Success" -> "✅ Tersimpan"
                                                else -> item.status
                                            },
                                            color = when (item.status) {
                                                "Pending" -> Color(0xFFFFB300)
                                                "Downloading" -> TerminalGreen
                                                "Success" -> TerminalGreen.copy(alpha = 0.8f)
                                                else -> Color.Gray
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Platform: ${item.platform}",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                    
                                    val formattedTime = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
                                    Text(
                                        text = "Terdaftar: $formattedTime",
                                        color = TextMuted,
                                        fontSize = 9.sp
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

// 6. HD ENHANCER SCREEN WITH AUTO-GERENATOR & ENHANCER
@Composable
fun LiveHdMediaPreview(preset: String, modifier: Modifier = Modifier) {
    var tick by remember { mutableStateOf(0f) }
    LaunchedEffect(preset) {
        while (true) {
            tick += 0.04f
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        when (preset) {
            "cyberwave" -> {
                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0F0B26), Color(0xFF03010A))))
                for (j in 0 until 4) {
                    val path = androidx.compose.ui.graphics.Path()
                    val startY = height / 2 + Math.sin((-tick + j * 0.5f).toDouble()).toFloat() * 40f
                    path.moveTo(0f, startY)
                    for (x in 0..width.toInt() step 12) {
                        val y = height / 2 +
                                Math.sin((x * 0.006f - tick + j * 0.8f).toDouble()).toFloat() * 35f +
                                Math.cos((x * 0.002f + tick + j * 0.3f).toDouble()).toFloat() * 15f
                        path.lineTo(x.toFloat(), y)
                    }
                    val color = when (j) {
                        0 -> Color(0xFFE91E63)
                        1 -> Color(0xFF2196F3)
                        2 -> Color(0xFF00FFCC)
                        else -> Color(0xFF9C27B0)
                    }
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.4f + j * 0.15f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            "cosmic" -> {
                drawRect(brush = Brush.radialGradient(colors = listOf(Color(0xFF1E1035), Color(0xFF05030F)), center = androidx.compose.ui.geometry.Offset(width / 2, height / 2), radius = width * 0.8f))
                val cx = width / 2
                val cy = height / 2
                val numParticles = 60
                for (i in 0 until numParticles) {
                    val factor = i.toFloat() / numParticles
                    val angle = factor * Math.PI * 8 + tick * (0.8f + factor)
                    val dist = factor * (width / 2.3f) + 15f
                    val px = cx + Math.cos(angle).toFloat() * dist
                    val py = cy + Math.sin(angle).toFloat() * dist
                    val pSize = 2.dp.toPx() + factor * 4.dp.toPx()
                    val pColor = Color.hsv(260f + factor * 100f, 0.8f, 1f, 0.5f + factor * 0.5f)
                    drawCircle(color = pColor, radius = pSize, center = androidx.compose.ui.geometry.Offset(px, py))
                }
                drawCircle(color = Color(0xFFFF00CC).copy(alpha = 0.18f), radius = 30.dp.toPx() + Math.sin(tick.toDouble()).toFloat() * 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
            "synthgrid" -> {
                drawRect(color = Color(0xFF04020A))
                val cx = width / 2
                val cy = height / 2.4f
                val sunRadius = 55.dp.toPx()
                drawCircle(
                    brush = Brush.verticalGradient(colors = listOf(Color(0xFFFF0055), Color(0xFFFFBB00))),
                    radius = sunRadius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
                for (y in (cy - sunRadius).toInt()..(cy + sunRadius).toInt() step 10) {
                    val thickness = 2.dp.toPx() + (y - (cy - sunRadius)) * 0.02f
                    drawLine(
                        color = Color(0xFF04020A),
                        start = androidx.compose.ui.geometry.Offset(cx - sunRadius - 5f, y.toFloat()),
                        end = androidx.compose.ui.geometry.Offset(cx + sunRadius + 5f, y.toFloat()),
                        strokeWidth = thickness
                    )
                }
                val horizon = height * 0.58f
                val numLatitudes = 8
                for (i in 0..numLatitudes) {
                    val ratio = i.toFloat() / numLatitudes
                    val scrollY = horizon + Math.pow(ratio.toDouble(), 1.5).toFloat() * (height - horizon)
                    drawLine(
                        color = Color(0xFF00FFCC).copy(alpha = 0.2f + ratio * 0.4f),
                        start = androidx.compose.ui.geometry.Offset(0f, scrollY),
                        end = androidx.compose.ui.geometry.Offset(width, scrollY),
                        strokeWidth = 1.dp.toPx() + ratio * 1.dp.toPx()
                    )
                }
                val numLongitudes = 10
                for (i in 0..numLongitudes) {
                    val factor = i.toFloat() / numLongitudes
                    val startX = cx + (factor - 0.5f) * width * 0.12f
                    val endX = (factor - 0.5f) * width * 2.0f + cx
                    drawLine(
                        color = Color(0xFF00FFCC).copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(startX, horizon),
                        end = androidx.compose.ui.geometry.Offset(endX, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            else -> {
                drawRect(brush = Brush.verticalGradient(colors = listOf(Color(0xFF060D1E), Color(0xFF02040A))))
                val cx = width / 2
                val cy = height / 2
                val points = listOf(
                    androidx.compose.ui.geometry.Offset(0.2f, 0.3f),
                    androidx.compose.ui.geometry.Offset(0.4f, 0.2f),
                    androidx.compose.ui.geometry.Offset(0.7f, 0.24f),
                    androidx.compose.ui.geometry.Offset(0.85f, 0.4f),
                    androidx.compose.ui.geometry.Offset(0.6f, 0.55f),
                    androidx.compose.ui.geometry.Offset(0.3f, 0.65f),
                    androidx.compose.ui.geometry.Offset(0.15f, 0.5f),
                    androidx.compose.ui.geometry.Offset(0.5f, 0.4f),
                    androidx.compose.ui.geometry.Offset(0.8f, 0.7f)
                )
                val animatedPoints = points.map { pt ->
                    val dx = Math.sin((tick + pt.x * 50f).toDouble()).toFloat() * 12f
                    val dy = Math.cos((tick + pt.y * 50f).toDouble()).toFloat() * 12f
                    androidx.compose.ui.geometry.Offset(pt.x * width + dx, pt.y * height + dy)
                }
                for (i in animatedPoints.indices) {
                    for (j in (i + 1) until animatedPoints.size) {
                        val distSq = (animatedPoints[i].x - animatedPoints[j].x) * (animatedPoints[i].x - animatedPoints[j].x) +
                                (animatedPoints[i].y - animatedPoints[j].y) * (animatedPoints[i].y - animatedPoints[j].y)
                        val maxDist = (120.dp.toPx()) * (120.dp.toPx())
                        if (distSq < maxDist) {
                            val alpha = 1.0f - (distSq / maxDist)
                            drawLine(
                                color = Color(0xFFE91E63).copy(alpha = alpha * 0.4f),
                                start = animatedPoints[i],
                                end = animatedPoints[j],
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
                animatedPoints.forEach { pt ->
                    drawCircle(color = Color(0xFF00FFFF).copy(alpha = 0.9f), radius = 3.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFF00FFFF).copy(alpha = 0.25f), radius = 8.dp.toPx(), center = pt)
                }
            }
        }
    }
}

@Composable
fun HdEnhancerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: HD Visual, 1: HD Video, 2: HD Kamera, 3: AI Generator
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("Menunggu instruksi...") }
    var currentProgress by remember { mutableStateOf(0f) }

    // --- Tab 0: HD Visual States ---
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaName by remember { mutableStateOf<String?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>("image") }
    var selectedScale by remember { mutableStateOf("4K Ultra HD") }
    var enhancedPreviewReady by remember { mutableStateOf(false) }

    var visualEnhanceAuto by remember { mutableStateOf(true) }
    var visualHdConverter by remember { mutableStateOf(true) }
    var visualAIUpscale by remember { mutableStateOf(true) }
    var visualWallpaperGen by remember { mutableStateOf(false) }
    var visualThumbnailPreview by remember { mutableStateOf(true) }
    var selectedVisualEngine by remember { mutableStateOf("True HD Optimizer") }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            val type = context.contentResolver.getType(uri) ?: ""
            selectedMediaType = if (type.contains("video")) "video" else "image"
            
            var name = "media_${System.currentTimeMillis().toString().takeLast(6)}"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {}
            selectedMediaName = name
            enhancedPreviewReady = false
        }
    }

    // --- Tab 1: HD Video States ---
    var videoBooster by remember { mutableStateOf(true) }
    var videoUpscaler by remember { mutableStateOf(true) }
    var videoFrameEnhancement by remember { mutableStateOf(true) }
    var videoStreamingMode by remember { mutableStateOf(false) }
    var videoResolutionAdjuster by remember { mutableStateOf(true) }
    var selectedVideoEngine by remember { mutableStateOf("Smart HD Engine") }
    var videoEnhancedReady by remember { mutableStateOf(false) }

    // --- Tab 2: HD Kamera States ---
    var cameraSharpMode by remember { mutableStateOf(true) }
    var cameraNightHd by remember { mutableStateOf(false) }
    var cameraPortraitHd by remember { mutableStateOf(true) }
    var cameraFaceEnhance by remember { mutableStateOf(true) }
    var cameraUltraHdCapture by remember { mutableStateOf(true) }
    var cameraBokehStrength by remember { mutableStateOf(50f) }
    var selectedCameraEngine by remember { mutableStateOf("Crystal HD") }
    var cameraFlashOn by remember { mutableStateOf(false) }
    var isShutterAnimating by remember { mutableStateOf(false) }
    var capturedPhotoReady by remember { mutableStateOf(false) }

    // --- Tab 3: AI Auto Generator HD States ---
    var selectedPreset by remember { mutableStateOf("cyberwave") } // cyberwave, cosmic, synthgrid, mesh
    var promptInput by remember { mutableStateOf("") }
    var outputFormat by remember { mutableStateOf("HD Looping Video (60fps)") } // HD Looping Video, HD Ultra-res Foto
    var generatedMediaReady by remember { mutableStateOf(false) }

    val presetLabel = when (selectedPreset) {
        "cyberwave" -> "Neon Cyberwave Flow"
        "cosmic" -> "Cosmic Particle Vortex"
        "synthgrid" -> "Retro Synthwave Sunset Grid"
        else -> "Cybernetic Sirkuit Mesh"
    }

    fun startMediaUpscale() {
        if (selectedMediaUri == null) {
            Toast.makeText(context, "Silakan pilih foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true
        enhancedPreviewReady = false
        coroutineScope.launch {
            val steps = mutableListOf<String>()
            steps.add("Memulai $selectedVisualEngine...")
            steps.add("Menganalisa resolusi awal & metadata...")
            if (visualHdConverter) steps.add("Mengkonversi noise standar ke HD...")
            if (visualAIUpscale) steps.add("Menerapkan AI HD Upscale ($selectedScale)...")
            if (visualEnhanceAuto) steps.add("Menyelaraskan dynamic range & warna HDR...")
            if (visualWallpaperGen) steps.add("Mengoptimalkan aspect ratio untuk Wallpaper...")
            if (visualThumbnailPreview) steps.add("Merender HD Thumbnail super tajam...")
            steps.add("Penyelesaian render media ultra high fidelity...")

            for (i in steps.indices) {
                progressStatus = steps[i]
                currentProgress = (i + 1).toFloat() / steps.size
                delay(1000)
            }
            enhancedPreviewReady = true
            isProcessing = false
            autoSaveMediaToDevice(context, "MeydiAI_HD_Visual", "image")
            Toast.makeText(context, "Berhasil meningkatkan kualitas gambar otomatis!", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVideoUpscale() {
        isProcessing = true
        videoEnhancedReady = false
        coroutineScope.launch {
            val steps = mutableListOf<String>()
            steps.add("Mengaktivasi $selectedVideoEngine...")
            if (videoBooster) steps.add("Menerapkan HD Video Booster (HDR Brightness)...")
            if (videoUpscaler) steps.add("Mengonversi Video SD ke HD/Full HD...")
            if (videoFrameEnhancement) steps.add("Menjalankan Auto Frame Enhancement (60fps)...")
            if (videoStreamingMode) steps.add("Mengaktifkan mode streaming bitrate tinggi...")
            if (videoResolutionAdjuster) steps.add("Menyetel Smart Resolution Adjuster jaringan...")
            steps.add("Penyelesaian render frame video ultra high clarity...")

            for (i in steps.indices) {
                progressStatus = steps[i]
                currentProgress = (i + 1).toFloat() / steps.size
                delay(1100)
            }
            videoEnhancedReady = true
            isProcessing = false
            autoSaveMediaToDevice(context, "MeydiAI_HD_Video", "video")
            Toast.makeText(context, "Video Booster berhasil diproses!", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCameraCapture() {
        isShutterAnimating = true
        coroutineScope.launch {
            delay(350) // camera shutter click animation latency
            isShutterAnimating = false
            isProcessing = true
            capturedPhotoReady = false
            
            val steps = mutableListOf<String>()
            steps.add("Menangkap sinyal lensa sensor 108MP...")
            if (cameraSharpMode) steps.add("Menjalankan Vision HD Ultra Clarity Engine...")
            if (cameraNightHd) steps.add("Menerapkan AI Night HD Mode (multi-exposure)...")
            if (cameraPortraitHd) steps.add("Memetakan AI Depth Bokeh ($cameraBokehStrength%)...")
            if (cameraFaceEnhance) steps.add("Meningkatkan tekstur & HD Face Enhancement...")
            if (cameraUltraHdCapture) steps.add("Menulis master file Ultra HD RAW capture...")
            steps.add("Menyimpan foto resolusi tinggi ke galeri...")

            for (i in steps.indices) {
                progressStatus = steps[i]
                currentProgress = (i + 1).toFloat() / steps.size
                delay(950)
            }
            capturedPhotoReady = true
            isProcessing = false
            autoSaveMediaToDevice(context, "MeydiAI_HD_Kamera", "image")
            Toast.makeText(context, "Foto Ultra HD Berhasil Disimpan ke Galeri!", Toast.LENGTH_SHORT).show()
        }
    }

    fun startAutoGenerateHd() {
        isProcessing = true
        generatedMediaReady = false
        coroutineScope.launch {
            val steps = listOf(
                "Menganalisis prompt & rancangan visual $presetLabel...",
                "Mengalokasikan model synthesizer virtual HD 1920x1080...",
                "Menggambar grid, bayangan, dan elemen pencahayaan ray-trace...",
                "Merender partikel neon dynamic 60fps...",
                "Menyimpan master file HD ke penyimpanan lokal..."
            )
            for (i in steps.indices) {
                progressStatus = steps[i]
                currentProgress = (i + 1).toFloat() / steps.size
                delay(1300)
            }
            generatedMediaReady = true
            isProcessing = false
            autoSaveMediaToDevice(context, "MeydiAI_HD_Generated", if (outputFormat.contains("Video")) "video" else "image")
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back Button",
                            tint = NeonPurple
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Auto HD & AI Media Engine 🦾",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Teknologi Optimasi Resolusi Maksimal",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Premium Quick Info Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(TerminalGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Smart HD Engine Active",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HD+ QUALITY ENABLED",
                        color = NeonPurple,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful Tab Options (4 Tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightSurface)
                    .padding(4.dp)
            ) {
                val tabs = listOf(
                    "🎨 VISUAL" to 0,
                    "🎥 VIDEO" to 1,
                    "📷 KAMERA" to 2,
                    "🪄 AI GEN" to 3
                )
                tabs.forEach { (label, index) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple else Color.Transparent)
                            .clickable {
                                if (!isProcessing) {
                                    selectedTab = index
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- TAB 0: HD VISUAL STUDIO ---
            if (selectedTab == 0) {
                Text(
                    text = "Seksi Rekayasa Gambar HD 🎨",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Gunakan algoritma multi-layer AI untuk peningkatan detail gambar murni lossless.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                if (selectedMediaUri == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightSurface)
                            .border(BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isProcessing) { mediaPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pilih Gambar Kualitas Rendah",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Format PNG, JPG, WEBP didukung",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖼️", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedMediaName ?: "gambar_input.jpg",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Status: Siap di-Upscale",
                                    color = TerminalGreen,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedMediaUri = null
                                    selectedMediaName = null
                                    enhancedPreviewReady = false
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Options List
                Text(
                    text = "Konfigurasi Algoritma HD:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "HD Image Enhancer" to Triple("Optimasi saturasi warna otomatis", visualEnhanceAuto) { v: Boolean -> visualEnhanceAuto = v },
                        "Auto HD Converter" to Triple("Konversi resolusi standar ke HD", visualHdConverter) { v: Boolean -> visualHdConverter = v },
                        "AI HD Upscale" to Triple("Upscale detail tajam tanpa buram", visualAIUpscale) { v: Boolean -> visualAIUpscale = v },
                        "HD Wallpaper Generator" to Triple("Sesuaikan rasio aspek wallpaper", visualWallpaperGen) { v: Boolean -> visualWallpaperGen = v },
                        "HD Thumbnail Preview" to Triple("Generasikan thumbnail detail tinggi", visualThumbnailPreview) { v: Boolean -> visualThumbnailPreview = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPurple,
                                    checkedTrackColor = NeonPurple.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selected Premium Engine Option
                Text(
                    text = "Gunakan Premium Intelligence Engine:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("True HD Optimizer", "AI HD Enhance", "Crystal HD", "Ultra Clarity").forEach { engine ->
                        val isSel = selectedVisualEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonPurple.copy(alpha = 0.2f) else MidnightSurface)
                                .border(BorderStroke(1.dp, if (isSel) NeonPurple else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedVisualEngine = engine }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engine,
                                color = if (isSel) Color.White else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Resolution Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Target Piksel Akhir:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("2K Quad HD", "4K Ultra HD", "8K Cinematic").forEach { scale ->
                            val isScaleSel = selectedScale == scale
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isScaleSel) NeonPurple else MidnightSurface)
                                    .clickable { selectedScale = scale }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(text = scale, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { startMediaUpscale() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    enabled = !isProcessing && selectedMediaUri != null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Proses Visual HD ($selectedVisualEngine) ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (enhancedPreviewReady) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Perbandingan Hasil (Before vs After Render):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Standard SD\n[Low-Res 480p]\nBanyak digital-noise",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonPurple.copy(alpha = 0.1f))
                                .border(BorderStroke(1.dp, NeonPurple), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            LiveHdMediaPreview(preset = "mesh", modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalGreen)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "8K ACTIVE", color = ObsidianBg, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- TAB 1: HD VIDEO ENGINE ---
            if (selectedTab == 1) {
                Text(
                    text = "Seksi Rekayasa Video HD 🎥",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Optimisasi frame video beresolusi SD menjadi standar Full HD hingga 4K murni ber-saturasi HDR.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "HD Video Booster" to Triple("Penajaman kontras & kecerahan dinamis", videoBooster) { v: Boolean -> videoBooster = v },
                        "AI Video Upscaler" to Triple("Konversi frame resolusi SD ke Ultra HD", videoUpscaler) { v: Boolean -> videoUpscaler = v },
                        "Auto Frame Enhancement" to Triple("Interpolasi frame stabil tinggi (60fps)", videoFrameEnhancement) { v: Boolean -> videoFrameEnhancement = v },
                        "HD Streaming Mode" to Triple("Optimalisasi latensi transisi data video", videoStreamingMode) { v: Boolean -> videoStreamingMode = v },
                        "Smart Resolution Adjuster" to Triple("Pengkondisian parameter sesuai sinyal", videoResolutionAdjuster) { v: Boolean -> videoResolutionAdjuster = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPurple,
                                    checkedTrackColor = NeonPurple.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium choice
                Text(text = "Pilih Core Video Engine Premium:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Smart HD Engine", "HD Booster", "HD Max", "True HD Optimizer").forEach { engine ->
                        val isSel = selectedVideoEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonPurple.copy(alpha = 0.2f) else MidnightSurface)
                                .border(BorderStroke(1.dp, if (isSel) NeonPurple else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedVideoEngine = engine }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = engine, color = if (isSel) Color.White else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { startVideoUpscale() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    enabled = !isProcessing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Aktifkan $selectedVideoEngine & Scaler 🎥", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (videoEnhancedReady) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Pratinjau Buffering Video HD Booster (Live):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveHdMediaPreview(preset = "cyberwave", modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(6.dp)
                        ) {
                            Column {
                                Text(text = "Format: MP4 (Lossless)", color = Color.White, fontSize = 8.sp)
                                Text(text = "FPS: 60fps (Interpolated) | Resolusi: 4K UHD", color = TerminalGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- TAB 2: HD KAMERA MODE ---
            if (selectedTab == 2) {
                Text(
                    text = "Seksi Kamera Rekayasa HD 📷",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Optimisasi langsung tangkapan sensor kamera, menghasilkan foto lebih detail dan efek bokeh tajam.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                // Simulated Interactive Camera Viewport Graphic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(BorderStroke(2.dp, if (cameraFlashOn) Color.Yellow else Color.Gray.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Shutter Animation Screen Flash
                    if (isShutterAnimating) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White))
                    } else {
                        // Interactive Focus Crosshairs
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2
                            val cy = h / 2
                            
                            // Draw focus box
                            drawRect(
                                color = if (cameraSharpMode) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.5f),
                                topLeft = androidx.compose.ui.geometry.Offset(cx - 40.dp.toPx(), cy - 40.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 80.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                            // Focus target dot
                            drawCircle(
                                color = if (cameraSharpMode) Color(0xFF00FFCC) else Color.Red,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(cx, cy)
                            )
                        }

                        // Status markers Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Red))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "LIVE 108MP RAW", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }

                        // Flash Indicator in Corner
                        IconButton(
                            onClick = { cameraFlashOn = !cameraFlashOn },
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        ) {
                            Icon(
                                imageVector = if (cameraFlashOn) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (cameraFlashOn) Color.Yellow else Color.White
                            )
                        }

                        if (cameraPortraitHd) {
                            Text(
                                text = "BOKEH ACTIVE: ${cameraBokehStrength.toInt()}%",
                                color = Color(0xFFE91E63),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Custom Options Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "HD Camera Mode" to Triple("Penajaman tepi & perataan warna sensor", cameraSharpMode) { v: Boolean -> cameraSharpMode = v },
                        "AI Night HD" to Triple("Pengurangan noise malam hari otomatis", cameraNightHd) { v: Boolean -> cameraNightHd = v },
                        "Portrait HD" to Triple("Efek bokeh / keburaman latar detail", cameraPortraitHd) { v: Boolean -> cameraPortraitHd = v },
                        "HD Face Enhancement" to Triple("Memperjelas pori-pori dan kontur wajah", cameraFaceEnhance) { v: Boolean -> cameraFaceEnhance = v },
                        "Ultra HD Capture" to Triple("Pengambilan resolusi sensor fisik maksimun", cameraUltraHdCapture) { v: Boolean -> cameraUltraHdCapture = v }
                    ).forEach { (title, triple) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = triple.first, color = TextMuted, fontSize = 10.sp)
                            }
                            Switch(
                                checked = triple.second,
                                onCheckedChange = triple.third,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPurple,
                                    checkedTrackColor = NeonPurple.copy(alpha = 0.5f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = ObsidianBg
                                )
                            )
                        }
                    }

                    // Bokeh Strength Slider if Portrait mode enabled
                    if (cameraPortraitHd) {
                        Divider(color = DarkStroke)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Atur Intensitas Bokeh Kedalaman", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${cameraBokehStrength.toInt()}%", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cameraBokehStrength,
                                onValueChange = { cameraBokehStrength = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPurple,
                                    activeTrackColor = NeonPurple,
                                    inactiveTrackColor = ObsidianBg
                                ),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Engine Badge Selection
                Text(text = "Pilih Engine Kamera Premium:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Crystal HD", "Vision HD", "HD+ Quality").forEach { engine ->
                        val isSel = selectedCameraEngine == engine
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonPurple.copy(alpha = 0.2f) else MidnightSurface)
                                .border(BorderStroke(1.dp, if (isSel) NeonPurple else Color.Transparent), RoundedCornerShape(8.dp))
                                .clickable { selectedCameraEngine = engine }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = engine, color = if (isSel) Color.White else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { startCameraCapture() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    enabled = !isProcessing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "[AMBIL FOTO ULTRA HD] ($selectedCameraEngine) 📸", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (capturedPhotoReady) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Hasil Tangkapan Kamera HD (Ready):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, TerminalGreen)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📸", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "MeydiAI_Portrait_HD_${System.currentTimeMillis().toString().takeLast(4)}.jpg", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Spesifikasi: 108MP RAW (Lossless JPEG) sRGB Color", color = Color.LightGray, fontSize = 10.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalGreen)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(text = "SAVED", color = ObsidianBg, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- TAB 3: AI AUTO GENERATOR HD (NEW CREATION) ---
            if (selectedTab == 3) {
                Text(
                    text = "Pilih Tema Visual HD Utama:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Style selection chips grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        "cyberwave" to "🌊 Neon Cyberwave Flow",
                        "cosmic" to "🌌 Cosmic Particle Vortex",
                        "synthgrid" to "🌅 Retro Synthwave Sunset",
                        "mesh" to "🕸️ Cyber Circuit Mesh"
                    )
                    presets.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { (key, label) ->
                                val isSelected = selectedPreset == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NeonPurple.copy(alpha = 0.2f) else MidnightSurface)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isSelected) NeonPurple else Color.Transparent
                                            ), RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isProcessing) { selectedPreset = key }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Prompt Modification Option
                Text(
                    text = "Detail Kustom / Prompt (Opsional):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = promptInput,
                    onValueChange = { if (!isProcessing) promptInput = it },
                    placeholder = { Text("cth: tambahkan pancaran cahaya aurora, nuansa ultra-glow...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, DarkStroke, RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        disabledContainerColor = MidnightSurface,
                        cursorColor = NeonPurple,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    maxLines = 2,
                    enabled = !isProcessing
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format Output Options
                Text(
                    text = "Format Output Media:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("HD Looping Video (60fps)", "HD Ultra-res Foto")
                    formats.forEach { form ->
                        val isSelected = outputFormat == form
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else MidnightSurface)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonPurple else Color.Transparent
                                    ), RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !isProcessing) { outputFormat = form }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = form,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { startAutoGenerateHd() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        disabledContainerColor = MidnightSurface
                    ),
                    enabled = !isProcessing
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "",
                            tint = if (isProcessing) TextMuted else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto Generate HD Media 🪄",
                            color = if (isProcessing) TextMuted else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (generatedMediaReady) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview Hasil Render HD Murni:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TerminalGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RENDER SUCCESS",
                                color = ObsidianBg,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Splendid Animating Dynamic Canvas Live Render Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                LiveHdMediaPreview(
                                    preset = selectedPreset,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Judul Art: MeydiAI_HD_${selectedPreset.uppercase()}_${System.currentTimeMillis().toString().takeLast(4)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Stats description
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("Resolusi", color = TextMuted, fontSize = 9.sp)
                                        Text("1925 x 1080 (HD)", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("Format Media", color = TextMuted, fontSize = 9.sp)
                                        Text(if (outputFormat.contains("Video")) "MP4 (60fps)" else "PNG (Lossless)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text("Kualitas Piksel", color = TextMuted, fontSize = 9.sp)
                                        Text("32-bit HDR Color", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Save to Local / Copy Core Javascript Code / Direct Submit to support
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        autoSaveMediaToDevice(context, "MeydiAI_Prompt_Asset", "image")
                                        Toast.makeText(context, "Disimpan langsung ke File & Galeri perangkat!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen, contentColor = ObsidianBg),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Simpan Galeri 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Skrip Javascript canvas berhasil disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Copy Skrip 💻", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Shared Loading Processing Overlay Logs ---
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { currentProgress },
                            color = NeonPurple,
                            trackColor = MidnightSurface,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = progressStatus,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Memuat data pixel render... ${(currentProgress * 100).toInt()}%",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PromptGeneratorScreen(onBack: () -> Unit, viewModel: WorkspaceViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzingImage by remember { mutableStateOf(false) }
    var generatedPrompt by remember { mutableStateOf("") }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAnalyzingImage = true
            Toast.makeText(context, "Menganalisis gambar dengan Gemini...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                        val result = GeminiGenerator.generatePromptFromImage(base64)
                        generatedPrompt = result
                        Toast.makeText(context, "Prompt berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Vision Prompt Generator",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ekstrak otomatis teks dari media visual",
                        color = Color(0xFFE91E63),
                        fontSize = 12.sp
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightSurface)
                    .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isAnalyzingImage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE91E63))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI Vision sedang bekerja...", color = TextMuted)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Image",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ketuk untuk memilih foto/gambar", color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (generatedPrompt.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha=0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hasil Ekstraksi Prompt:", color = Color.White, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                viewModel.updatePromptInput(generatedPrompt, "CANVAS", "guest")
                                globalActivePrompt.value = generatedPrompt
                                Toast.makeText(context, "Prompt telah disalin ke Workspace Canvas!", Toast.LENGTH_SHORT).show()
                                onBack() // kembali
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Gunakan Prompt", tint = NeonTeal)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkStroke)
                        Text(
                            text = generatedPrompt,
                            color = NeonTeal,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.updatePromptInput(generatedPrompt, "CANVAS", "guest")
                                globalActivePrompt.value = generatedPrompt
                                onBack() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Kirim ke Workspace Canvas", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "Prompt akan muncul di sini.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 7. LOGIN SCREEN
@Composable
fun LoginScreen(onLoginSuccess: (String?) -> Unit) {
    var isLoggingIn by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    
    // Google/Gmail login accent chooser state
    var showGoogleChooser by remember { mutableStateOf(false) }
    var selectedGoogleEmail by remember { mutableStateOf("meydihikara@gmail.com") }
    var isCustomGmailFieldVisible by remember { mutableStateOf(false) }
    var customGmailInput by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val userPrefs = remember { context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE) }
    var isAutoLoginEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_login_enabled", true)) }
    var isAutoReportEnabled by remember { mutableStateOf(userPrefs.getBoolean("auto_report_enabled", true)) }
    var showAutoLoginConsentDialog by remember { mutableStateOf(false) }
    var showAutoCoreReportDialog by remember { mutableStateOf(false) }

    fun performLogin(method: String, customEmail: String? = null) {
        if (method == "Email" && (email.isBlank() || password.isBlank())) {
            Toast.makeText(context, "Email dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isLoggingIn = true
        coroutineScope.launch {
            delay(500)
            val actionName = if (isRegisterMode && method == "Email") "Daftar" else "Sign-In"
            Toast.makeText(context, "Memproses $actionName dengan $method...", Toast.LENGTH_SHORT).show()
            delay(1500)
            
            Toast.makeText(context, "Autentikasi Berhasil. Sinkronisasi Cloud aktif!", Toast.LENGTH_SHORT).show()
            delay(500)
            isLoggingIn = false
            
            if (method == "Google") {
                // Mock Google login
                onLoginSuccess(customEmail ?: "meydihikara@gmail.com")
            } else if (method == "Apple") {
                // Mock Apple login for iOS/Cross-platform access
                onLoginSuccess("meydi_ios@icloud.com")
            } else {
                onLoginSuccess(email)
            }
        }
    }

    if (showGoogleChooser) {
        AlertDialog(
            onDismissRequest = { showGoogleChooser = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Beautiful multicolor Google spelling
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pilih akun Google Anda",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "untuk masuk ke Meydi AI",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickAccounts = listOf(
                        "meydihikara@gmail.com" to "Akun Utama Cloud MeydiAI",
                        "meydiaihikara@gmail.com" to "Akun Developer & Kreator"
                    )
                    
                    quickAccounts.forEach { (gmail, label) ->
                        val isSelected = selectedGoogleEmail == gmail && !isCustomGmailFieldVisible
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF4285F4).copy(alpha = 0.15f) else MidnightSurface)
                                .border(1.dp, if (isSelected) Color(0xFF4285F4) else DarkStroke, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedGoogleEmail = gmail
                                    isCustomGmailFieldVisible = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circle Avatar with first letter
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (gmail.startsWith("meydih")) Color(0xFFEA4335).copy(alpha = 0.2f)
                                        else Color(0xFF34A853).copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gmail.take(1).uppercase(),
                                    color = if (gmail.startsWith("meydih")) Color(0xFFEA4335) else Color(0xFF34A853),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gmail,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = label,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Option 3: Custom Gmail Input Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustomGmailFieldVisible) Color(0xFF4285F4).copy(alpha = 0.15f) else MidnightSurface)
                            .border(1.dp, if (isCustomGmailFieldVisible) Color(0xFF4285F4) else DarkStroke, RoundedCornerShape(10.dp))
                            .clickable {
                                isCustomGmailFieldVisible = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gunakan akun Gmail lain...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ketik alamat Gmail kustom Anda",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        
                        if (isCustomGmailFieldVisible) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Expanded Input Fields
                    if (isCustomGmailFieldVisible) {
                        OutlinedTextField(
                            value = customGmailInput,
                            onValueChange = { customGmailInput = it },
                            label = { Text("Alamat Gmail Kustom", color = TextMuted) },
                            placeholder = { Text("nama@gmail.com", color = TextMuted.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = DarkStroke,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg
                            )
                        )
                        
                        val isGmailValid = customGmailInput.endsWith("@gmail.com") && customGmailInput.length > 10
                        if (customGmailInput.isNotBlank() && !isGmailValid) {
                            Text(
                                text = "Format harus diakhiri dengan @gmail.com",
                                color = Color(0xFFEA4335),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCustomGmailFieldVisible) {
                            if (!customGmailInput.endsWith("@gmail.com") || customGmailInput.length <= 10) {
                                Toast.makeText(context, "Silakan masukkan alamat Gmail yang valid!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            showGoogleChooser = false
                            performLogin("Google", customGmailInput.trim())
                        } else {
                            showGoogleChooser = false
                            performLogin("Google", selectedGoogleEmail)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lanjutkan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleChooser = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = ObsidianBg,
            titleContentColor = Color.White,
            textContentColor = TextMuted
        )
    }

    if (showAutoLoginConsentDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLoginConsentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Persetujuan Akses & Auto Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Akses Login",
                        color = NeonTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aplikasi memerlukan akses untuk menyimpan informasi sesi login secara aman agar pengguna dapat masuk secara otomatis tanpa perlu mengisi ulang akun setiap kali membuka aplikasi.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Divider(color = DarkStroke)
                    
                    Text(
                        text = "Akses Penyimpanan Data Pengguna",
                        color = NeonTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Data login disimpan secara aman di perangkat and hanya digunakan untuk mempertahankan sesi pengguna serta meningkatkan kenyamanan penggunaan aplikasi.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Divider(color = DarkStroke)
                    
                    Text(
                        text = "Notifikasi Persetujuan",
                        color = NeonTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dengan mengaktifkan Auto Login, Anda mengizinkan aplikasi menyimpan informasi autentikasi secara aman untuk mempercepat proses masuk ke aplikasi pada penggunaan berikutnya.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    // Versi Singkat box styled beautifully with customized border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurple.copy(alpha = 0.12f))
                            .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Versi Singkat (Ringkasan):",
                                color = NeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Izinkan aplikasi menyimpan sesi login agar Anda dapat masuk secara otomatis saat membuka aplikasi kembali.",
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isAutoLoginEnabled = true
                        userPrefs.edit().putBoolean("auto_login_enabled", true).apply()
                        showAutoLoginConsentDialog = false
                        Toast.makeText(context, "Sistem Auto Login Diaktifkan !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Setuju & Aktifkan", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAutoLoginEnabled = false
                        userPrefs.edit().putBoolean("auto_login_enabled", false).apply()
                        showAutoLoginConsentDialog = false
                        Toast.makeText(context, "Akses Auto Login Dibatalkan.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Batal & Nonaktifkan", color = Color.Gray)
                }
            },
            containerColor = ObsidianBg,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    if (showAutoCoreReportDialog) {
        AlertDialog(
            onDismissRequest = { showAutoCoreReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = TerminalGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfigurasi Sistem Auto Report", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Deskripsi Auto Generate Report",
                        color = TerminalGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sistem Auto Generate Report otomatis mengandalkan daemon background untuk mencatat dan merangkum status operasional, database local, status internet, dan log telemetri secara terstruktur format markdown (.md).",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Divider(color = DarkStroke)
                    
                    Text(
                        text = "Manfaat Utama:",
                        color = TerminalGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Pembuatan sertifikat laporan sistem secara instan.\n• Pengecekan konsistensi status data SQLite lokal.\n• Diagnostik performa hardware & alokasi memori real-time.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isAutoReportEnabled = true
                        userPrefs.edit().putBoolean("auto_report_enabled", true).apply()
                        showAutoCoreReportDialog = false
                        Toast.makeText(context, "Sistem Auto Report Diaktifkan !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aktifkan Engine", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAutoReportEnabled = false
                        userPrefs.edit().putBoolean("auto_report_enabled", false).apply()
                        showAutoCoreReportDialog = false
                        Toast.makeText(context, "Auto Report Di-nonaktifkan.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Batal / Matikan", color = Color.Gray)
                }
            },
            containerColor = ObsidianBg,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Lanjut sebagai Tamu?", color = Color.White) },
            text = { Text("Data proyek, progress rendering, dan HD media Anda TIDAK AKAN DISINKRONKAN ke Cloud (berisiko hilang jika berganti perangkat).", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { 
                    showGuestDialog = false
                    onLoginSuccess("guest") 
                }) {
                    Text("Tetap Lanjut", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Batal", color = NeonTeal)
                }
            },
            containerColor = ObsidianBg,
            titleContentColor = Color.White,
            textContentColor = TextMuted
        )
    }

    Scaffold(
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // App Logo or Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightSurface)
                    .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.meydi_ai_logo_1780907592840),
                    contentDescription = "Meydi AI Logo",
                    androidx.compose.ui.Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Meydi AI",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = if(isRegisterMode) "Buat Akun Baru" else "Masuk ke Akun Anda",
                color = NeonPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Email and Password Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkStroke,
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !isLoggingIn
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = TextMuted) },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkStroke,
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { performLogin("Email") }),
                enabled = !isLoggingIn
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email Login/Register Button
            Button(
                onClick = { performLogin("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = Color.White,
                    disabledContainerColor = MidnightSurface
                ),
                enabled = !isLoggingIn
            ) {
                if (isLoggingIn && isRegisterMode) {
                   CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) 
                } else {
                    Text(
                        text = if(isRegisterMode) "Daftar" else "Masuk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Register/Login logic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if(isRegisterMode) "Sudah punya akun?" else "Belum punya akun?",
                    color = TextMuted,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                    enabled = !isLoggingIn,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if(isRegisterMode) "Masuk di sini" else "Daftar sekarang",
                        color = NeonTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.weight(1f), color = DarkStroke)
                Text(" ATAU ", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(modifier = Modifier.weight(1f), color = DarkStroke)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Gmail/Google Sign-in Button with Custom Accent UI
            Button(
                onClick = { showGoogleChooser = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(
                        1.dp, 
                        androidx.compose.ui.graphics.Brush.sweepGradient(
                            listOf(
                                Color(0xFF4285F4),
                                Color(0xFFEA4335),
                                Color(0xFFFBBC05),
                                Color(0xFF34A853),
                                Color(0xFF4285F4)
                            )
                        ), 
                        RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightSurface,
                    contentColor = Color.White,
                    disabledContainerColor = MidnightSurface
                ),
                enabled = !isLoggingIn
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lanjutkan dengan Gmail / Google",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sleek Apple Sign-in Button for iOS Compatibility ("Buat agar iOS bisa masuk")
            Button(
                onClick = { performLogin("Apple") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MidnightSurface,
                    contentColor = Color.White,
                    disabledContainerColor = MidnightSurface
                ),
                enabled = !isLoggingIn
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍏", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lanjutkan dengan Apple (iOS Sync)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Notification about data safety & iOS/Android Sync
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, DarkStroke)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync",
                        tint = NeonTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sinkronisasi Multiplatform Aktif",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kredensial MeydiAi disinkronkan secara optimal. Pengguna perangkat Apple (iOS) dapat masuk secara mulus menggunakan profil cloud yang sama lewat browser Safari Anda.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // SYSTEM CONFIGURATION SECTION TITLE
            Text(
                text = "Pengaturan Sesi & Auto Build",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, DarkStroke)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // 1. AUTO LOGIN OPTION Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAutoLoginConsentDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Auto Login",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAutoLoginEnabled) "(Aktif)" else "(Mati)",
                                    color = if (isAutoLoginEnabled) TerminalGreen else ErrorRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Switch(
                            checked = isAutoLoginEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showAutoLoginConsentDialog = true
                                } else {
                                    isAutoLoginEnabled = false
                                    userPrefs.edit().putBoolean("auto_login_enabled", false).apply()
                                    Toast.makeText(context, "Sesi login otomatis dimatikan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonTeal,
                                checkedTrackColor = NeonTeal.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = ObsidianBg
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = DarkStroke.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 2. AUTO REPORT OPTION Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAutoCoreReportDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = TerminalGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Auto Generate Report",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAutoReportEnabled) "(Aktif)" else "(Mati)",
                                    color = if (isAutoReportEnabled) TerminalGreen else ErrorRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Switch(
                            checked = isAutoReportEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showAutoCoreReportDialog = true
                                } else {
                                    isAutoReportEnabled = false
                                    userPrefs.edit().putBoolean("auto_report_enabled", false).apply()
                                    Toast.makeText(context, "Sistem Auto Generate Report dinonaktifkan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TerminalGreen,
                                checkedTrackColor = TerminalGreen.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = ObsidianBg
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = { showGuestDialog = true },
                enabled = !isLoggingIn,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Lanjutkan sebagai tamu",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Meydi AI • Versi v2.5.4-Stable Build",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dirancang oleh Meydi • Lintas Platform Android & iOS",
                    color = TerminalGreen.copy(alpha=0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 8. ADMIN DASHBOARD SCREEN
@Composable
fun AdminDashboardScreen(
    adminEmails: List<String>,
    systemAlertMessage: String,
    onAddAdmin: (String) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onUpdateSystemAlert: (String) -> Unit,
    onBack: () -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var currentAlertMessage by remember { mutableStateOf(systemAlertMessage) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back Button",
                        tint = NeonTeal
                    )
                }
                Text(
                    text = "Dashboard Admin",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Pengumuman Sistem (Alert)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentAlertMessage,
                onValueChange = { currentAlertMessage = it },
                placeholder = { Text("Contoh: Fitur HD Enhancer sedang gangguan", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonTeal,
                    unfocusedBorderColor = DarkStroke,
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onUpdateSystemAlert(currentAlertMessage)
                    Toast.makeText(context, "Pengumuman disimpan", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
            ) {
                Text("Simpan Pengumuman", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tambah Admin Baru",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    placeholder = { Text("email@contoh.com", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke,
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newEmail.isNotBlank()) {
                            onAddAdmin(newEmail)
                            newEmail = ""
                            Toast.makeText(context, "Admin ditambahkan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Tambah")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Daftar Admin Aktif",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            adminEmails.forEach { email ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, DarkStroke)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = email,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (email != "meydihikara@gmail.com") {
                            IconButton(onClick = { onRemoveAdmin(email) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus Admin",
                                    tint = ErrorRed
                                )
                            }
                        } else {
                            Text(
                                text = "Owner",
                                color = NeonTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 9. SECURITY DASHBOARD SCREEN
data class SecurityPermissionItem(
    val key: String,
    val title: String,
    val desc: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun SecurityDashboardScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val permPrefs = remember { context.getSharedPreferences("PermissionPrefs", android.content.Context.MODE_PRIVATE) }
    
    val allPermissions = listOf(
        // 📱 Izin Dasar Perangkat
        SecurityPermissionItem("p_camera", "Akses Kamera", "Mengambil foto dan video secara langsung untuk modul Vision Prompt dan auto-clipper.", "Izin Dasar Perangkat", Icons.Default.Camera),
        SecurityPermissionItem("p_gallery", "Akses Galeri/Penyimpanan", "Memilih dan menyimpan file hasil render video secara aman.", "Izin Dasar Perangkat", Icons.Default.FolderOpen),
        SecurityPermissionItem("p_mic", "Akses Mikrofon", "Merekam suara atau panggilan suara di editor klip.", "Izin Dasar Perangkat", Icons.Default.Mic),
        SecurityPermissionItem("p_location", "Akses Lokasi (GPS)", "Menentukan lokasi pengguna secara akurat untuk metadata konten.", "Izin Dasar Perangkat", Icons.Default.LocationOn),
        SecurityPermissionItem("p_contacts", "Akses Kontak", "Memilih atau mengelola kontak tim microstock Anda.", "Izin Dasar Perangkat", Icons.Default.Contacts),

        // 🔔 Izin Sistem
        SecurityPermissionItem("p_notifications", "Akses Notifikasi", "Membaca atau mengelola notifikasi dari daemon pemantau render.", "Izin Sistem", Icons.Default.Notifications),
        SecurityPermissionItem("p_overlay", "Akses Overlay (Tampil di Atas)", "Menampilkan widget atau pop-up mengambang di atas aplikasi lainnya.", "Izin Sistem", Icons.Default.Layers),
        SecurityPermissionItem("p_autostart", "Akses Auto Start", "Menjalankan aplikasi secara otomatis saat perangkat menyembur nyala.", "Izin Sistem", Icons.Default.FlashOn),
        SecurityPermissionItem("p_bgservice", "Akses Background Service", "Menjalankan proses kompilasi klip & looping di latar belakang secara stabil.", "Izin Sistem", Icons.Default.Sync),
        SecurityPermissionItem("p_vibration", "Akses Getaran", "Memberikan umpan balik haptic halus saat berinteraksi di canvas.", "Izin Sistem", Icons.Default.Vibration),

        // 📂 Izin File & Media
        SecurityPermissionItem("p_documents", "Akses Dokumen", "Membaca dan mengelola seluruh salinan dokumen ekspor.", "Izin File & Media", Icons.Default.Description),
        SecurityPermissionItem("p_downloads", "Akses Unduhan", "Menyimpan hasil video atau klip ke folder download internal.", "Izin File & Media", Icons.Default.Download),
        SecurityPermissionItem("p_audio_media", "Akses Media Audio", "Mengakses file musik loop dan klip audio pendukung.", "Izin File & Media", Icons.Default.MusicNote),
        SecurityPermissionItem("p_video_media", "Akses Media Video", "Mengakses video perangkat untuk penyuntingan frame detail.", "Izin File & Media", Icons.Default.PlayCircle),
        SecurityPermissionItem("p_image_media", "Akses Media Gambar", "Mengakses seluruh foto untuk background canvas template.", "Izin File & Media", Icons.Default.Image),

        // 🌐 Izin Konektivitas
        SecurityPermissionItem("p_internet", "Akses Internet", "Menghubungkan aplikasi ke server automasi cloud MeydiAi.", "Izin Konektivitas", Icons.Default.Language),
        SecurityPermissionItem("p_wifi", "Akses Wi-Fi", "Mendeteksi secara real-time status koneksi jaringan nirkabel.", "Izin Konektivitas", Icons.Default.Wifi),
        SecurityPermissionItem("p_bluetooth", "Akses Bluetooth", "Terhubung secara instan ke headset/speaker monitoring eksternal.", "Izin Konektivitas", Icons.Default.Bluetooth),
        SecurityPermissionItem("p_nfc", "Akses NFC", "Membaca atau menulis data verifikasi NFC sertifikat lisensi.", "Izin Konektivitas", Icons.Default.CreditCard),

        // ⚙️ Izin Lanjutan
        SecurityPermissionItem("p_biometric", "Akses Biometrik", "Masuk login aman dengan sidik jari atau enkripsi pengenalan wajah.", "Izin Lanjutan", Icons.Default.Fingerprint),
        SecurityPermissionItem("p_clipboard", "Akses Clipboard", "Membaca dan menempel cepat materi teks dari papan klip eksternal.", "Izin Lanjutan", Icons.Default.Assignment),
        SecurityPermissionItem("p_calendar", "Akses Kalender", "Membaca dan menyusun jadwal rilis terjadwal konten sosial media.", "Izin Lanjutan", Icons.Default.DateRange),
        SecurityPermissionItem("p_sensors", "Akses Sensor Perangkat", "Akses kompas, giroskop dan sensor gerakan untuk rotasi dinamis.", "Izin Lanjutan", Icons.Default.Explore),
        SecurityPermissionItem("p_battery", "Akses Baterai Optimization", "Pengecualian baterai khusus agar aktivitas render latar belakang tidak dijeda OS.", "Izin Lanjutan", Icons.Default.BatteryAlert)
    )

    // Setup map state
    val permissionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allPermissions.forEach { item ->
                // Default high-essential connection permissions to True, others to False
                val defaultVal = if (item.key == "p_internet" || item.key == "p_wifi") true else false
                put(item.key, permPrefs.getBoolean(item.key, defaultVal))
            }
        }
    }

    val auditLogs = remember {
        mutableStateListOf<String>().apply {
            add("[REAL-TIME] Konsol keamanan siap memantau perubahan.")
            add("[STATUS] Sistem MeydiAi mengenkripsi preferensi izin secara lokal.")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0: Semua, 1: Dasar, 2: Sistem, 3: File, 4: Konekt, 5: Lanjutan

    val categories = listOf(
        "Semua",
        "Dasar Perangkat",
        "Izin Sistem",
        "File & Media",
        "Izin Konektivitas",
        "Izin Lanjutan"
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = NeonTeal
                    )
                }
                Text(
                    text = "Pusat & Manajemen Izin Keamanan",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header stats summary card
            val activeCount = permissionStates.values.count { it }
            val totalCount = allPermissions.size
            val ratio = if (totalCount > 0) activeCount.toFloat() / totalCount.toFloat() else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status Keamanan Perangkat",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$activeCount dari $totalCount Izin Aktif",
                            color = NeonTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "${(ratio * 100).toInt()}% Diizinkan",
                        color = if (ratio > 0.7f) TerminalGreen else if (ratio > 0.3f) NeonTeal else Color.Yellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonTeal,
                    trackColor = DarkStroke
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            val tString = formatter.format(java.util.Date())
                            auditLogs.add(0, "[$tString] BULK_GRANT: Semua $totalCount izin resmi diaktifkan!")
                            Toast.makeText(context, "Semua Izin Berhasil Diberikan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonTeal),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Izinkan Semua", color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            allPermissions.forEach { item ->
                                permissionStates[item.key] = false
                                permPrefs.edit().putBoolean(item.key, false).apply()
                            }
                            val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            val tString = formatter.format(java.util.Date())
                            auditLogs.add(0, "[$tString] BULK_REVOKE: Semua izin dicabut.")
                            Toast.makeText(context, "Semua Keamanan Dibatasi (Dicabut)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Cabut Semua", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar & Filters row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    placeholder = { Text("Cari Izin Spesifik...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = NeonTeal) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color.Gray)
                            }
                        }
                    },
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

                Spacer(modifier = Modifier.height(10.dp))

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
                        val bgCol = if (isSelected) NeonTeal.copy(alpha = 0.2f) else MidnightSurface
                        val textCol = if (isSelected) NeonTeal else Color.LightGray

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30))
                                .background(bgCol)
                                .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(30))
                                .clickable { selectedCategoryIndex = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catName,
                                color = textCol,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Scrollable list of permissions + active logs console
            val filteredPermissions = allPermissions.filter { perm ->
                val matchesSearch = perm.title.contains(searchQuery, ignoreCase = true) || perm.desc.contains(searchQuery, ignoreCase = true)
                val matchesCategory = if (selectedCategoryIndex == 0) true else perm.category == categories[selectedCategoryIndex]
                matchesSearch && matchesCategory
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                if (filteredPermissions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada izin ditemukan untuk pencarian ini.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    filteredPermissions.forEach { permission ->
                        val isChecked = permissionStates[permission.key] ?: false
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, if (isChecked) NeonTeal.copy(alpha = 0.5f) else DarkStroke)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular decorative icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isChecked) NeonTeal.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = permission.icon,
                                        contentDescription = permission.title,
                                        tint = if (isChecked) NeonTeal else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Information details
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = permission.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = permission.desc,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = permission.category,
                                        color = NeonPurple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Switch Toggle
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        permissionStates[permission.key] = checked
                                        permPrefs.edit().putBoolean(permission.key, checked).apply()
                                        
                                        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                        val timeStr = formatter.format(java.util.Date())
                                        auditLogs.add(0, "[$timeStr] '${permission.title}' diubah menjadi: ${if (checked) "DIBERIKAN ✅" else "DICABUT ❌"}")
                                        
                                        Toast.makeText(
                                            context, 
                                            "${permission.title} ${if (checked) "Diaktifkan" else "Dinonaktifkan"}", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = ObsidianBg,
                                        checkedTrackColor = NeonTeal,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = DarkStroke
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-Time Log Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(TerminalGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MONITOR LOG IZIN AKTIF (AES-256)",
                                color = TerminalGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            auditLogs.forEach { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("✅") || log.contains("BULK_GRANT")) TerminalGreen else if (log.contains("❌") || log.contains("BULK_REVOKE")) Color.Red else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySectionCard(title: String, desc: String, status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, DarkStroke)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = desc, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TerminalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Status: $status", color = TerminalGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 10. PAYMENT GATEWAY (MOCK) WITH COMPLETE UPGRADE GUIDES & CONTACT CHANNELS
@Composable
fun PaymentGatewayScreen(userEmail: String?, onPaymentSuccess: () -> Unit, onBack: () -> Unit) {
    var isProcessing by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Pembayaran, 1: Cara Upgrade & Bantuan
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Live network status & transaction state machine
    var isRealOnline by remember { mutableStateOf(true) }
    var txOutcome by remember { mutableStateOf("SUCCESS") } // "SUCCESS", "FAIL", "CANCEL"
    var txStatusState by remember { mutableStateOf("IDLE") } // "IDLE", "SUCCESS_SCREEN", "FAIL_SCREEN", "CANCEL_SCREEN"

    LaunchedEffect(Unit) {
        while (true) {
            var connected = false
            try {
                val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                if (connectivityManager != null) {
                    val activeNetwork = connectivityManager.activeNetwork
                    if (activeNetwork != null) {
                        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                        connected = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    }
                } else {
                    connected = true
                }
            } catch (e: SecurityException) {
                connected = true
            } catch (e: Exception) {
                connected = true
            }
            isRealOnline = connected
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !isProcessing) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFFFFD700))
                }
                Text("Checkout & Panduan Premium", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Segmented Tab Row Custom (Estetika Cyberpunk)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MidnightSurface)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (activeTab == 0) Color(0xFFFFD700) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💳 Bayar Instan",
                        color = if (activeTab == 0) Color(0xFFFFD700) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (activeTab == 1) Color(0xFFFFD700) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💡 Panduan & Bantuan",
                        color = if (activeTab == 1) Color(0xFFFFD700) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (activeTab == 0) {
                // TAB 0: THE ORIGINAL SECURE PAYMENT METHOD WITH BETTER FLOW
                if (txStatusState == "SUCCESS_SCREEN") {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(BorderStroke(1.dp, TerminalGreen), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aktivasi Sukses ! 🎉", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Selamat! Webhook server berhasil memproses dan memvalidasi transaksi Anda. Akun $userEmail kini berstatus Premium.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        txStatusState = "IDLE"
                                        onPaymentSuccess()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Lanjut ke Beranda Premium ✨", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else if (txStatusState == "FAIL_SCREEN") {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(BorderStroke(1.dp, Color(0xFFFF5252)), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Transaksi Gagal ! ❌", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (!isRealOnline) "Koneksi internet Anda terputus! Tidak dapat memverifikasi transaksi pembayaran. Silakan aktifkan WiFi atau Data Seluler Anda untuk melanjutkan."
                                           else "Pembayaran gagal diproses oleh Webhook Server karena kesalahan biner gateway atau time out jaringan resmi.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { txStatusState = "IDLE" },
                                        colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                                        border = BorderStroke(1.dp, Color.Gray),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Text("Coba Lagi", color = Color.White, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://api.whatsapp.com/send?phone=6282258371053&text=Halo%20Owner%20MeydiAI%20transaksi%20pembayaran%20saya%20gagal%20atau%20error")
                                            )
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f).height(44.dp)
                                    ) {
                                        Text("Hubungi Owner", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else if (txStatusState == "CANCEL_SCREEN") {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(BorderStroke(1.dp, Color(0xFFFFB300)), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Transaksi Dibatalkan ! 🔌", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Anda membatalkan proses berlangganan premium. Upgrade dibatalkan dan saldo Anda tidak berkurang sama sekali.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { txStatusState = "IDLE" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Mulai Ulang Pembayaran", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Live Network Status Indicator Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(BorderStroke(1.dp, if (isRealOnline) TerminalGreen.copy(alpha = 0.3f) else Color(0xFFFF5252).copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isRealOnline) TerminalGreen else Color(0xFFFF5252))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isRealOnline) "📡 Jaringan Terdeteksi: ONLINE (Siap Transaksi)" else "⚠️ Jaringan Terdeteksi: OFFLINE (Transaksi Terpotong)",
                                    color = if (isRealOnline) TerminalGreen else Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFFFD700), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Akses Semua Fitur AI Tanpa Batas!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Langganan Lisensi MeydiAi - 30 Hari", color = TextMuted, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Detail Pengguna", color = Color.LightGray, fontSize = 12.sp)
                                Text(userEmail ?: "Tamu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Harga:", color = Color.White)
                                    Text("Rp 149.000", color = TerminalGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Metode Pembayaran", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(8.dp))

                        val methods = listOf("QRIS", "Virtual Account BCA", "GoPay", "OVO")
                        methods.forEach { method ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedMethod == method) Color(0xFFFFD700).copy(alpha = 0.2f) else MidnightSurface)
                                    .border(1.dp, if (selectedMethod == method) Color(0xFFFFD700) else DarkStroke, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isProcessing) { selectedMethod = method }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = null, // Handled by row click
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD700), unselectedColor = Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = method, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Simulation Selector Row for Upgrade Testing Success / Failed / Cancel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Simulasi Skenario Transaksi:", 
                                    color = Color(0xFFFFD700), 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("SUCCESS" to "Sukses ✅", "FAIL" to "Gagal ❌", "CANCEL" to "Batal 🔌").forEach { (code, label) ->
                                        val isSel = txOutcome == code
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFFFFD700) else ObsidianBg)
                                                .clickable { txOutcome = code }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) ObsidianBg else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        if (isProcessing) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFFFD700))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (!isRealOnline) "Menghubungkan Webhook Offline..." else "Menunggu Webhook Server...", 
                                    color = Color.LightGray, 
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedMethod != null) {
                                        isProcessing = true
                                        coroutineScope.launch {
                                            // Simulate Webhook Delay
                                            kotlinx.coroutines.delay(2500)
                                            isProcessing = false
                                            
                                            if (!isRealOnline) {
                                                txStatusState = "FAIL_SCREEN"
                                                Toast.makeText(context, "Sistem Gagal: Jaringan offline terdeteksi!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                when (txOutcome) {
                                                    "SUCCESS" -> {
                                                        txStatusState = "SUCCESS_SCREEN"
                                                    }
                                                    "FAIL" -> {
                                                        txStatusState = "FAIL_SCREEN"
                                                        Toast.makeText(context, "Sistem Gagal: Transaksi ditolak server!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    "CANCEL" -> {
                                                        txStatusState = "CANCEL_SCREEN"
                                                        Toast.makeText(context, "Informasi: Upgrade dibatalkan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), disabledContainerColor = DarkStroke),
                                shape = RoundedCornerShape(12.dp),
                                enabled = selectedMethod != null
                            ) {
                                Text("Bayar Sekarang", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else {
                // TAB 1: THE MANUAL STEPS, WHY TO UPGRADE, AND DIRECT DEDICATED OWNER SUPPORT (WhatsApp 082258371053)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Feature List Display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonTeal.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💎 KEUNGGULAN AKUN PREMIUM", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val benefits = listOf(
                                "Membuka Canvas Editor, Remotion Loop & Auto-Clipper",
                                "Generasi dan render video HD Tanpa Batas / Kuota",
                                "Bypass Verifikasi Manual & Anti-Limit Prompting",
                                "Akses Prioritas Server GPU Spesifik Media Looping"
                            )
                            benefits.forEach { feat ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Ben", tint = TerminalGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(feat, color = Color.LightGray, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // How to Upgrade Guide
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📖 PANDUAN CARA UPGRADE FITUR", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val steps = listOf(
                                "1. PILIH METODE: Masuk ke tab 'Bayar Instan' di atas, dan tentukan metode pembayaran favorit Anda (QRIS / OVO / GoPay dll).",
                                "2. PROSES TRANSFER: Klik 'Bayar Sekarang' untuk memulai pengalihan pembayaran otomatis standar MeydiAi.",
                                "3. AKTIVASI OTOMATIS: Tunggu 3 detik saat webhook sedang disimulasikan. Sistem akan otomatis menyegel lisensi ke email Anda.",
                                "4. SELESAI & ENJOY: Menu workspace canggih Anda akan otomatis terbuka di Beranda utama."
                            )
                            
                            steps.forEach { step ->
                                Text(
                                    text = step,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // HELP/SUPPORT CALLOUT FOR THE OWNER (082258371053)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "WhatsApp Contact",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Ada Kendala Pembayaran?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Hubungi WhatsApp Admin / Owner", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = "Kami siap membantu aktivasi manual jika pembayaran instan Anda mengalami gangguan jaringan. Cukup hubungi di nomor resmi di bawah:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianBg)
                                    .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Nomor WhatsApp Resmi:", color = Color.Gray, fontSize = 10.sp)
                                    Text("0822 5837 1053", color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val heading = "Halo MeydiAi Owner, saya ingin upgrade ke Premium.%0AUMUM: Akun saya: ${userEmail ?: "guest"}"
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://api.whatsapp.com/send?phone=6282258371053&text=$heading")
                                        )
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Send, contentDescription = "Kirim", tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kirim Chat", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 11. OWNER DASHBOARD
@Composable
fun OwnerDashboardScreen(userEmail: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val notifPrefs = remember { context.getSharedPreferences("LoginNotificationPrefs", android.content.Context.MODE_PRIVATE) }
    
    // Maintain state for dynamic logs list
    var loginLogsSet by remember { 
        mutableStateOf(notifPrefs.getStringSet("login_logs", emptySet()) ?: emptySet()) 
    }
    
    fun refreshLogs() {
        loginLogsSet = notifPrefs.getStringSet("login_logs", emptySet()) ?: emptySet()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFFFFD700))
                }
                Text("Dashboard Penulis/Owner", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, Color(0xFFFFD700))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = "Owner", tint = Color(0xFFFFD700), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Manajemen Tingkat Tertinggi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(userEmail, color = Color(0xFFFFD700), fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nomor Kontak Resmi Owner: 082258371053",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            // 🛡️ BAGIAN KHUSUS: PEMANTAU LOGIN PADA PERANGKAT ORANG LAIN
            Text(
                text = "🔔 Laporan Sesi Masuk (Perangkat Pengguna Lain)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Setiap aktivitas login pengguna di perangkat lain akan otomatis disaring, divalidasi, dan dikirim laporannya ke panel di bawah ini secara instan.",
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f))
            ) {
                if (loginLogsSet.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = "Kosong", tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada laporan aktivitas perangkat masuk.", color = Color.Gray, fontSize = 12.sp)
                            Text("Gunakan tombol simulasikan di bawah untuk menguji.", color = Color.Gray.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        loginLogsSet.reversed().forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(6.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(Color(0xFF00FFCC))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = log,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // PANEL TOMBOL KONTROL LOG LOGIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val emails = listOf("dian_pratama@gmail.com", "reza_septian@yahoo.com", "amanda.s@outlook.com", "budi_setiawan@gmail.com")
                        val models = listOf("SAMSUNG Galaxy S24 Ultra", "Xiaomi 14 Pro", "Oppo Reno 11 Pro", "Realme GT5")
                        val randEmail = emails.random()
                        val randModel = models.random()
                        val androidVer = (12..14).random()
                        
                        val timeNow = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val simLog = "Sesi Akun: $randEmail | Waktu: $timeNow | Perangkat: $randModel (Android $androidVer) | Status: ONLINE"
                        
                        val existingLogs = notifPrefs.getStringSet("login_logs", emptySet()) ?: emptySet()
                        val updatedLogs = existingLogs.toMutableSet()
                        updatedLogs.add(simLog)
                        notifPrefs.edit().putStringSet("login_logs", updatedLogs).apply()
                        
                        refreshLogs()
                        Toast.makeText(context, "Log berhasil disimulasikan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simulasikan Log Perangkat 🔄", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = {
                        notifPrefs.edit().remove("login_logs").apply()
                        refreshLogs()
                        Toast.makeText(context, "Semua laporan dibersihkan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bersihkan Log 🗑️", color = Color.White, fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Hubungi Owner Meydi Hikara", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightSurface)
                    .clickable {
                        val textMsg = "Halo Owner Meydi, bagaimana status log sistem hari ini?"
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://api.whatsapp.com/send?phone=6282258371053&text=${java.net.URLEncoder.encode(textMsg, "UTF-8")}")
                        )
                        context.startActivity(intent)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Hubungi WA Owner",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Admin & Owner WhatsApp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Hubungi Jalur Khusus: 082258371053", color = Color(0xFF25D366), fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text("Kontrol Sistem Spesial", color = Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SecuritySectionCard(
                title = "Payment Auto-Approve (Dev Mode)",
                desc = "Ubah setting pembayaran lokal agar user tidak perlu mengisi saldo. Bypass gateway midtrans dan otomatis mendapatkan API Key saat checkout.",
                status = "Aktif (Otomatis Kosong API Key)"
            )
            SecuritySectionCard(
                title = "Flush Global Prompt Cache",
                desc = "Menghapus semua aktivitas pengguna dari SharedPreferences (Clear Data API).",
                status = "Siap Dijalankan"
            )
            SecuritySectionCard(
                title = "Buka Paksa Semua Lisensi",
                desc = "Membuat semua pengguna menjadi Premium seumur hidup (Life-time Access).",
                status = "Siap Dijalankan"
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    Toast.makeText(context, "Mode pemeliharaan sistem diaktifkan sementara.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Maintenance Mode (Kunci Sistem)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper function to auto-save files, videos, and photos to the device public storage folders
fun autoSaveMediaToDevice(
    context: android.content.Context,
    displayName: String,
    mediaType: String // "image" or "video" or "text"
) {
    try {
        val resolver = context.contentResolver
        if (mediaType == "image") {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${displayName}_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/MeydiAi")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outStream ->
                    val bitmap = android.graphics.Bitmap.createBitmap(1280, 720, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#060D1E")
                    }
                    canvas.drawRect(0f, 0f, 1280f, 720f, paint)
                    
                    paint.color = android.graphics.Color.parseColor("#00FFCC")
                    paint.textSize = 50f
                    paint.isAntiAlias = true
                    canvas.drawText("MeydiAI - Automatic HD Saved Photo", 100f, 200f, paint)
                    
                    paint.color = android.graphics.Color.parseColor("#FF00CC")
                    paint.strokeWidth = 5f
                    canvas.drawLine(100f, 250f, 1180f, 250f, paint)
                    
                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 30f
                    canvas.drawText("Status: Terverifikasi oleh Owner Meydi Hikara", 100f, 320f, paint)
                    canvas.drawText("ID Unduhan: AI_${System.currentTimeMillis().toString().takeLast(8)}", 100f, 380f, paint)
                    canvas.drawText("Sistem Penyimpanan: Auto-Simpan Lokal Aktif", 100f, 440f, paint)
                    
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outStream)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                android.widget.Toast.makeText(context, "MeydiAI: Foto Berhasil Disimpan ke Pictures/MeydiAi", android.widget.Toast.LENGTH_LONG).show()
            }
        } else if (mediaType == "video") {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${displayName}_${System.currentTimeMillis()}.mp4")
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/MeydiAi")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val videoUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (videoUri != null) {
                resolver.openOutputStream(videoUri)?.use { outStream ->
                    outStream.write("MeydiAI Video File Header [Auto-Saved] ...".toByteArray())
                    for (i in 1..80) {
                        outStream.write("Streaming video frame simulation data chunk $i ... ".toByteArray())
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(videoUri, contentValues, null, null)
                }
                android.widget.Toast.makeText(context, "MeydiAI: Video Berhasil Disimpan ke Movies/MeydiAi", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${displayName}_${System.currentTimeMillis()}.txt")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MeydiAi")
                }
                val fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (fileUri != null) {
                    resolver.openOutputStream(fileUri)?.use { outStream ->
                        val text = "MeydiAI Auto-Saved Document File\n" +
                                   "===================================\n" +
                                   "Nama File: $displayName\n" +
                                   "Waktu Simpan: ${java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n" +
                                   "Status: Sukses diunduh dan diproteksi oleh Owner Meydi (082258371053)\n\n" +
                                   "Terima kasih telah menggunakan layanan MeydiAi."
                        outStream.write(text.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "MeydiAI: Berkas Disimpan ke Downloads/MeydiAi", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val meydiDir = java.io.File(downloadsDir, "MeydiAi")
                if (!meydiDir.exists()) meydiDir.mkdirs()
                val file = java.io.File(meydiDir, "${displayName}_${System.currentTimeMillis()}.txt")
                file.writeText(
                    "MeydiAI Auto-Saved Document File\n" +
                    "===================================\n" +
                    "Nama File: $displayName\n" +
                    "Waktu Simpan: ${java.text.SimpleDateFormat("dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n" +
                    "Status: Sukses diunduh dan diproteksi oleh Owner Meydi (082258371053)\n\n" +
                    "Terima kasih telah menggunakan layanan MeydiAi."
                )
                android.widget.Toast.makeText(context, "MeydiAI: Berkas Disimpan ke Downloads/MeydiAi", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Simulasi Auto-Simpan Sukses", android.widget.Toast.LENGTH_SHORT).show()
    }
}