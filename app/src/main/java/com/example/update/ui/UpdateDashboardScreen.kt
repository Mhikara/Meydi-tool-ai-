package com.example.update.ui

import android.widget.Toast
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.update.model.AppConfigEntity
import com.example.update.model.ChangelogEntity
import com.example.update.viewmodel.ApkUpdateState
import com.example.update.viewmodel.OtaSyncState
import com.example.update.viewmodel.UpdateViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDashboardScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // View Model state flows
    val apkState by viewModel.apkUpdateState.collectAsStateWithLifecycle()
    val otaState by viewModel.otaSyncState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadStatusText by viewModel.downloadStatusText.collectAsStateWithLifecycle()
    val changelogsHistory by viewModel.changelogsHistory.collectAsStateWithLifecycle()
    val configsCache by viewModel.configsCache.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isOfflineSimulated by viewModel.isOfflineSimulated.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📊 CEK VERSI", "📋 HISTORI CHANGELOG", "📣 OTA CONFIG", "⚡ LAB SIMULASI")

    // Preferences for Custom Sandbox simulation
    val simPrefs = remember { context.getSharedPreferences("meydiai_update_manager", android.content.Context.MODE_PRIVATE) }
    var simRemoteVersion by remember { mutableStateOf(simPrefs.getString("sim_remote_version", "1.2.0") ?: "1.2.0") }
    var simForceUpdate by remember { mutableStateOf(simPrefs.getBoolean("sim_force_update", false)) }
    var simCorruption by remember { mutableStateOf(simPrefs.getBoolean("sim_corruption_active", false)) }

    // Alert Handling on State updates
    LaunchedEffect(otaState) {
        when (val state = otaState) {
            is OtaSyncState.RollbackTriggered -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            is OtaSyncState.Success -> {
                if (state.updatedCount > 0) {
                    Toast.makeText(context, "🔥 Berhasil menerapkan ${state.updatedCount} konfigurasi incremental!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonTeal.copy(alpha = 0.15f))
                            .border(1.dp, NeonTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudDownload, null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Meydi AI Update Center", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isOfflineSimulated) "Koneksi: SIMULASI OFFLINE" else "Koneksi: SECURE HTTPS / REAL-TIME",
                            color = if (isOfflineSimulated) ErrorRed else TerminalGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MidnightSurface)
                        .border(1.dp, DarkStroke, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Horizontal Navigation tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MidnightSurface,
                contentColor = NeonTeal,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonTeal,
                        height = 2.dp
                    )
                },
                divider = { HorizontalDivider(color = DarkStroke) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) NeonTeal else TextMuted
                            )
                        }
                    )
                }
            }

            // Connection indicator alert when offline
            AnimatedVisibility(
                visible = isOfflineSimulated,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Simulasi Offline Aktif: Data diambil dari cache lokal Room SQLite.",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pull to refresh support container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Interactive tabs content
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabTransition"
                ) { target ->
                    when (target) {
                        0 -> CheckVersionTab(
                            apkState = apkState,
                            downloadProgress = downloadProgress,
                            downloadStatusText = downloadStatusText,
                            onCheck = { viewModel.triggerManualCheck() },
                            onStartDownload = { url -> viewModel.startSimulatedDownload(url) },
                            onCancelDownload = { viewModel.clearDownloadState() }
                        )
                        1 -> ChangelogHistoryTab(
                            history = changelogsHistory,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) }
                        )
                        2 -> OtaConfigTab(
                            configs = configsCache,
                            otaState = otaState,
                            onForceSync = { viewModel.syncOtaConfigs() }
                        )
                        3 -> SandboxSimulationTab(
                            simRemoteVersion = simRemoteVersion,
                            simForceUpdate = simForceUpdate,
                            simCorruption = simCorruption,
                            isOfflineSimulated = isOfflineSimulated,
                            onSaveSim = { ver, force, corruption ->
                                simRemoteVersion = ver
                                simForceUpdate = force
                                simCorruption = corruption
                                simPrefs.edit()
                                    .putString("sim_remote_version", ver)
                                    .putBoolean("sim_force_update", force)
                                    .putBoolean("sim_corruption_active", corruption)
                                    .apply()
                                Toast.makeText(context, "Parameter simulasi lab disimpan!", Toast.LENGTH_SHORT).show()
                                viewModel.syncOtaConfigs()
                            },
                            onToggleOffline = { active ->
                                viewModel.setOfflineSimulation(active)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. CHECK VERSION TAB
// ==========================================
@Composable
fun CheckVersionTab(
    apkState: ApkUpdateState,
    downloadProgress: Float?,
    downloadStatusText: String,
    onCheck: () -> Unit,
    onStartDownload: (String) -> Unit,
    onCancelDownload: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Version card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("INFORMASI PAKET LOKAL", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Meydi AI Engine v1.1.0", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("STABLE RELEASES", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = DarkStroke, modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Target SDK", color = TextMuted, fontSize = 10.sp)
                        Text("Android 14 (API 34)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sistem Tipe", color = TextMuted, fontSize = 10.sp)
                        Text("Universal Gradle APK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Action Trigger Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    null,
                    tint = NeonTeal,
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeonTeal.copy(0.1f), CircleShape)
                        .padding(10.dp)
                )

                Text(
                    "Pemeriksa Versi Server",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Sistem akan memvalidasi tanda tangan digital, header checksum SHA-256, dan versi bundel biner server secara asinkron.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Text("Periksa Update Sekarang", color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results Presentation based on State
        AnimatedContent(targetState = apkState, label = "UpdateStateSpec") { state ->
            when (state) {
                is ApkUpdateState.Checking -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonTeal.copy(0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = NeonTeal, modifier = Modifier.size(28.dp))
                            Text("Mendownload manifes metadata versi terbaru...", color = NeonTeal, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                is ApkUpdateState.UpdateAvailable -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonMagenta)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.NewReleases, null, tint = NeonMagenta)
                                    Text("UPDATE BARU TERSEDIA", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (state.info.isForceUpdate) ErrorRed.copy(0.15f) else Color.Gray.copy(0.15f))
                                        .border(1.dp, if (state.info.isForceUpdate) ErrorRed else Color.Gray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (state.info.isForceUpdate) "CRITICAL / WAJIB" else "OPSIONAL",
                                        color = if (state.info.isForceUpdate) ErrorRed else Color.LightGray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Versi Terpasang: ${state.info.currentVersion}", color = TextMuted, fontSize = 11.sp)
                                Text("Versi Terbaru: ${state.info.latestVersion}", color = NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                                    .border(1.dp, DarkStroke, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Changelog Server:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(state.info.changelog, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }

                            if (downloadProgress == null) {
                                Button(
                                    onClick = { onStartDownload(state.info.downloadUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Pasang Paket Update Sekarang", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                is ApkUpdateState.UpToDate -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, TerminalGreen.copy(0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = TerminalGreen, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Aplikasi Sudah Update", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Anda menggunakan biner terbaru. Sesi aman dan lancar.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                is ApkUpdateState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, ErrorRed.copy(0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Gagal Menghubungi Server", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(state.message, color = ErrorRed, fontSize = 11.sp)
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Animated Downloading Progress Overlay
        downloadProgress?.let { progress ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, NeonTeal)
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
                        Text("PROGRESS DOWNLOAD APK", color = NeonTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = onCancelDownload, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = NeonTeal,
                        trackColor = DarkStroke
                    )

                    Text(
                        text = downloadStatusText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. CHANGELOG HISTORY TAB
// ==========================================
@Composable
fun ChangelogHistoryTab(
    history: List<ChangelogEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    var expandedVersion by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Cari rilis, e.g. '1.1.0'...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonTeal,
                unfocusedBorderColor = DarkStroke,
                focusedContainerColor = MidnightSurface,
                unfocusedContainerColor = MidnightSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada riwayat changelog yang cocok.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                items(history) { record ->
                    val isExpanded = expandedVersion == record.version
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedVersion = if (isExpanded) null else record.version },
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, if (isExpanded) NeonTeal else DarkStroke)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (isExpanded) NeonTeal else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("v${record.version}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(record.releaseDate)),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (isExpanded) {
                                HorizontalDivider(color = DarkStroke, modifier = Modifier.padding(vertical = 4.dp))
                                
                                Text("🚀 FITUR BARU:", color = TerminalGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(record.featuresList, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))

                                Text("🐛 BUG FIXES:", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(record.bugFixesList, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))

                                Text("📈 PERINGKAT PERFORMA:", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(record.perfGainsList, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))

                                Text("🔒 ENKRIPSI & KEAMANAN:", color = NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(record.securityList, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. OTA CONFIGURATION TAB
// ==========================================
@Composable
fun OtaConfigTab(
    configs: List<AppConfigEntity>,
    otaState: OtaSyncState,
    onForceSync: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("OTA REAL-TIME CONFIGS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Diterapkan otomatis tanpa kompilasi ulang biner APK", color = TextMuted, fontSize = 9.sp)
            }

            IconButton(
                onClick = onForceSync,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MidnightSurface)
                    .border(1.dp, DarkStroke, RoundedCornerShape(6.dp))
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, "Sync", tint = NeonTeal, modifier = Modifier.size(16.dp))
            }
        }

        // OTA Status Presentation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val icon = if (otaState is OtaSyncState.Syncing) Icons.Default.Sync else Icons.Default.CloudQueue
                val text = when (otaState) {
                    is OtaSyncState.Syncing -> "Menghubungkan & mendownload data OTA..."
                    is OtaSyncState.Success -> "Sistem sinkron! Semua konfigurasi server aktif."
                    is OtaSyncState.OfflineFallback -> "Modus offline aktif. Membaca sesi SQLite."
                    is OtaSyncState.RollbackTriggered -> "Rollback aktif: File server rusak, memulihkan cadangan."
                    else -> "Sistem siap sinkronisasi otomatis."
                }
                val color = when (otaState) {
                    is OtaSyncState.Syncing -> NeonTeal
                    is OtaSyncState.Success -> TerminalGreen
                    is OtaSyncState.OfflineFallback -> Color.Yellow
                    is OtaSyncState.RollbackTriggered -> ErrorRed
                    else -> Color.Gray
                }

                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Dynamic configs list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (configs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada konfigurasi OTA terdeteksi. Silakan Refresh.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            } else {
                items(configs) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MidnightSurface.copy(0.4f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.configKey, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(item.configValue, color = Color.Gray, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonTeal.copy(0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Hash: " + item.versionHash.take(6).uppercase(),
                                color = NeonTeal,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SANDBOX SIMULATION TAB
// ==========================================
@Composable
fun SandboxSimulationTab(
    simRemoteVersion: String,
    simForceUpdate: Boolean,
    simCorruption: Boolean,
    isOfflineSimulated: Boolean,
    onSaveSim: (String, Boolean, Boolean) -> Unit,
    onToggleOffline: (Boolean) -> Unit
) {
    var verInput by remember { mutableStateOf(simRemoteVersion) }
    var forceInput by remember { mutableStateOf(simForceUpdate) }
    var corruptInput by remember { mutableStateOf(simCorruption) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("LABORATORIUM & SIMULASI QA (DEBUG PANEL)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simulasi Parameter Update", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = verInput,
                    onValueChange = { verInput = it },
                    label = { Text("Versi Remote Server (APK)", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Paksa Update (Force)", color = Color.White, fontSize = 11.sp)
                        Text("Menghilangkan tombol Nanti", color = TextMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = forceInput,
                        onCheckedChange = { forceInput = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonTeal, checkedTrackColor = NeonTeal.copy(0.3f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulasikan Kerusakan Data (Rollback)", color = Color.White, fontSize = 11.sp)
                        Text("Mengirim JSON korup untuk memaksa Rollback ke SQLite", color = TextMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = corruptInput,
                        onCheckedChange = { corruptInput = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonMagenta, checkedTrackColor = NeonMagenta.copy(0.3f))
                    )
                }

                Button(
                    onClick = { onSaveSim(verInput, forceInput, corruptInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, NeonTeal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Simpan dan Sinkronisasi Simulasi", color = NeonTeal, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simulasi Skenario Jaringan", color = NeonTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulasi Modus Offline", color = Color.White, fontSize = 11.sp)
                        Text("Mematikan internet asinkron untuk memaksa cache Room", color = TextMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = isOfflineSimulated,
                        onCheckedChange = { onToggleOffline(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ErrorRed, checkedTrackColor = ErrorRed.copy(0.3f))
                    )
                }
            }
        }
    }
}
