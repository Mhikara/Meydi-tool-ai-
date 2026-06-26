package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- THEME ---
private val PrimaryTheme = Color(0xFF00FFCC)
private val PanelBackground = Color(0xFF131524)
private val WindowBg = Color(0xFF1E2032)

data class FeatureItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
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
    onNavigateToEncryption: () -> Unit = {},
    onNavigateToOwner: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRemotion: () -> Unit,
    onNavigateToClipper: () -> Unit,
    onNavigateToDownloader: () -> Unit,
    onNavigateToEnhancer: () -> Unit,
    onNavigateToPromptGenerator: () -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToTemplateStudio: () -> Unit,
    onNavigateToNetworkMonitor: () -> Unit,
    onNavigateToUpdateManager: () -> Unit,
    onNavigateToApiManagement: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sysPrefs = remember { context.getSharedPreferences("OSCoreEnginePrefs", android.content.Context.MODE_PRIVATE) }

    // API & Settings States
    var isPremiumLicense by remember { mutableStateOf(sysPrefs.getBoolean("premium_license", false)) }
    var geminiKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getGeminiKey()) }
    var azbryKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getAzbryKey()) }
    var firebaseApiKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseApiKey()) }
    var firebaseAppIdInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseAppId()) }
    var firebaseProjectIdInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseProjectId()) }
    var supabaseUrlInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getSupabaseUrl()) }
    var supabaseAnonKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getSupabaseAnonKey()) }
    var showSettingsModal by remember { mutableStateOf(false) }
    
    val features = listOf(
        FeatureItem("downloader", "Downloader", "TikTok, YT, IG", Icons.Default.Download, Color(0xFF25F4EE), onNavigateToDownloader),
        FeatureItem("assistant", "Meydi AI", "Assistant", Icons.Default.ChatBubbleOutline, Color(0xFFE040FB), onNavigateToAssistant),
        FeatureItem("enhancer", "HD Enhancer", "4K Quality", Icons.Default.Image, Color(0xFF00E676), onNavigateToEnhancer),
        FeatureItem("clipper", "AI Clipper", "Smart Cut", Icons.Default.Movie, Color(0xFFFF5722), onNavigateToClipper),
        FeatureItem("template", "AI Studio", "Templates", Icons.Default.AutoFixHigh, Color(0xFFFF9800), onNavigateToTemplateStudio),
        FeatureItem("remotion", "Remotion", "Video Edit", Icons.Default.Layers, Color(0xFF00B0FF), onNavigateToRemotion),
        FeatureItem("prompt", "Prompt Gen", "AI Prompt", Icons.Default.FlashOn, Color(0xFFFFEB3B), onNavigateToPromptGenerator),
        FeatureItem("network", "Monitor", "Net Status", Icons.Default.SignalCellularAlt, Color(0xFF90A4AE), onNavigateToNetworkMonitor),
        FeatureItem("api_manager", "Central API", "Config & Log", Icons.Default.LockOpen, Color(0xFFE91E63), onNavigateToApiManagement)
    )

    val dockFeatures = listOf(
        FeatureItem("profile", "Profile", "", Icons.Default.Person, Color.White, onNavigateToProfile),
        FeatureItem("assistant", "AI", "", Icons.Default.AutoAwesome, Color(0xFFE040FB), onNavigateToAssistant),
        FeatureItem("settings", "Setup", "", Icons.Default.Settings, Color.White, { showSettingsModal = true }),
        FeatureItem("logout", "Exit", "", Icons.Default.Logout, Color(0xFFFF3366), onLogout)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A)) // Deepest black
    ) {
        // Decorative background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(PrimaryTheme.copy(0.15f), Color.Transparent)))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // OS Status Bar Simulation
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "09:41",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.SignalCellular4Bar, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.BatteryFull, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // User Info Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Meydi AI",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (isOwner) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFD700).copy(0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("OWNER", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Text(
                                if (isOwner) "System Core Root" else if (isAdmin) "Admin Level 1" else "Premium Member",
                                color = PrimaryTheme.copy(0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.08f))
                                .clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // System Alert if any
                    if (systemAlertMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3366).copy(0.15f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF3366))
                                Spacer(Modifier.width(12.dp))
                                Text(systemAlertMessage, color = Color(0xFFFF3366), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Main App Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(280.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(features.size) { index ->
                            val feature = features[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { feature.onClick() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(feature.color, feature.color.copy(0.7f))
                                            )
                                        )
                                        .shadow(10.dp, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(feature.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    feature.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Admin Section
                    if (isAdmin || isOwner) {
                        Spacer(Modifier.height(24.dp))
                        Text("System Administration", color = Color.White.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminQuickAction(
                                modifier = Modifier.weight(1f),
                                name = "Owner Panel",
                                icon = Icons.Default.VerifiedUser,
                                isVisible = isOwner,
                                onClick = onNavigateToOwner
                            )
                            AdminQuickAction(
                                modifier = Modifier.weight(1f),
                                name = "Admin Console",
                                icon = Icons.Default.AdminPanelSettings,
                                isVisible = isAdmin,
                                onClick = onNavigateToAdmin
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminQuickAction(
                                modifier = Modifier.weight(1f),
                                name = "Security",
                                icon = Icons.Default.Security,
                                isVisible = true,
                                onClick = onNavigateToSecurity
                            )
                            AdminQuickAction(
                                modifier = Modifier.weight(1f),
                                name = "Network",
                                icon = Icons.Default.Dns,
                                isVisible = true,
                                onClick = onNavigateToNetworkMonitor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(120.dp)) // Space for dock
                }

                // iOS Style Dock
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockFeatures.forEach { feature ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(0.05f))
                                    .clickable { feature.onClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Settings Dialog (unchanged logic, improved styling)
    if (showSettingsModal) {
        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            containerColor = Color(0xFF131524),
            title = { Text("Settings Console", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("System Configuration & API Registry", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    
                    SettingInput("Gemini AI API", geminiKeyInput) { geminiKeyInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Azbry Key", azbryKeyInput) { azbryKeyInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Firebase API Key", firebaseApiKeyInput) { firebaseApiKeyInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Firebase App ID", firebaseAppIdInput) { firebaseAppIdInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Firebase Project ID", firebaseProjectIdInput) { firebaseProjectIdInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Supabase URL", supabaseUrlInput) { supabaseUrlInput = it }
                    Spacer(Modifier.height(12.dp))
                    SettingInput("Supabase Anon Key", supabaseAnonKeyInput) { supabaseAnonKeyInput = it }
                    
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Meydi AI Pro License", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = isPremiumLicense,
                            onCheckedChange = {
                                isPremiumLicense = it
                                sysPrefs.edit().putBoolean("premium_license", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryTheme)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    com.example.api.ApiKeyRegistry.saveGeminiKey(geminiKeyInput)
                    com.example.api.ApiKeyRegistry.saveAzbryKey(azbryKeyInput)
                    com.example.api.ApiKeyRegistry.saveFirebaseApiKey(firebaseApiKeyInput)
                    com.example.api.ApiKeyRegistry.saveFirebaseAppId(firebaseAppIdInput)
                    com.example.api.ApiKeyRegistry.saveFirebaseProjectId(firebaseProjectIdInput)
                    com.example.api.ApiKeyRegistry.saveSupabaseUrl(supabaseUrlInput)
                    com.example.api.ApiKeyRegistry.saveSupabaseAnonKey(supabaseAnonKeyInput)
                    
                    // Inisialisasi ulang Firebase secara dinamis jika diubah
                    try {
                        val manager = com.example.config.FirebaseConfigManager(context)
                        manager.initializeFirebase()
                    } catch (e: Exception) {}
                    
                    showSettingsModal = false
                }) {
                    Text("Apply Changes", color = PrimaryTheme, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AdminQuickAction(
    modifier: Modifier = Modifier,
    name: String,
    icon: ImageVector,
    isVisible: Boolean,
    onClick: () -> Unit
) {
    if (!isVisible) return
    Card(
        modifier = modifier.height(60.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = PrimaryTheme, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SettingInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label.uppercase(), color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTheme,
                unfocusedBorderColor = Color.White.copy(0.1f)
            ),
            singleLine = true
        )
    }
}

