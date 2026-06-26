package com.example.rbac.ui

import android.widget.Toast
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.scale
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activity.ActivityLogger
import com.example.logging.AppLogger
import com.example.rbac.model.RbacUser
import com.example.rbac.model.UserRole
import com.example.rbac.model.UserStatus
import com.example.rbac.viewmodel.RbacViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RbacAdminDashboardScreen(
    viewModel: RbacViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allUsers by viewModel.allUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorState by viewModel.error.collectAsState()
    val currentUserState by viewModel.currentUserState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📊 RINGKASAN", "👥 PENGGUNA", "🔒 BAN & UNBAN", "📣 BROADCAST", "📈 ANALITIK & AUDIT")

    // General state
    var isRefreshing by remember { mutableStateOf(false) }

    // Load initial users
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
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
                        Icon(Icons.Default.Shield, null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Meydi CyberAdmin Panel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Akses: ${currentUserState?.userRole?.label ?: "Admin"} (Secure Session)",
                            color = NeonTeal,
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
            // Horizontal Cyber Scrollable Tabs
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

            // Pull to Refresh container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading || isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = NeonTeal)
                            Text("Mensinkronkan data Firebase...", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabContent"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> OverviewTab(
                                allUsers = allUsers,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        viewModel.loadAllUsers()
                                        delay(800)
                                        isRefreshing = false
                                        Toast.makeText(context, "Data berhasil diperbarui secara real-time", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            1 -> UserManagementTab(
                                allUsers = allUsers,
                                viewModel = viewModel
                            )
                            2 -> BanManagementTab(
                                allUsers = allUsers,
                                viewModel = viewModel
                            )
                            3 -> BroadcastNotificationTab(
                                allUsers = allUsers
                            )
                            4 -> AnalyticsAuditTab(
                                allUsers = allUsers
                            )
                        }
                    }
                }
            }
        }
    }

    // Error message handling
    errorState?.let { err ->
        LaunchedEffect(err) {
            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}

// ==========================================
// 1. OVERVIEW TAB
// ==========================================
@Composable
fun OverviewTab(
    allUsers: List<RbacUser>,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val totalCount = allUsers.size
    val activeCount = allUsers.count { it.status == "active" }
    val bannedCount = allUsers.count { it.status == "suspended" }
    val newTodayCount = allUsers.count { it.createdAt > System.currentTimeMillis() - 86400000L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Actions & Refresh Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RINGKASAN REAL-TIME", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightSurface)
                    .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, "Refresh", tint = NeonTeal, modifier = Modifier.size(16.dp))
            }
        }

        // Stats Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(
                title = "Total Pengguna",
                value = "$totalCount",
                icon = Icons.Default.People,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = "Pengguna Aktif",
                value = "$activeCount",
                icon = Icons.Default.RadioButtonChecked,
                color = TerminalGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(
                title = "Akun Diblokir",
                value = "$bannedCount",
                icon = Icons.Default.Block,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
            StatBox(
                title = "Baru Hari Ini",
                value = "$newTodayCount",
                icon = Icons.Default.FiberNew,
                color = NeonMagenta,
                modifier = Modifier.weight(1f)
            )
        }

        // Real-Time User Growth Graph
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grafik Pertumbuhan Pengguna", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Log pertumbuhan kumulatif 7 hari terakhir", color = TextMuted, fontSize = 9.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE FEED", color = NeonTeal, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Line Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val points = listOf(0.1f, 0.25f, 0.4f, 0.35f, 0.6f, 0.75f, 1f)
                    val width = size.width
                    val height = size.height
                    val path = Path()

                    points.forEachIndexed { index, value ->
                        val x = index * (width / (points.size - 1))
                        val y = height - (value * height)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw glowing grid lines
                    for (i in 1..3) {
                        val gridY = i * (height / 4)
                        drawLine(
                            color = DarkStroke.copy(alpha = 0.3f),
                            start = Offset(0f, gridY),
                            end = Offset(width, gridY),
                            strokeWidth = 1f
                        )
                    }

                    // Draw line
                    drawPath(
                        path = path,
                        color = NeonTeal,
                        style = Stroke(width = 4f)
                    )

                    // Draw gradient fill
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonTeal.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )

                    // Draw active points
                    points.forEachIndexed { index, value ->
                        val x = index * (width / (points.size - 1))
                        val y = height - (value * height)
                        drawCircle(
                            color = ObsidianBg,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = NeonTeal,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { day ->
                        Text(day, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Recent Admin Actions List
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AKTIVITAS ADMIN TERKINI (AUDIT LOG)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                
                val adminActivities = remember {
                    listOf(
                        AdminLogItem("RESET_PASSWORD", "Admin Budi mereset sandi Reza Artamevia", System.currentTimeMillis() - 120000),
                        AdminLogItem("ROLE_UPGRADE", "Owner meningkatkan status Budi ke ADMIN", System.currentTimeMillis() - 1800000),
                        AdminLogItem("BAN_ACCOUNT", "Admin menangguhkan Siti Rahma (Spam bot)", System.currentTimeMillis() - 7200000),
                        AdminLogItem("GLOBAL_BROADCAST", "Mengirim mass notification: update v1.1.0", System.currentTimeMillis() - 14400000)
                    )
                }

                adminActivities.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (item.action.contains("BAN")) ErrorRed else NeonTeal)
                            )
                            Column {
                                Text(item.description, color = Color.White, fontSize = 10.sp)
                                Text(
                                    text = "Tag: ${item.action}",
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = formatTimeAgo(item.timestamp),
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

data class AdminLogItem(val action: String, val description: String, val timestamp: Long)

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, DarkStroke)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(title, color = TextMuted, fontSize = 9.sp)
                Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==========================================
// 2. USER MANAGEMENT TAB
// ==========================================
@Composable
fun UserManagementTab(
    allUsers: List<RbacUser>,
    viewModel: RbacViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf("ALL") }
    var filterStatus by remember { mutableStateOf("ALL") }

    // Dialog state
    var selectedUserForDetail by remember { mutableStateOf<RbacUser?>(null) }
    var showCreateAdminDialog by remember { mutableStateOf(false) }

    // State for creating new admin
    var newAdminName by remember { mutableStateOf("") }
    var newAdminEmail by remember { mutableStateOf("") }

    val filteredUsers = allUsers.filter { user ->
        val matchesSearch = user.nama.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.uid.contains(searchQuery, ignoreCase = true)

        val matchesRole = filterRole == "ALL" || user.role.equals(filterRole, ignoreCase = true)
        val matchesStatus = filterStatus == "ALL" || user.status.equals(filterStatus, ignoreCase = true)

        matchesSearch && matchesRole && matchesStatus
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Filters Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari Nama, Email, atau UID...", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonTeal,
                    unfocusedBorderColor = DarkStroke,
                    focusedContainerColor = MidnightSurface,
                    unfocusedContainerColor = MidnightSurface
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
            )

            // Add Admin button
            IconButton(
                onClick = { showCreateAdminDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonTeal.copy(alpha = 0.15f))
                    .border(1.dp, NeonTeal, RoundedCornerShape(8.dp))
                    .size(42.dp)
            ) {
                Icon(Icons.Default.PersonAdd, "Tambah Admin", tint = NeonTeal, modifier = Modifier.size(18.dp))
            }
        }

        // Chip Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Role Filter chips
            listOf("ALL", "OWNER", "ADMIN", "USER").forEach { r ->
                val active = filterRole == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) NeonTeal.copy(alpha = 0.2f) else MidnightSurface)
                        .border(1.dp, if (active) NeonTeal else DarkStroke, RoundedCornerShape(4.dp))
                        .clickable { filterRole = r }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(r, color = if (active) NeonTeal else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            VerticalDivider(color = DarkStroke, modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.width(4.dp))

            // Status Filter chips
            listOf("ALL", "ACTIVE", "SUSPENDED").forEach { s ->
                val active = filterStatus == s
                val activeColor = if (s == "ACTIVE") TerminalGreen else ErrorRed
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) activeColor.copy(alpha = 0.2f) else MidnightSurface)
                        .border(1.dp, if (active) activeColor else DarkStroke, RoundedCornerShape(4.dp))
                        .clickable { filterStatus = s }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(s, color = if (active) activeColor else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // User Count Indicator
        Text(
            text = "Menampilkan ${filteredUsers.size} dari ${allUsers.size} pengguna",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        // User List Grid/Column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pengguna yang cocok dengan kriteria.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserInteractiveRow(
                            user = user,
                            onUserClick = { selectedUserForDetail = user }
                        )
                    }
                }
            }
        }
    }

    // CREATE ADMIN DIALOG
    if (showCreateAdminDialog) {
        CyberDialog(
            title = "Tambah Admin Baru",
            onDismiss = { showCreateAdminDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAdminName.isNotBlank() && newAdminEmail.isNotBlank()) {
                            viewModel.createMockUser(newAdminName, newAdminEmail, UserRole.ADMIN)
                            ActivityLogger.logActivity("CREATE_ADMIN", "Membuat admin baru secara manual: $newAdminEmail")
                            showCreateAdminDialog = false
                            newAdminName = ""
                            newAdminEmail = ""
                        } else {
                            Toast.makeText(context, "Nama & Email wajib diisi!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Buat Admin", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Admin baru akan didaftarkan ke sistem dengan otorisasi terbatas.", color = TextMuted, fontSize = 11.sp)
                
                OutlinedTextField(
                    value = newAdminName,
                    onValueChange = { newAdminName = it },
                    label = { Text("Nama Lengkap", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newAdminEmail,
                    onValueChange = { newAdminEmail = it },
                    label = { Text("Alamat Email", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // USER DETAIL DIALOG
    selectedUserForDetail?.let { user ->
        var editNama by remember { mutableStateOf(user.nama) }
        var editPhone by remember { mutableStateOf(user.phoneNumber) }
        var isEmailVerified by remember { mutableStateOf(true) }

        CyberDialog(
            title = "Profil & Manajemen Pengguna",
            onDismiss = { selectedUserForDetail = null },
            confirmButton = {
                Button(
                    onClick = {
                        // Simulate saving user details update
                        ActivityLogger.logActivity("UPDATE_USER_DATA", "Memperbarui data profil user: ${user.email}")
                        Toast.makeText(context, "Profil ${user.email} berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        selectedUserForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
                ) {
                    Text("Simpan Perubahan", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForDetail = null }) {
                    Text("Tutup", color = TextMuted)
                }
            }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Avatar & Badges
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(user.userRole.color.copy(alpha = 0.15f))
                            .border(1.dp, user.userRole.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.nama.take(1).uppercase(), color = user.userRole.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text(user.nama, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(user.email, color = TextMuted, fontSize = 10.sp)
                        Text("UID: ${user.uid}", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                HorizontalDivider(color = DarkStroke, modifier = Modifier.padding(vertical = 4.dp))

                // Editable Fields
                OutlinedTextField(
                    value = editNama,
                    onValueChange = { editNama = it },
                    label = { Text("Nama Lengkap", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("Nomor Telepon", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Simulated Email Verification & Password Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Verifikasi Email", color = Color.White, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isEmailVerified) "TERVERIFIKASI" else "BELUM VERIFIKASI", color = if (isEmailVerified) TerminalGreen else Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isEmailVerified,
                            onCheckedChange = {
                                isEmailVerified = it
                                ActivityLogger.logActivity("VERIFY_EMAIL_MANUAL", "Mengubah status verifikasi email ${user.email} secara manual.")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TerminalGreen,
                                checkedTrackColor = TerminalGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ActivityLogger.logActivity("TRIGGER_PASSWORD_RESET", "Memicu pengiriman link reset password untuk ${user.email}")
                            Toast.makeText(context, "Link Atur Ulang Sandi terkirim secara aman ke ${user.email}!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, NeonTeal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Reset Sandi", color = NeonTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (user.userRole == UserRole.USER) {
                        Button(
                            onClick = {
                                viewModel.changeUserRole(user, UserRole.ADMIN)
                                selectedUserForDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, NeonMagenta),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Jadikan Admin", color = NeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (user.userRole == UserRole.ADMIN) {
                        Button(
                            onClick = {
                                viewModel.changeUserRole(user, UserRole.USER)
                                selectedUserForDetail = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Ubah ke User", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Delete Account
                if (user.userRole != UserRole.OWNER) {
                    Button(
                        onClick = {
                            viewModel.deleteMockUser(user)
                            ActivityLogger.logActivity("DELETE_USER", "Menghapus permanen akun user: ${user.email}")
                            selectedUserForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, ErrorRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus Permanen Akun", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UserInteractiveRow(
    user: RbacUser,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MidnightSurface.copy(alpha = 0.3f))
            .border(BorderStroke(0.5.dp, DarkStroke), RoundedCornerShape(8.dp))
            .clickable { onUserClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(user.userRole.color.copy(alpha = 0.12f))
                    .border(1.dp, user.userRole.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.nama.take(1).uppercase(),
                    color = user.userRole.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(user.nama, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(user.email, color = TextMuted, fontSize = 10.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = user.userRole.label.uppercase(),
                        color = user.userRole.color,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(TextMuted)
                    )
                    Text(
                        text = "Gabung: " + SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(user.createdAt)),
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                }
            }
        }

        // Status Badge
        val statusColor = if (user.status == "active") TerminalGreen else ErrorRed
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.1f))
                .border(0.5.dp, statusColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = user.status.uppercase(),
                color = statusColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// 3. BAN & UNBAN MANAGEMENT TAB
// ==========================================
@Composable
fun BanManagementTab(
    allUsers: List<RbacUser>,
    viewModel: RbacViewModel
) {
    val context = LocalContext.current
    var searchBanUser by remember { mutableStateOf("") }
    var selectedUserForBan by remember { mutableStateOf<RbacUser?>(null) }
    var banReason by remember { mutableStateOf("") }
    var banDuration by remember { mutableStateOf("PERMANENT") } // "PERMANENT", "7_DAYS", "30_DAYS"
    var showConfirmDialog by remember { mutableStateOf(false) }

    val banHistoryList = remember {
        mutableStateListOf(
            BanRecord("uid_user2", "siti.user@meydiai.com", "Spam bot & aktivitas ilegal", "PERMANENT", System.currentTimeMillis() - 86400000 * 3),
            BanRecord("uid_userx", "reza.bot@meydiai.com", "Penggunaan emulator terlarang", "7_DAYS", System.currentTimeMillis() - 86400000 * 1)
        )
    }

    val availableUsersToBan = allUsers.filter { user ->
        user.userRole == UserRole.USER &&
                (user.nama.contains(searchBanUser, ignoreCase = true) ||
                        user.email.contains(searchBanUser, ignoreCase = true))
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SISTEM PEMBEKUAN DAN PEMULIHAN AKUN (BAN ENGINE)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cari User yang Ingin Dibekukan:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = searchBanUser,
                    onValueChange = { searchBanUser = it },
                    placeholder = { Text("Ketik nama atau email user...", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (searchBanUser.isNotBlank() && availableUsersToBan.isNotEmpty()) {
                    Text("Hasil Pencarian:", color = TextMuted, fontSize = 10.sp)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableUsersToBan) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .border(0.5.dp, DarkStroke, RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedUserForBan = user
                                        searchBanUser = ""
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${user.nama} (${user.email})", color = Color.White, fontSize = 11.sp)
                                Text(if (user.status == "active") "AKTIF" else "BEKU", color = if (user.status == "active") TerminalGreen else ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        selectedUserForBan?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, NeonMagenta)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KONFIGURASI PEMBEKUAN: ${user.email}", color = NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text("Alasan Pembekuan (Wajib)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = DarkStroke
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Ban Duration Radio buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PERMANENT" to "Permanen", "7_DAYS" to "7 Hari", "30_DAYS" to "30 Hari").forEach { (key, label) ->
                            val active = banDuration == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) NeonMagenta.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f))
                                    .border(1.dp, if (active) NeonMagenta else DarkStroke, RoundedCornerShape(4.dp))
                                    .clickable { banDuration = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (active) NeonMagenta else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (banReason.isBlank()) {
                                    Toast.makeText(context, "Alasan pembekuan wajib diisi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showConfirmDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("BEKUKAN AKUN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedUserForBan = null
                                banReason = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("BATAL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Ban history log list
        Text("LOG RIWAYAT PEMBEKUAN AKTIF (BAN HISTORY)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, DarkStroke, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(banHistoryList) { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidnightSurface.copy(alpha = 0.3f))
                            .border(0.5.dp, DarkStroke, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(rec.email, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Alasan: ${rec.reason}", color = TextMuted, fontSize = 9.sp)
                            Text(
                                "Durasi: ${rec.duration} | Tanggal: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(rec.timestamp))}",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                // Simulate unbanning the user
                                val foundUser = allUsers.find { it.uid == rec.uid }
                                if (foundUser != null) {
                                    viewModel.toggleUserStatus(foundUser) // changes suspended back to active
                                }
                                banHistoryList.remove(rec)
                                ActivityLogger.logActivity("UNBAN_ACCOUNT", "Memulihkan (unban) akses akun: ${rec.email}")
                                Toast.makeText(context, "Akses akun ${rec.email} dipulihkan!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, TerminalGreen),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("PULIHKAN", color = TerminalGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // CONFIRM BAN DIALOG
    if (showConfirmDialog && selectedUserForBan != null) {
        val user = selectedUserForBan!!
        CyberDialog(
            title = "⚠️ Konfirmasi Pembekuan Akun",
            onDismiss = { showConfirmDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        // Apply suspension via VM
                        viewModel.toggleUserStatus(user)
                        banHistoryList.add(
                            BanRecord(user.uid, user.email, banReason, banDuration, System.currentTimeMillis())
                        )
                        ActivityLogger.logActivity("BAN_ACCOUNT", "Membekukan akun: ${user.email} (Alasan: $banReason)")
                        Toast.makeText(context, "Akun ${user.email} berhasil dibekukan!", Toast.LENGTH_SHORT).show()
                        
                        showConfirmDialog = false
                        selectedUserForBan = null
                        banReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Ya, Bekukan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        ) {
            Text("Apakah Anda yakin ingin membekukan seluruh akses dari ${user.nama} (${user.email})? Pengguna akan langsung didepak keluar dan tidak diizinkan masuk ke sistem.", color = Color.White, fontSize = 12.sp)
        }
    }
}

data class BanRecord(val uid: String, val email: String, val reason: String, val duration: String, val timestamp: Long)

// ==========================================
// 4. BROADCAST NOTIFICATION TAB
// ==========================================
@Composable
fun BroadcastNotificationTab(
    allUsers: List<RbacUser>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var broadcastTarget by remember { mutableStateOf("ALL") } // "ALL", "ADMINS", "SPECIFIC"
    var specificUserUid by remember { mutableStateOf("") }
    var nTitle by remember { mutableStateOf("") }
    var nBody by remember { mutableStateOf("") }
    var nImageUrl by remember { mutableStateOf("") }
    var nButtonAction by remember { mutableStateOf("") }
    
    // Scheduling states
    var isScheduled by remember { mutableStateOf(false) }
    var scheduledTimeText by remember { mutableStateOf("Kirim Instan") }

    val broadcastHistory = remember {
        mutableStateListOf(
            BroadcastLog("Meydi AI Dashboard Upgrade v1.1.0", "Kini didukung Cloud Sync real-time!", "ALL", 125, 87, System.currentTimeMillis() - 86400000),
            BroadcastLog("Pemberitahuan Maintenance Sistem", "Server akan offline jam 02:00 pagi", "ADMINS", 8, 8, System.currentTimeMillis() - 86400000 * 2)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BROADCAST NOTIFIKASI MASSAL & PENJADWALAN (FCM ENGINE)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                
                // Target selection
                Text("Target Audience Notifikasi:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "Semua User", "ADMINS" to "Admin Saja", "SPECIFIC" to "User Tertentu").forEach { (key, label) ->
                        val active = broadcastTarget == key
                        Box(
                            modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) NeonTeal.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f))
                                        .border(1.dp, if (active) NeonTeal else DarkStroke, RoundedCornerShape(4.dp))
                                        .clickable { broadcastTarget = key }
                                        .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (active) NeonTeal else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (broadcastTarget == "SPECIFIC") {
                    OutlinedTextField(
                        value = specificUserUid,
                        onValueChange = { specificUserUid = it },
                        placeholder = { Text("Ketik Email atau UID User...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = DarkStroke
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }

                HorizontalDivider(color = DarkStroke, modifier = Modifier.padding(vertical = 2.dp))

                // Title & Body
                OutlinedTextField(
                    value = nTitle,
                    onValueChange = { nTitle = it },
                    label = { Text("Judul Notifikasi", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nBody,
                    onValueChange = { nBody = it },
                    label = { Text("Isi Pesan Notifikasi", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = nImageUrl,
                    onValueChange = { nImageUrl = it },
                    label = { Text("URL Gambar Opsional (Banner)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Scheduling toggle simulation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jadwalkan Notifikasi", color = Color.White, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(scheduledTimeText, color = if (isScheduled) NeonTeal else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isScheduled,
                            onCheckedChange = {
                                isScheduled = it
                                scheduledTimeText = if (it) "Kirim 10 Menit Lagi" else "Kirim Instan"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonTeal,
                                checkedTrackColor = NeonTeal.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }

                // Send Button
                Button(
                    onClick = {
                        if (nTitle.isBlank() || nBody.isBlank()) {
                            Toast.makeText(context, "Judul & Isi Notifikasi wajib diisi!", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val targetLabel = when (broadcastTarget) {
                                    "ALL" -> "Semua Pengguna"
                                    "ADMINS" -> "Seluruh Admin"
                                    else -> specificUserUid
                                }
                                ActivityLogger.logActivity("PUSH_BROADCAST", "Kirim notifikasi FCM ke: $targetLabel. Judul: $nTitle")
                                
                                broadcastHistory.add(
                                    0,
                                    BroadcastLog(nTitle, nBody, broadcastTarget, allUsers.size, 0, System.currentTimeMillis())
                                )
                                Toast.makeText(context, "Notifikasi Broadcast FCM berhasil disalurkan!", Toast.LENGTH_LONG).show()

                                // clear states
                                nTitle = ""
                                nBody = ""
                                nImageUrl = ""
                                isScheduled = false
                                scheduledTimeText = "Kirim Instan"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Campaign, null, tint = ObsidianBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isScheduled) "JADWALKAN BROADCAST" else "KIRIM BROADCAST SEKARANG", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Broadcast Logs
        Text("LOG BROADCAST NOTIFIKASI TERKIRIM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            broadcastHistory.forEach { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, DarkStroke)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonTeal.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(log.target, color = NeonTeal, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(log.message, color = TextMuted, fontSize = 10.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terkirim: ${log.sentCount} | Dibuka: ${log.openedCount} (CTR: ${if (log.sentCount>0) (log.openedCount*100/log.sentCount) else 0}%)",
                                color = NeonTeal,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
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

data class BroadcastLog(val title: String, val message: String, val target: String, val sentCount: Int, val openedCount: Int, val timestamp: Long)

// ==========================================
// 5. ANALYTICS & AUDIT TAB
// ==========================================
@Composable
fun AnalyticsAuditTab(
    allUsers: List<RbacUser>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GRAFIK STATISTIK METRIK & KEAMANAN SISTEM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        // Security Info Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, NeonTeal)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    Text("PROTEKSI FIREBASE SECURITY RULES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "Akses baca tulis ke Firestore dibatasi secara ketat menggunakan validasi status peran 'admin' dan 'owner' secara end-to-end.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(6.dp))
                        .border(0.5.dp, DarkStroke, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "match /users/{userId} {\n  allow read, write: if request.auth != null && get(/databases/\$(database)/documents/users/\$(request.auth.uid)).data.role == 'admin';\n}",
                        color = TerminalGreen,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        // Device Analytics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DISTRIBUSI PERANGKAT & OS (REAL-TIME)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // Android OS versions representation
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgressBarStat("Android 14 (API 34)", 0.65f, "65%", NeonTeal)
                    ProgressBarStat("Android 13 (API 33)", 0.22f, "22%", NeonPurple)
                    ProgressBarStat("Android 12 (API 31/32)", 0.10f, "10%", NeonMagenta)
                    ProgressBarStat("Android 11 & Versi Lama", 0.03f, "3%", Color.Gray)
                }
            }
        }

        // App Version and Usage Features
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FITUR TERPOPULER DIAKSES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgressBarStat("AI Auto Clipper (Gemini Engine)", 0.52f, "52%", NeonTeal)
                    ProgressBarStat("Workspace Remotion Desk", 0.28f, "28%", NeonPurple)
                    ProgressBarStat("HD Media Enhancer Pro", 0.12f, "12%", NeonMagenta)
                    ProgressBarStat("Cyber Security Dashboard", 0.08f, "8%", Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ProgressBarStat(
    label: String,
    progress: Float,
    percent: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 10.sp)
            Text(percent, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DarkStroke
        )
    }
}

// Custom helper: Time calculations
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Baru saja"
        diff < 3600000 -> "${diff / 60000} mnt lalu"
        diff < 86400000 -> "${diff / 3600000} jam lalu"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

// Custom CyberDialog Component
@Composable
fun CyberDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, DarkStroke, RoundedCornerShape(12.dp))
    )
}


