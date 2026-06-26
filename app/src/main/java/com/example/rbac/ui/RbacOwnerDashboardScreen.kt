package com.example.rbac.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.rbac.model.RbacUser
import com.example.rbac.model.UserRole
import com.example.rbac.model.UserStatus
import com.example.rbac.viewmodel.RbacViewModel
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RbacOwnerDashboardScreen(
    viewModel: RbacViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allUsers by viewModel.allUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorState by viewModel.error.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("👥 KELOLA USER", "⚙️ CONFIG SISTEM", "📂 BACKUP/RESTORE", "📋 AUDIT LOG")

    var searchQuery by remember { mutableStateOf("") }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    // Form state for creating user
    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf(UserRole.USER) }

    // Load users on launch
    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .background(NeonMagenta.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Stars, null, tint = NeonMagenta)
                        }
                        Text("Dashboard Owner Mutlak", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

                Text(
                    text = "Akses tanpa batas. Anda berwenang mengatur seluruh role pengguna, menangguhkan akun, melakukan konfigurasi sistem global, serta mengekspor data audit log.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Horizontal Tab Menu
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = MidnightSurface,
            contentColor = NeonMagenta,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = NeonMagenta,
                    height = 2.dp
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
                            color = if (activeTab == index) NeonMagenta else TextMuted
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                0 -> {
                    // Manage Users Tab
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari Email / Nama...", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonMagenta,
                                    unfocusedBorderColor = DarkStroke,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Add user button
                            Button(
                                onClick = { showCreateUserDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = ObsidianBg)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("User", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Users List View
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (isLoading) {
                                RbacSkeletonLoader(modifier = Modifier.padding(8.dp))
                            } else {
                                val filtered = allUsers.filter {
                                    it.email.contains(searchQuery, ignoreCase = true) || 
                                    it.nama.contains(searchQuery, ignoreCase = true)
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filtered) { user ->
                                        OwnerUserItem(
                                            user = user,
                                            onToggleStatus = { viewModel.toggleUserStatus(user) },
                                            onChangeRole = { newRole -> viewModel.changeUserRole(user, newRole) },
                                            onDeleteUser = { viewModel.deleteMockUser(user) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // System Config Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, DarkStroke)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Sistem Keamanan Global", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                
                                var maintenanceMode by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Mode Pemeliharaan (Maintenance Mode)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Blokir seluruh akses pengguna standard selama maintenance.", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = maintenanceMode,
                                        onCheckedChange = { 
                                            maintenanceMode = it
                                            Toast.makeText(context, "Mode pemeliharaan berhasil di${if(it) "aktifkan" else "nonaktifkan"}!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonMagenta, checkedTrackColor = NeonMagenta.copy(alpha = 0.3f))
                                    )
                                }

                                HorizontalDivider(color = DarkStroke)

                                var maxSessions by remember { mutableFloatStateOf(3f) }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Batas Maksimal Sesi Bersamaan: ${maxSessions.toInt()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Batasi berapa perangkat yang bisa masuk secara serentak per akun.", color = TextMuted, fontSize = 10.sp)
                                    Slider(
                                        value = maxSessions,
                                        onValueChange = { maxSessions = it },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        colors = SliderDefaults.colors(thumbColor = NeonMagenta, activeTrackColor = NeonMagenta)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Backup / Restore Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, DarkStroke)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Pencadangan Sistem & Database", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Ekspor seluruh konfigurasi database lokal, metadata, log keamanan, dan session cache ke format JSON aman.", color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                                
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Backup Berhasil: meydi_backup_2026.json", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, tint = ObsidianBg)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Backup Database Sekarang", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Memulihkan database dari backup terakhir...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                                    border = BorderStroke(1.dp, NeonMagenta),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.SettingsBackupRestore, null, tint = NeonMagenta)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Restore Database", color = NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Audit Log Tab
                    val logPref = remember { context.getSharedPreferences("SecurityAuditLogs", android.content.Context.MODE_PRIVATE) }
                    val auditLogs = remember { logPref.getStringSet("audit_logs", emptySet())?.toList()?.sorted()?.reversed() ?: emptyList() }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Aktivitas Keamanan Terakhir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                logPref.edit().clear().apply()
                                Toast.makeText(context, "Log berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Clear Logs", color = ErrorRed, fontSize = 11.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, DarkStroke), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (auditLogs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Belum ada catatan log aktivitas keamanan.", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(auditLogs) { log ->
                                        val parts = log.split(" | ")
                                        val date = parts.getOrNull(0) ?: ""
                                        val action = parts.getOrNull(1) ?: ""
                                        val severity = parts.getOrNull(2) ?: ""
                                        val msg = parts.getOrNull(3) ?: ""

                                        val color = when (severity) {
                                            "CRITICAL" -> ErrorRed
                                            "WARNING" -> Color.Yellow
                                            else -> TerminalGreen
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MidnightSurface.copy(alpha = 0.3f))
                                                .border(BorderStroke(0.5.dp, DarkStroke), RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = action, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(text = severity, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = msg, color = Color.LightGray, fontSize = 10.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = date, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
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

    // Create User Dialog
    if (showCreateUserDialog) {
        var expandedRole by remember { mutableStateOf(false) }

        CyberDialog(
            title = "Tambah User Simulasi",
            onDismiss = { showCreateUserDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank() && newUserEmail.isNotBlank()) {
                            viewModel.createMockUser(newUserName, newUserEmail, newUserRole)
                            showCreateUserDialog = false
                            newUserName = ""
                            newUserEmail = ""
                        } else {
                            Toast.makeText(context, "Isi nama dan email!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("Buat", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = newUserName,
                    onValueChange = { newUserName = it },
                    label = { Text("Nama Lengkap", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonMagenta, unfocusedBorderColor = DarkStroke),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newUserEmail,
                    onValueChange = { newUserEmail = it },
                    label = { Text("Email", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonMagenta, unfocusedBorderColor = DarkStroke),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newUserRole.label,
                        onValueChange = {},
                        label = { Text("Role Akses", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonMagenta, unfocusedBorderColor = DarkStroke),
                        modifier = Modifier.fillMaxWidth().clickable { expandedRole = true },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = NeonMagenta) }
                    )

                    DropdownMenu(
                        expanded = expandedRole,
                        onDismissRequest = { expandedRole = false },
                        modifier = Modifier.background(MidnightSurface)
                    ) {
                        UserRole.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.label, color = r.color) },
                                onClick = {
                                    newUserRole = r
                                    expandedRole = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Error Snackbar
    errorState?.let { err ->
        LaunchedEffect(err) {
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}

@Composable
fun OwnerUserItem(
    user: RbacUser,
    onToggleStatus: () -> Unit,
    onChangeRole: (UserRole) -> Unit,
    onDeleteUser: () -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.5.dp, DarkStroke.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
            .background(MidnightSurface.copy(alpha = 0.3f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(user.userRole.color.copy(alpha = 0.15f))
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
                
                // Role Badge clickable
                Box {
                    Text(
                        text = "${user.userRole.label.uppercase()} ▼",
                        color = user.userRole.color,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { if (user.uid != "uid_owner") showRoleMenu = true }
                            .padding(vertical = 2.dp)
                    )

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false },
                        modifier = Modifier.background(MidnightSurface)
                    ) {
                        UserRole.entries.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption.label, color = roleOption.color) },
                                onClick = {
                                    onChangeRole(roleOption)
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSuspended = user.userStatus == UserStatus.SUSPENDED
            
            // Suspend Button (cannot suspend owner themselves)
            if (user.userRole != UserRole.OWNER) {
                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSuspended) Icons.Default.LockOpen else Icons.Default.Block,
                        contentDescription = "Toggle Status",
                        tint = if (isSuspended) Color(0xFF00FF88) else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteUser,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete User",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonMagenta.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("OWNER", color = NeonMagenta, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
