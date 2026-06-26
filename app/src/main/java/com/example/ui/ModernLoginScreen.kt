package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.utils.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.content.Context
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private val BrandNeonTeal = Color(0xFF00FFCC)
private val BrandNeonPurple = Color(0xFF7F00FF)
private val DarkBg = Color(0xFF0A0A10)
private val InputBg = Color(0xFF151522)
private val TextMuted = Color(0xFFAAAAAA)

enum class AuthScreenMode {
    LOGIN, REGISTER, FORGOT_PASSWORD, PHONE_AUTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLoginScreen(onLoginSuccess: (String?) -> Unit) {
    var currentMode by remember { mutableStateOf(AuthScreenMode.LOGIN) }
    
    Crossfade(targetState = currentMode, animationSpec = tween(500), label = "Auth Transition") { mode ->
        when (mode) {
            AuthScreenMode.LOGIN -> LoginContent(
                onLoginSuccess = onLoginSuccess,
                onNavigateRegister = { currentMode = AuthScreenMode.REGISTER },
                onNavigateForgot = { currentMode = AuthScreenMode.FORGOT_PASSWORD },
                onNavigatePhone = { currentMode = AuthScreenMode.PHONE_AUTH }
            )
            AuthScreenMode.REGISTER -> RegisterContent(
                onRegisterSuccess = onLoginSuccess,
                onNavigateLogin = { currentMode = AuthScreenMode.LOGIN }
            )
            AuthScreenMode.FORGOT_PASSWORD -> ForgotPasswordContent(
                onNavigateLogin = { currentMode = AuthScreenMode.LOGIN }
            )
            AuthScreenMode.PHONE_AUTH -> PhoneAuthContent(
                onAuthSuccess = onLoginSuccess,
                onNavigateLogin = { currentMode = AuthScreenMode.LOGIN }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    onLoginSuccess: (String?) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onNavigatePhone: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: com.example.auth.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Configuration preferences
    val authPrefs = remember { context.getSharedPreferences("AuthSystemPrefs", Context.MODE_PRIVATE) }
    var activeAuthSystem by remember { 
        mutableStateOf(authPrefs.getString("active_auth_system", "auto") ?: "auto") 
    }

    // States for Backend Dynamic Setup Dialog
    var showBackendSetup by remember { mutableStateOf(false) }
    var firebaseApiKey by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseApiKey()) }
    var firebaseAppId by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseAppId()) }
    var firebaseProjectId by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getFirebaseProjectId()) }
    
    var supabaseUrl by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getSupabaseUrl()) }
    var supabaseAnonKey by remember { mutableStateOf(com.example.api.ApiKeyRegistry.getSupabaseAnonKey()) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is com.example.auth.model.AuthState.Loading -> {
                isLoading = true
            }
            is com.example.auth.model.AuthState.Authenticated -> {
                isLoading = false
                val isSupabase = com.example.api.ApiKeyRegistry.hasCustomSupabase() && 
                        (activeAuthSystem == "supabase" || activeAuthSystem == "auto")
                Toast.makeText(context, "Login Berhasil via ${if (isSupabase) "Supabase" else "Firebase"}", Toast.LENGTH_SHORT).show()
                onLoginSuccess(state.user.email)
            }
            is com.example.auth.model.AuthState.Error -> {
                isLoading = false
                Toast.makeText(context, "Login Gagal: ${state.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearError()
            }
            is com.example.auth.model.AuthState.EmailNotVerified -> {
                isLoading = false
                Toast.makeText(context, "Email belum diverifikasi. Silakan periksa inbox Anda.", Toast.LENGTH_LONG).show()
            }
            else -> {
                isLoading = false
            }
        }
    }

    fun attemptLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        
        // Auto-configure before login attempt
        val isSupabaseConfigured = com.example.api.ApiKeyRegistry.hasCustomSupabase()
        if (activeAuthSystem == "supabase" && !isSupabaseConfigured) {
            isLoading = false
            Toast.makeText(context, "Konfigurasi Supabase kosong! Harap atur di menu Settings (Pojok Kanan Atas)", Toast.LENGTH_LONG).show()
            showBackendSetup = true
            return
        }

        authViewModel.login(email, password)
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        
        // Tombol Pengaturan Backend di pojok kanan atas
        IconButton(
            onClick = { showBackendSetup = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Backend Setup",
                tint = BrandNeonTeal
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(brush = Brush.linearGradient(colors = listOf(BrandNeonPurple, BrandNeonTeal))),
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp)) }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Backend active badge indicator
            val isSupabaseActive = com.example.api.ApiKeyRegistry.hasCustomSupabase() && 
                    (activeAuthSystem == "supabase" || activeAuthSystem == "auto")
            Surface(
                color = if (isSupabaseActive) BrandNeonTeal.copy(alpha = 0.15f) else BrandNeonPurple.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (isSupabaseActive) BrandNeonTeal else BrandNeonPurple),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (isSupabaseActive) "ACTIVE: SUPABASE AUTH" else "ACTIVE: FIREBASE AUTH",
                    color = if (isSupabaseActive) BrandNeonTeal else BrandNeonPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(text = "WELCOME BACK", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White, letterSpacing = 2.sp)
            Text(text = "Masuk ke Meydi OS Workspace", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email", color = TextMuted) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandNeonTeal) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedBorderColor = BrandNeonTeal, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password", color = TextMuted) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandNeonPurple) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedBorderColor = BrandNeonPurple, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onNavigateForgot) {
                    Text("Lupa Password?", color = BrandNeonTeal, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { attemptLogin() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNeonTeal),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp))
                else Text("LOGIN", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Social & Other Logins
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { 
                        // Google Login Placeholder
                        Toast.makeText(context, "Google Sign-In membutuhkan google-services.json & SHA-1", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Google", color = Color.White) }
                
                Button(
                    onClick = onNavigatePhone,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Phone", color = Color.White) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text("Belum punya akun? ", color = TextMuted)
                Text("Daftar", color = BrandNeonTeal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateRegister() })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { onLoginSuccess("guest") }) {
                Text("Lanjutkan sebagai Guest", color = Color.Gray)
            }
        }

        if (showBackendSetup) {
            AlertDialog(
                onDismissRequest = { showBackendSetup = false },
                containerColor = Color(0xFF131524),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = BrandNeonTeal, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Backend System Configuration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        item {
                            Text(
                                "Pilih backend aktif dan masukkan kredensial untuk mengaktifkan sistem registrasi & login secara real-time.",
                                color = Color.White.copy(0.7f),
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            // Active system selector
                            Text("Sistem Auth Aktif:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { activeAuthSystem = "supabase" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeAuthSystem == "supabase") BrandNeonTeal else Color(0xFF1E1E2C)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Supabase", color = if (activeAuthSystem == "supabase") DarkBg else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { activeAuthSystem = "firebase" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeAuthSystem == "firebase") BrandNeonPurple else Color(0xFF1E1E2C)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Firebase", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        if (activeAuthSystem == "supabase" || activeAuthSystem == "auto") {
                            item {
                                Text("SUPABASE CREDENTIALS", color = BrandNeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(10.dp))
                                
                                OutlinedTextField(
                                    value = supabaseUrl,
                                    onValueChange = { supabaseUrl = it },
                                    label = { Text("Supabase URL", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                                        focusedBorderColor = BrandNeonTeal, unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = supabaseAnonKey,
                                    onValueChange = { supabaseAnonKey = it },
                                    label = { Text("Supabase Anon Key", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                                        focusedBorderColor = BrandNeonTeal, unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                        
                        if (activeAuthSystem == "firebase" || activeAuthSystem == "auto") {
                            item {
                                Text("FIREBASE CREDENTIALS", color = BrandNeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(10.dp))
                                
                                OutlinedTextField(
                                    value = firebaseApiKey,
                                    onValueChange = { firebaseApiKey = it },
                                    label = { Text("Firebase API Key", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                                        focusedBorderColor = BrandNeonPurple, unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = firebaseAppId,
                                    onValueChange = { firebaseAppId = it },
                                    label = { Text("Firebase App ID", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                                        focusedBorderColor = BrandNeonPurple, unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = firebaseProjectId,
                                    onValueChange = { firebaseProjectId = it },
                                    label = { Text("Firebase Project ID", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                                        focusedBorderColor = BrandNeonPurple, unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(20.dp))
                            }
                        }

                        item {
                            Text(
                                "Seluruh kredensial ini akan disimpan secara aman di SharedPreferences terenkripsi perangkat Anda.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            com.example.api.ApiKeyRegistry.saveFirebaseApiKey(firebaseApiKey)
                            com.example.api.ApiKeyRegistry.saveFirebaseAppId(firebaseAppId)
                            com.example.api.ApiKeyRegistry.saveFirebaseProjectId(firebaseProjectId)
                            com.example.api.ApiKeyRegistry.saveSupabaseUrl(supabaseUrl)
                            com.example.api.ApiKeyRegistry.saveSupabaseAnonKey(supabaseAnonKey)
                            
                            authPrefs.edit().putString("active_auth_system", activeAuthSystem).apply()
                            
                            if (activeAuthSystem == "supabase") {
                                // Re-initialize Supabase dynamic endpoint
                                try {
                                    com.example.auth.utils.SupabaseManager.getInstance(context).initializeApi()
                                    Toast.makeText(context, "Sistem beralih ke Supabase!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal memuat Supabase: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                try {
                                    val manager = com.example.config.FirebaseConfigManager(context)
                                    manager.initializeFirebase()
                                    Toast.makeText(context, "Koneksi Firebase berhasil!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Sistem Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            showBackendSetup = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNeonTeal)
                    ) {
                        Text("Simpan & Terapkan", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackendSetup = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(onRegisterSuccess: (String?) -> Unit, onNavigateLogin: () -> Unit) {
    val context = LocalContext.current
    val authViewModel: com.example.auth.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is com.example.auth.model.AuthState.Loading -> {
                isLoading = true
            }
            is com.example.auth.model.AuthState.EmailNotVerified -> {
                isLoading = false
                Toast.makeText(context, "Registrasi berhasil! Silakan periksa kotak masuk email Anda untuk verifikasi.", Toast.LENGTH_LONG).show()
                onNavigateLogin()
            }
            is com.example.auth.model.AuthState.Authenticated -> {
                isLoading = false
                Toast.makeText(context, "Registrasi berhasil dan langsung masuk!", Toast.LENGTH_SHORT).show()
                onRegisterSuccess(state.user.email)
            }
            is com.example.auth.model.AuthState.Error -> {
                isLoading = false
                Toast.makeText(context, "Registrasi Gagal: ${state.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearError()
            }
            else -> {
                isLoading = false
            }
        }
    }

    fun attemptRegister() {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        authViewModel.register(name = username, username = username, email = email, password = password)
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "CREATE ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
            Text(text = "Bergabung dengan Meydi OS", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it }, label = { Text("Username", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandNeonTeal) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandNeonTeal) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password", color = TextMuted) },
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandNeonPurple) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { attemptRegister() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNeonPurple),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("REGISTER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text("Sudah punya akun? ", color = TextMuted)
                Text("Login", color = BrandNeonPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateLogin() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordContent(onNavigateLogin: () -> Unit) {
    val context = LocalContext.current
    val authViewModel: com.example.auth.ui.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Inisialisasi network monitor lokal
    val networkMonitor = remember { com.example.utils.NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    LaunchedEffect(authState) {
        when (val state = authState) {
            is com.example.auth.model.AuthState.Loading -> {
                isLoading = true
            }
            is com.example.auth.model.AuthState.Unauthenticated -> {
                isLoading = false
                if (state.message?.contains("Password reset email sent") == true) {
                    showSuccessDialog = true
                    cooldownSeconds = 60
                }
            }
            is com.example.auth.model.AuthState.Error -> {
                isLoading = false
                errorMessage = state.message
                authViewModel.clearError()
            }
            else -> {
                isLoading = false
            }
        }
    }

    // Cooldown timer effect
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
    }

    // Email Validation Regex
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        // Background ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandNeonTeal.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offline banner
            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3366).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFFF3366))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Mode Offline: Sambungkan ke internet untuk melakukan reset.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandNeonTeal.copy(alpha = 0.1f))
                    .border(BorderStroke(2.dp, BrandNeonTeal), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = BrandNeonTeal, modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "RESET PASSWORD", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.White, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Masukkan alamat email Anda untuk menerima tautan pemulihan kata sandi.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.pxToDp()) // Custom safety padding wrapper
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("Email Terdaftar", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandNeonTeal) },
                isError = email.isNotEmpty() && !isEmailValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                    focusedBorderColor = BrandNeonTeal, unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    errorBorderColor = Color(0xFFFF3366)
                ),
                shape = RoundedCornerShape(12.dp), 
                modifier = Modifier.fillMaxWidth()
            )
            
            if (email.isNotEmpty() && !isEmailValid) {
                Text(
                    text = "Format email tidak valid",
                    color = Color(0xFFFF3366),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Masukkan alamat email Anda terlebih dahulu."
                        return@Button
                    }
                    if (!isEmailValid) {
                        errorMessage = "Format alamat email salah."
                        return@Button
                    }
                    if (!isOnline) {
                        errorMessage = "Tidak ada koneksi internet. Silakan coba beberapa saat lagi."
                        return@Button
                    }
                    if (cooldownSeconds > 0) {
                        errorMessage = "Harap tunggu $cooldownSeconds detik sebelum mengirim kembali."
                        return@Button
                    }

                    isLoading = true
                    authViewModel.resetPassword(email)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandNeonTeal,
                    disabledContainerColor = InputBg
                ),
                enabled = !isLoading && isOnline
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp))
                } else if (cooldownSeconds > 0) {
                    Text("COOLDOWN (${cooldownSeconds}s)", color = TextMuted, fontWeight = FontWeight.Bold)
                } else {
                    Text("KIRIM LINK RESET", color = DarkBg, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateLogin) { 
                Text("KEMBALI KE LOGIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp) 
            }
        }
    }

    // Success Dialog modern
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Reset Terkirim", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    text = "Kami telah mengirimkan tautan penyetelan ulang kata sandi ke email: $email.\n\nSilakan periksa folder kotak masuk atau spam Anda.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNeonTeal)
                ) {
                    Text("Ok, Mengerti", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = InputBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Error Alert Dialog / Snackbar modern
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFF3366), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gagal Mengirim", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(text = msg, color = Color.LightGray, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366))
                ) {
                    Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = InputBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Utility extension helper for inline padding safety
@Composable
private fun Int.pxToDp(): androidx.compose.ui.unit.Dp {
    return (this / androidx.compose.ui.platform.LocalDensity.current.density).dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthContent(onAuthSuccess: (String?) -> Unit, onNavigateLogin: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = BrandNeonTeal, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "PHONE LOGIN", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
            Text(text = "Login cepat menggunakan nomor HP", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            if (!codeSent) {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP (Contoh: +628...)", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (FirebaseManager.auth == null) {
                            FirebaseManager.showNotSetupMessage(context)
                            codeSent = true
                        } else {
                            // Simulasi UI (Implementasi Firebase Auth Phone memerlukan Activity context & Callback khusus)
                            Toast.makeText(context, "Meminta kode OTP...", Toast.LENGTH_SHORT).show()
                            codeSent = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNeonTeal)
                ) { Text("KIRIM OTP", color = DarkBg, fontWeight = FontWeight.Bold) }
            } else {
                OutlinedTextField(
                    value = otp, onValueChange = { otp = it }, label = { Text("Kode OTP 6-Digit", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Verifikasi Sukses (Simulasi)", Toast.LENGTH_SHORT).show()
                        onAuthSuccess(phone)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNeonPurple)
                ) { Text("VERIFIKASI OTP", color = Color.White, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateLogin) { Text("Kembali ke Login", color = Color.White) }
        }
    }
}
