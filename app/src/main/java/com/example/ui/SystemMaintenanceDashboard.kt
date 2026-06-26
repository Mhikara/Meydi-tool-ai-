package com.example.ui

import android.content.Context
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.AppDatabase
import com.example.logging.AppLogger
import com.example.logging.SystemLog
import com.example.activity.ActivityHistory
import com.example.activity.ActivityLogger
import com.example.cache.CacheManager
import com.example.recovery.BackupRecord
import com.example.recovery.RecoveryManager
import com.example.sync.SyncManager
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMaintenanceDashboard(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📋 LOGS SISTEM", "⚡ CACHE & SYNC", "📂 BACKUP & INTEGRITY", "👥 RIWAYAT AKTIVITAS")

    // State for Logs Tab
    var logSearchQuery by remember { mutableStateOf("") }
    var logSeverityFilter by remember { mutableStateOf("ALL") }
    var logsList by remember { mutableStateOf<List<SystemLog>>(emptyList()) }

    // State for Activities Tab
    var activitySearchQuery by remember { mutableStateOf("") }
    var activitiesList by remember { mutableStateOf<List<ActivityHistory>>(emptyList()) }

    // State for Cache & Sync Tab
    var cacheCount by remember { mutableStateOf(0) }
    var syncQueueCount by remember { mutableStateOf(0) }
    val syncStatus by SyncManager.syncStatus.collectAsState()
    val uploadProgress by SyncManager.uploadProgress.collectAsState()

    // State for Backup & Integrity Tab
    var databaseHealth by remember { mutableStateOf("Menunggu Pemeriksaan...") }
    var backupHistory by remember { mutableStateOf<List<BackupRecord>>(emptyList()) }

    // Helper functions to fetch data
    fun reloadLogs() {
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                if (logSeverityFilter == "ALL") {
                    if (logSearchQuery.isEmpty()) {
                        db.systemLogDao().getAllLogs().first()
                    } else {
                        db.systemLogDao().searchLogs(logSearchQuery).first()
                    }
                } else {
                    db.systemLogDao().getLogsBySeverity(logSeverityFilter).first()
                }
            }
            logsList = list
        }
    }

    fun reloadActivities() {
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                if (activitySearchQuery.isEmpty()) {
                    db.activityHistoryDao().getAllActivities().first()
                } else {
                    db.activityHistoryDao().searchActivities(activitySearchQuery).first()
                }
            }
            activitiesList = list
        }
    }

    fun reloadStats() {
        scope.launch {
            val cCount = withContext(Dispatchers.IO) { db.cachedDataDao().getCacheCount() }
            val sCount = withContext(Dispatchers.IO) { db.syncQueueDao().getAllQueueItems().size }
            cacheCount = cCount
            syncQueueCount = sCount
        }
    }

    fun reloadBackups() {
        scope.launch {
            val backups = withContext(Dispatchers.IO) { db.backupRecordDao().getAllBackupRecords() }
            backupHistory = backups
        }
    }

    // Load initial data
    LaunchedEffect(selectedTab, logSearchQuery, logSeverityFilter, activitySearchQuery) {
        when (selectedTab) {
            0 -> reloadLogs()
            1 -> reloadStats()
            2 -> {
                reloadBackups()
                // Auto check integrity when entering tab
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            val cursor = db.openHelper.writableDatabase.query("PRAGMA integrity_check")
                            var resStr = "Corruption Detected!"
                            if (cursor.moveToFirst()) {
                                resStr = cursor.getString(0)
                            }
                            cursor.close()
                            resStr
                        } catch (e: Exception) {
                            "Error: ${e.message}"
                        }
                    }
                    databaseHealth = if (result.equals("ok", ignoreCase = true)) "SEHAT (PRAGMA OK)" else "RUSAK ($result)"
                }
            }
            3 -> reloadActivities()
        }
    }

    Scaffold(
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Custom cyber top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.AdminPanelSettings, null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("CyberAdmin Maintenance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Security & Access Management (Prod)", color = TextMuted, fontSize = 10.sp)
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

            // Cyber Status Summary Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, DarkStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Meydi AI Engine v1.1.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Enkripsi AES-256 & Cache Aktif", color = NeonTeal, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonTeal.copy(alpha = 0.15f))
                            .border(1.dp, NeonTeal, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PRODUCTION", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Scrollable Cyber Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MidnightSurface,
                contentColor = NeonMagenta,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonMagenta,
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
                                color = if (selectedTab == index) NeonMagenta else TextMuted
                            )
                        }
                    )
                }
            }

            // Tab contents
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> {
                        // LOGS VIEWER TAB
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Filters and controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search
                                OutlinedTextField(
                                    value = logSearchQuery,
                                    onValueChange = { logSearchQuery = it },
                                    placeholder = { Text("Cari log message/module...", color = TextMuted, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonMagenta,
                                        unfocusedBorderColor = DarkStroke,
                                        focusedContainerColor = MidnightSurface,
                                        unfocusedContainerColor = MidnightSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                // Export Buttons
                                IconButton(onClick = {
                                    val file = AppLogger.exportLogsToTxt(context)
                                    Toast.makeText(context, "Log diekspor ke: ${file.name}", Toast.LENGTH_LONG).show()
                                    ActivityLogger.logActivity("EXPORT_LOGS_TXT", "Mengekspor log sistem ke format TXT.")
                                }) {
                                    Icon(Icons.Default.Article, "Export TXT", tint = NeonTeal)
                                }

                                IconButton(onClick = {
                                    val file = AppLogger.exportLogsToJson(context)
                                    Toast.makeText(context, "Log diekspor ke: ${file.name}", Toast.LENGTH_LONG).show()
                                    ActivityLogger.logActivity("EXPORT_LOGS_JSON", "Mengekspor log sistem ke format JSON.")
                                }) {
                                    Icon(Icons.Default.DataObject, "Export JSON", tint = NeonTeal)
                                }

                                IconButton(onClick = {
                                    val file = AppLogger.exportLogsCompressed(context)
                                    Toast.makeText(context, "GZIP terbuat: ${file.name}", Toast.LENGTH_LONG).show()
                                    ActivityLogger.logActivity("EXPORT_LOGS_GZIP", "Kompresi GZIP untuk log selesai.")
                                }) {
                                    Icon(Icons.Default.Inventory2, "Export GZip", tint = NeonMagenta)
                                }
                            }

                            // Severity filters Row
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("ALL", "INFO", "DEBUG", "WARN", "ERROR", "CRITICAL").forEach { sev ->
                                    val active = logSeverityFilter == sev
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (active) NeonMagenta.copy(alpha = 0.2f) else MidnightSurface)
                                            .border(1.dp, if (active) NeonMagenta else DarkStroke, RoundedCornerShape(4.dp))
                                            .clickable { logSeverityFilter = sev }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(sev, color = if (active) NeonMagenta else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Logs list
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (logsList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Tidak ada log sistem yang cocok.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(logsList) { log ->
                                            val severityColor = when (log.severity) {
                                                "CRITICAL" -> ErrorRed
                                                "ERROR" -> ErrorRed
                                                "WARN" -> Color.Yellow
                                                "DEBUG" -> NeonTeal
                                                else -> TerminalGreen
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MidnightSurface.copy(alpha = 0.2f))
                                                    .border(0.5.dp, DarkStroke, RoundedCornerShape(4.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${log.module}::${log.functionName}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(log.severity, color = severityColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(log.message, color = Color.LightGray, fontSize = 10.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = SimpleDateFormat("dd MMM, HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp)),
                                                    color = TextMuted,
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

                    1 -> {
                        // CACHE & SYNC TAB
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            // Cache card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Storage, "Storage", tint = NeonTeal)
                                        Text("Manajemen Cache Lokal (Room)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Seluruh cache lokal terkompresi dengan GZip dan terenkripsi menggunakan AES-256 kunci aman Android Keystore.", color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Entri Cache Aktif:", color = Color.Gray, fontSize = 12.sp)
                                        Text("$cacheCount entri / 100 maks", color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    CacheManager.put("test_key_${System.currentTimeMillis()}", "Ini adalah data cache rahasia terenkripsi!")
                                                    reloadStats()
                                                    Toast.makeText(context, "Test Cache Berhasil Dimasukkan!", Toast.LENGTH_SHORT).show()
                                                    ActivityLogger.logActivity("PUT_TEST_CACHE", "Memasukkan entri cache tes baru.")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Tambah Cache Tes", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    CacheManager.clear()
                                                    reloadStats()
                                                    Toast.makeText(context, "Cache Berhasil Dikosongkan!", Toast.LENGTH_SHORT).show()
                                                    ActivityLogger.logActivity("CLEAR_CACHE", "Mengosongkan seluruh data cache Room.")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                                            border = BorderStroke(1.dp, ErrorRed),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Clear Cache", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Sync card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.CloudSync, "Sync", tint = NeonMagenta)
                                        Text("Otomasi Antrean Sinkronisasi (Firestore)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Saat offline, perubahan data diantrekan secara lokal. Sinkronisasi otomatis memicu retry cerdas dengan Exponential Backoff ketika internet kembali.", color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Status Sinkronisasi:", color = Color.Gray, fontSize = 12.sp)
                                        Text(syncStatus.toString().substringAfterLast("$"), color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Antrean Offline Terkunci:", color = Color.Gray, fontSize = 12.sp)
                                        Text("$syncQueueCount tugas tertunda", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (syncStatus is SyncManager.SyncStatus.Syncing) {
                                        LinearProgressIndicator(
                                            progress = uploadProgress,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = NeonMagenta,
                                            trackColor = DarkStroke
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            SyncManager.autoSync()
                                            scope.launch {
                                                reloadStats()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, null, tint = ObsidianBg)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Paksa Sinkronisasi Sekarang", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // BACKUP & INTEGRITY TAB
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Integrity Check Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                                border = BorderStroke(1.dp, DarkStroke)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Kesehatan File SQLite Database:", color = Color.Gray, fontSize = 12.sp)
                                        Text(databaseHealth, color = if (databaseHealth.contains("SEHAT")) TerminalGreen else ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val success = RecoveryManager.createBackup("Manual Backup Admin Panel")
                                                if (success) {
                                                    reloadBackups()
                                                    Toast.makeText(context, "Backup Database Berhasil Dibuat!", Toast.LENGTH_SHORT).show()
                                                    ActivityLogger.logActivity("CREATE_MANUAL_BACKUP", "Pencadangan database SQLite manual dibuat.")
                                                } else {
                                                    Toast.makeText(context, "Gagal membuat backup!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, tint = ObsidianBg)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ambil Cadangan Baru (Savepoint)", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val success = RecoveryManager.rollbackToLatestBackup()
                                                if (success) {
                                                    Toast.makeText(context, "Restorasi Database Sukses!", Toast.LENGTH_SHORT).show()
                                                    ActivityLogger.logActivity("ROLLBACK_BACKUP", "Mengembalikan data sistem ke savepoint valid terakhir.")
                                                } else {
                                                    Toast.makeText(context, "Gagal rollback! Tidak ada cadangan valid.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                                        border = BorderStroke(1.dp, NeonMagenta),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.SettingsBackupRestore, null, tint = NeonMagenta)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pulihkan ke Cadangan Valid Terakhir", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("Log Riwayat Cadangan (Backup History)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            // Backups history list
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (backupHistory.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Belum ada log cadangan database.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(backupHistory) { bk ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MidnightSurface.copy(alpha = 0.2f))
                                                    .border(0.5.dp, DarkStroke, RoundedCornerShape(4.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(bk.description, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(if (bk.isValid) "INTEG_PASS" else "CORRUPT", color = if (bk.isValid) TerminalGreen else ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Checksum MD5: ${bk.checksum}", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Waktu: ${SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(bk.timestamp))}",
                                                    color = TextMuted,
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // RIWAYAT AKTIVITAS TAB
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search Box
                            OutlinedTextField(
                                value = activitySearchQuery,
                                onValueChange = { activitySearchQuery = it },
                                placeholder = { Text("Cari riwayat aktivitas user...", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonMagenta,
                                    unfocusedBorderColor = DarkStroke,
                                    focusedContainerColor = MidnightSurface,
                                    unfocusedContainerColor = MidnightSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            // Activities list
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (activitiesList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Belum ada riwayat aktivitas yang terekam.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(activitiesList) { act ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MidnightSurface.copy(alpha = 0.2f))
                                                    .border(0.5.dp, DarkStroke, RoundedCornerShape(4.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(act.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(if (act.isSuccess) "SUKSES" else "GAGAL", color = if (act.isSuccess) TerminalGreen else ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(act.description, color = Color.LightGray, fontSize = 10.sp)
                                                if (act.additionalDetails.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(act.additionalDetails, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("User: ${act.userId}", color = TextMuted, fontSize = 8.sp)
                                                    Text(
                                                        text = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(act.timestamp)),
                                                        color = TextMuted,
                                                        fontSize = 8.sp
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
            }
        }
    }
}
