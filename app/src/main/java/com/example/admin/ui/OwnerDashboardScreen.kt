package com.example.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.admin.viewmodel.OwnerViewModel
import com.example.auth.model.User
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👑 Owner Control Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("System Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Users",
                            value = stats.totalUsers.toString(),
                            icon = Icons.Default.Person,
                            color = Color(0xFF4CAF50)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Premium",
                            value = stats.premiumUsers.toString(),
                            icon = Icons.Default.Star,
                            color = Color(0xFFFFC107)
                        )
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Revenue",
                            value = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(stats.totalRevenue),
                            icon = Icons.Default.ShoppingCart,
                            color = Color(0xFF2196F3)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Server Status",
                            value = stats.serverStatus,
                            icon = Icons.Default.Settings,
                            color = if (stats.serverStatus == "Online") Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("User Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                items(users) { user ->
                    var showUserActions by remember { mutableStateOf(false) }
                    
                    UserAdminCard(
                        user = user,
                        onAction = { showUserActions = true }
                    )

                    if (showUserActions) {
                        AlertDialog(
                            onDismissRequest = { showUserActions = false },
                            title = { Text("Manage ${user.fullName}") },
                            text = {
                                Column {
                                    Text("UID: ${user.uid}")
                                    Spacer(Modifier.height(16.dp))
                                    Text("Change Role:", fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("FREE", "PREMIUM", "ADMIN").forEach { role ->
                                            if (role != user.role.uppercase()) {
                                                Button(
                                                    onClick = {
                                                        viewModel.changeUserRole(user.uid, role.lowercase(), "SYSTEM_OWNER")
                                                        showUserActions = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text(role, fontSize = 10.sp) }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.toggleUserBlock(user.uid, user.isBlocked, "SYSTEM_OWNER")
                                        showUserActions = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (user.isBlocked) Color.Green else Color.Red
                                    )
                                ) {
                                    Text(if (user.isBlocked) "Unblock" else "Block")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUserActions = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = color)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun UserAdminCard(
    user: User,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = when(user.role.uppercase()) {
                            "OWNER" -> Color(0xFFFFD700)
                            "ADMIN" -> Color(0xFFE91E63)
                            "PREMIUM" -> Color(0xFF9C27B0)
                            else -> Color.Gray
                        }
                    ) {
                        Text(user.role, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    if (user.isBlocked) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = Color.Red) {
                            Text("BLOCKED", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
            IconButton(onClick = onAction) {
                Icon(Icons.Default.Edit, contentDescription = "Edit User")
            }
        }
    }
}
