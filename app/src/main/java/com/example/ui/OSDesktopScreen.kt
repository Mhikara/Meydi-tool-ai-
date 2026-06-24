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
    val sysPrefs = remember { context.getSharedPreferences("OSCoreEnginePrefs", android.content.Context.MODE_PRIVATE) }

    // API & Settings States
    var isPremiumLicense by remember { mutableStateOf(sysPrefs.getBoolean("premium_license", false)) }
    var geminiKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getGeminiKey()) }
    var azbryKeyInput by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getAzbryKey()) }
    var showSettingsModal by remember { mutableStateOf(false) }
    
    val features = listOf(
        FeatureItem("downloader", "Media Downloader", "Unduh video/gambar dari TikTok, Twitter, YouTube, & IG via URL", Icons.Default.Download, Color(0xFF25F4EE), onNavigateToDownloader),
        FeatureItem("assistant", "AI Assistant", "Asisten pintar Meydi AI", Icons.Default.ChatBubbleOutline, Color(0xFFE040FB), onNavigateToAssistant),
        FeatureItem("enhancer", "HD Enhancer", "Tingkatkan resolusi gambar menjadi 4K", Icons.Default.Image, Color(0xFF00E676), onNavigateToEnhancer),
        FeatureItem("clipper", "AI Clipper", "Potong video cerdas otomatis", Icons.Default.Movie, Color(0xFFFF5722), onNavigateToClipper),
        FeatureItem("template", "AI Studio", "Koleksi prompt pintar", Icons.Default.AutoFixHigh, Color(0xFFFF9800), onNavigateToTemplateStudio),
        FeatureItem("remotion", "Remotion Edit", "Editor efek remotion", Icons.Default.Layers, Color(0xFF00B0FF), onNavigateToRemotion),
        FeatureItem("prompt", "Prompt Gen", "Generator prompt cerdas", Icons.Default.FlashOn, Color(0xFFFFEB3B), onNavigateToPromptGenerator),
        FeatureItem("monitor", "Network", "Pantau koneksi & API", Icons.Default.NetworkCheck, Color(0xFFE91E63), onNavigateToNetworkMonitor)
    )

    Scaffold(
        containerColor = Color(0xFF0B0F19), // Dark sophisticated blueish-black
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1E293B), Color.Transparent))
                    )
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Meydi AI User",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val roleText = if (isOwner) "System Owner" else if (isAdmin) "Administrator" else "Premium Tier"
                            Text(
                                roleText,
                                color = PrimaryTheme,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSettingsModal = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.05f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Subscription Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPremiumLicense) "Meydi AI Pro Active" else "Upgrade to Pro",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPremiumLicense) "Nikmati akses tak terbatas ke semua fitur mutakhir AI." else "Buka fitur 4K HD upscaling, Clipper tanpa batas, dsb.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Text(
                "Fitur Ekosistem",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Simple Features List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { feature.onClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(feature.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = feature.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = feature.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Admin & Logout Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (isOwner) {
                    Button(
                        onClick = onNavigateToOwner,
                        modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                    ) {
                        Text("Owner Dashboard", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
                
                if (isAdmin) {
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTheme.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, PrimaryTheme.copy(alpha = 0.4f))
                    ) {
                        Text("Admin Console", color = PrimaryTheme, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onNavigateToSecurity,
                        modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTheme.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, PrimaryTheme.copy(alpha = 0.4f))
                    ) {
                        Text("Security Logs", color = PrimaryTheme, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366).copy(0.1f)),
                    border = BorderStroke(1.dp, Color(0xFFFF3366).copy(0.4f))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF3366))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar (Logout)", color = Color(0xFFFF3366), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal Settings Dialog
    if (showSettingsModal) {
        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = {
                Text(text = "Pengaturan Sistem & API", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Text("Ubah kunci API integrasi eksternal untuk mengaktifkan fungsi spesifik:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("GEMINI AI API KEY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTheme,
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("AZBRY DOWNLOADER KEY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = azbryKeyInput,
                        onValueChange = { azbryKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTheme,
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lisensi VIP Pro", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Upgrade gratis tier dev", fontSize = 11.sp)
                        }
                        Switch(
                            checked = isPremiumLicense,
                            onCheckedChange = {
                                isPremiumLicense = it
                                sysPrefs.edit().putBoolean("premium_license", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryTheme, checkedTrackColor = PrimaryTheme.copy(alpha=0.3f))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        com.example.api.ApiKeyRegistry.saveGeminiKey(geminiKeyInput)
                        com.example.api.ApiKeyRegistry.saveAzbryKey(azbryKeyInput)
                        showSettingsModal = false
                    }
                ) {
                    Text("Simpan", color = PrimaryTheme, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}
