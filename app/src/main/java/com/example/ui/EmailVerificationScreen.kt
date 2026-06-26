package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.VerificationStatusConstants
import com.example.viewmodel.EmailVerificationViewModel

// Brand Cyberpunk Theme Colors
private val BrandNeonTeal = Color(0xFF00FFCC)
private val BrandNeonPurple = Color(0xFF7F00FF)
private val BrandDarkBg = Color(0xFF0A0A10)
private val BrandSurfaceBg = Color(0xFF151522)
private val BrandTextMuted = Color(0xFFAAAAAA)
private val BrandGreenSuccess = Color(0xFF00FF66)
private val BrandRedAlert = Color(0xFFFF3366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    onVerificationSuccess: () -> Unit,
    onLogout: () -> Unit,
    viewModel: EmailVerificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val verificationData by viewModel.verificationData.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val cooldownSeconds by viewModel.cooldownSeconds.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    // Ambil User yang sedang login secara langsung dari Firebase Auth
    val authUser = remember { com.example.utils.FirebaseManager.auth?.currentUser }
    val userEmail = authUser?.email ?: "Tidak ada email"

    // Registrasikan event lifecycle onResume untuk otomatis cek status ketika pengguna kembali ke aplikasi
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStatus(onVerified = onVerificationSuccess)
                viewModel.startAutoPoll(onVerified = onVerificationSuccess)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopAutoPoll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopAutoPoll()
        }
    }

    // Tampilkan pesan toast jika ada pembaruan state
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // Animasi Pulse untuk efek visual neon glow pada indikator status
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = BrandDarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "EMAIL VERIFICATION",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = BrandRedAlert
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkStatus(onVerified = onVerificationSuccess) },
                        enabled = !isRefreshing && isOnline
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BrandNeonTeal,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Status",
                                tint = BrandNeonTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BrandDarkBg),
            contentAlignment = Alignment.Center
        ) {
            // Background ambient gradients
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BrandNeonPurple.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Connection indicator banner
                AnimatedVisibility(
                    visible = !isOnline,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandRedAlert.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, BrandRedAlert.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = "Offline", tint = BrandRedAlert)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mode Offline: Sambungkan ke internet untuk memverifikasi akun Anda.",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Status Ring & Icon (Glow Neon effect)
                val status = verificationData?.status ?: VerificationStatusConstants.UNVERIFIED
                val ringColor = when (status) {
                    VerificationStatusConstants.VERIFIED -> BrandGreenSuccess
                    VerificationStatusConstants.VERIFYING -> BrandNeonTeal
                    VerificationStatusConstants.FAILED -> BrandRedAlert
                    else -> BrandNeonPurple
                }
                
                val statusIcon = when (status) {
                    VerificationStatusConstants.VERIFIED -> Icons.Default.CheckCircle
                    VerificationStatusConstants.VERIFYING -> Icons.Default.MarkEmailUnread
                    VerificationStatusConstants.FAILED -> Icons.Default.Error
                    else -> Icons.Default.MarkEmailUnread
                }

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = 0.1f))
                        .border(BorderStroke(2.dp, Brush.linearGradient(listOf(ringColor, ringColor.copy(alpha = 0.3f)))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status Icon",
                        tint = ringColor,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Heading & Status Label
                Text(
                    text = "STATUS AKUN ANDA",
                    color = BrandTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = status.uppercase(),
                    color = ringColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Address Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BrandSurfaceBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Email Terdaftar",
                            color = BrandTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userEmail,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step Guidance Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GuidanceRow(
                        step = "1",
                        description = "Sistem secara otomatis mengirimkan tautan verifikasi ke email Anda setelah mendaftar."
                    )
                    GuidanceRow(
                        step = "2",
                        description = "Buka folder inbox atau spam Anda, cari email dari Meydi OS, dan klik tautan konfirmasi."
                    )
                    GuidanceRow(
                        step = "3",
                        description = "Kembali ke aplikasi ini. Status akan terdeteksi dan diperbarui secara otomatis."
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Main Actions
                // Button 1: Kirim Ulang Email Verifikasi (with Cooldown)
                Button(
                    onClick = { viewModel.resendVerificationEmail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("resend_verification_button"),
                    enabled = cooldownSeconds == 0 && !isRefreshing && isOnline,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandNeonPurple,
                        disabledContainerColor = BrandSurfaceBg
                    )
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else if (cooldownSeconds > 0) {
                        Text(
                            text = "KIRIM ULANG (${cooldownSeconds}s)",
                            color = BrandTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KIRIM ULANG EMAIL VERIFIKASI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button 2: Manual Check Status
                OutlinedButton(
                    onClick = { viewModel.checkStatus(onVerified = onVerificationSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("check_status_button"),
                    enabled = !isRefreshing && isOnline,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandNeonTeal),
                    border = BorderStroke(1.5.dp, if (isOnline) BrandNeonTeal else BrandSurfaceBg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CEK STATUS VERIFIKASI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cooldown linear progress bar (visualizer)
                if (cooldownSeconds > 0) {
                    val progress = cooldownSeconds / 60f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BrandNeonTeal,
                        trackColor = BrandSurfaceBg
                    )
                }
            }
        }
    }
}

@Composable
fun GuidanceRow(step: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BrandNeonTeal.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, BrandNeonTeal), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = BrandNeonTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = description,
            color = Color.LightGray,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
