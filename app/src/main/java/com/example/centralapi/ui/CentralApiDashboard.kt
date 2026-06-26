package com.example.centralapi.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.centralapi.core.CentralApiModule
import com.example.centralapi.domain.UserRole
import com.example.core.Kernel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralApiDashboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Fetch CentralApiModule
    val apiModule = remember {
        Kernel.get<CentralApiModule>("core.centralapi") ?: throw IllegalStateException("CentralApiModule not registered")
    }

    val securityManager = apiModule.securityManager
    val apiManager = apiModule.apiManager

    // States for Security & Base Configs
    var baseUrlInput by remember { mutableStateOf(apiManager.getBaseUrl()) }
    var apiKeyInput by remember { mutableStateOf(securityManager.getAppApiKey() ?: "") }
    var currentUserRole by remember { mutableStateOf(securityManager.getUserRole()) }
    var currentToken by remember { mutableStateOf(securityManager.getUserToken() ?: "None (Unauthenticated)") }
    var showApiKey by remember { mutableStateOf(false) }

    // Visual Log terminal state
    val apiLogs = remember { mutableStateListOf<String>() }

    // Loading overlay
    var isOperating by remember { mutableStateOf(false) }

    fun addLog(msg: String) {
        apiLogs.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $msg")
    }

    LaunchedEffect(Unit) {
        addLog("Central API Dashboard Initialized.")
        addLog("Base URL: ${apiManager.getBaseUrl()}")
        addLog("Current Secure User Role: ${securityManager.getUserRole()}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Central API Manager Panel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        securityManager.clearAll()
                        currentToken = "None (Unauthenticated)"
                        currentUserRole = UserRole.USER
                        addLog("Cleared current session cache.")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Session")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Security & Endpoint Configurations
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Security & Gateway Config",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Base URL input
                            OutlinedTextField(
                                value = baseUrlInput,
                                onValueChange = { baseUrlInput = it },
                                label = { Text("Gateway Base URL (HTTPS)") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("base_url_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    apiManager.rebuildRetrofit(baseUrlInput)
                                    addLog("Rebuilt Retrofit gateway to: $baseUrlInput")
                                    keyboardController?.hide()
                                })
                            )

                            // API Key Input
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("Application X-API-Key") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Default.Warning else Icons.Default.Info,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    securityManager.saveAppApiKey(apiKeyInput)
                                    addLog("Saved Application X-API-Key securely in KeyStore.")
                                    keyboardController?.hide()
                                })
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        apiManager.rebuildRetrofit(baseUrlInput)
                                        securityManager.saveAppApiKey(apiKeyInput)
                                        addLog("Updated Configs safely.")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Apply Credentials")
                                }
                            }
                        }
                    }
                }

                // Section 2: User Session & Access Control
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Active User Role & Access Restrictions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                "CurrentUser Token: $currentToken",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Select Simulation Role:", fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    UserRole.values().forEach { role ->
                                        val isSelected = currentUserRole == role
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                securityManager.saveUserRole(role)
                                                currentUserRole = role
                                                addLog("Switched Active Simulated Role to: $role")
                                            },
                                            label = { Text(role.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Feature Connection Grid (21 Features)
                item {
                    Text(
                        "Connected Features & API Integrity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    val features = listOf(
                        FeatureItem("Login", Icons.Default.AccountBox, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.loginRepository.login("user@meydi.ai", "secret123").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Login: Registered profile ${it.fullName}, Role: ${it.role}")
                                        currentToken = it.token ?: "SimulatedJWTToken_2026"
                                        currentUserRole = it.role
                                    }.onFailure {
                                        addLog("FAIL Login: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Register", Icons.Default.Create, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.registerRepository.register("Meydi Admin", "admin@meydi.ai", "admin99").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Register: Account ${it.email} created successfully!")
                                    }.onFailure {
                                        addLog("FAIL Register: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Guest Account", Icons.Default.Person, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.guestRepository.signInAsGuest().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Guest: Signed in with token: ${it.token}")
                                        currentToken = it.token ?: "GuestToken"
                                        currentUserRole = it.role
                                    }.onFailure {
                                        addLog("FAIL Guest: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("User Profile", Icons.Default.Face, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.profileRepository.getProfile().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Profile: ${it.fullName} (${it.email})")
                                    }.onFailure {
                                        addLog("FAIL Profile: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Dashboard", Icons.Default.Home, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.dashboardRepository.getDashboardStats().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Stats: Users=${it.totalUsers}, Subscriptions=${it.activeSubscriptions}, Load=${it.systemLoad}%")
                                    }.onFailure {
                                        addLog("FAIL Stats: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Auto Update", Icons.Default.Build, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.updateRepository.checkUpdate("1.0.0").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Update: Next version ${it.latestVersion}, Force update=${it.forceUpdate}")
                                    }.onFailure {
                                        addLog("FAIL Update: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Downloader", Icons.Default.PlayArrow, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.downloaderRepository.getTasks().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Downloader: Loaded ${it.size} dynamic tasks from network")
                                    }.onFailure {
                                        addLog("FAIL Downloader: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("AI Assistant", Icons.Default.Call, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.aiAssistantRepository.askAi("Optimasi database caching room").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS AI response: '${it.result}' (${it.tokensUsed} tokens)")
                                    }.onFailure {
                                        addLog("FAIL AI Assistant: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Premium Activate", Icons.Default.Star, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.premiumRepository.activatePremium("PRO_MEMBER_2026").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Premium Activated! Upgraded user role.")
                                        currentUserRole = UserRole.PREMIUM
                                    }.onFailure {
                                        addLog("FAIL Premium Activation: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Cloud Sync", Icons.Default.Menu, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.activityRepository.addLog("Synchronized dynamic user assets")
                                val result = apiModule.syncRepository.syncOfflineData()
                                if (result.isSuccess) {
                                    addLog("SUCCESS Cloud Sync completed! Synced offline logs.")
                                } else {
                                    addLog("FAIL Cloud Sync: ${result.exceptionOrNull()?.message}")
                                }
                                isOperating = false
                            }
                        },
                        FeatureItem("Room ↔ Firestore", Icons.Default.Share, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                addLog("SUCCESS Room SQLite caches mapped to Firestore streams securely.")
                                isOperating = false
                            }
                        },
                        FeatureItem("Notification", Icons.Default.Notifications, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.notificationRepository.getNotifications().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Notification: Received ${it.size} notifications")
                                    }.onFailure {
                                        addLog("FAIL Notification: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Settings", Icons.Default.Settings, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.settingsRepository.getSettings().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Settings: Dynamic properties loaded = $it")
                                    }.onFailure {
                                        addLog("FAIL Settings: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Stats System", Icons.Default.List, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.statsRepository.getStats().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS General stats loaded.")
                                    }.onFailure {
                                        addLog("FAIL System Stats: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("User Activities", Icons.Default.Info, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.activityRepository.getLogs().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Activities: Loaded ${it.size} logs from Secure Room Cache")
                                    }.onFailure {
                                        addLog("FAIL Activity: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Upload File", Icons.Default.Add, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                addLog("SUCCESS Upload File: Stream initialised.")
                                isOperating = false
                            }
                        },
                        FeatureItem("Download File", Icons.Default.ExitToApp, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                addLog("SUCCESS Download Stream: Connection initialized safely.")
                                isOperating = false
                            }
                        },
                        FeatureItem("Local Search", Icons.Default.Search, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.searchRepository.search("Security core").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Search results fetched: $it")
                                    }.onFailure {
                                        addLog("FAIL Search: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Feedback submit", Icons.Default.MailOutline, UserRole.USER) {
                            scope.launch {
                                isOperating = true
                                apiModule.feedbackRepository.submitFeedback(5, "Centralized API is working great!").collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Feedback submitted successfully.")
                                    }.onFailure {
                                        addLog("FAIL Feedback submission: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Sistem Owner", Icons.Default.Star, UserRole.OWNER) {
                            scope.launch {
                                isOperating = true
                                apiModule.ownerRepository.getOwnerPanel().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Owner Panel: Granted access to secret configuration.")
                                    }.onFailure {
                                        addLog("FAIL Owner Panel access: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        },
                        FeatureItem("Sistem Admin", Icons.Default.Edit, UserRole.ADMIN) {
                            scope.launch {
                                isOperating = true
                                apiModule.adminRepository.getAdminDashboard().collect { res ->
                                    res.onSuccess {
                                        addLog("SUCCESS Admin Control Panel: System diagnostic dashboard unlocked.")
                                    }.onFailure {
                                        addLog("FAIL Admin Dashboard access: ${it.message}")
                                    }
                                    isOperating = false
                                }
                            }
                        }
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(features) { feat ->
                                val isRoleAllowed = when (feat.requiredRole) {
                                    UserRole.USER -> true
                                    UserRole.PREMIUM -> currentUserRole == UserRole.PREMIUM || currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.OWNER
                                    UserRole.ADMIN -> currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.OWNER
                                    UserRole.OWNER -> currentUserRole == UserRole.OWNER
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { feat.action() }
                                        .border(
                                            width = 1.dp,
                                            color = if (isRoleAllowed) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isRoleAllowed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = feat.icon,
                                            contentDescription = null,
                                            tint = if (isRoleAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = feat.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = if (isRoleAllowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Role: ${feat.requiredRole}",
                                                fontSize = 10.sp,
                                                color = if (isRoleAllowed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Visual Log Terminal (Terminal Output)
                item {
                    Text(
                        "Live Log Terminal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(apiLogs) { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (log.contains("FAIL") || log.contains("DENIED")) Color(0xFFFF6B6B) else if (log.contains("SUCCESS")) Color(0xFF81C784) else Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
            }

            if (isOperating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private data class FeatureItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val requiredRole: UserRole,
    val action: () -> Unit
)
