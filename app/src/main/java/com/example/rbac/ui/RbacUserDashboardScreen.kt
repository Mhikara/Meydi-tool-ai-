package com.example.rbac.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rbac.viewmodel.RbacViewModel
import com.example.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun RbacUserDashboardScreen(
    viewModel: RbacViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUserState.collectAsState()
    val scrollState = rememberScrollState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonTeal, NeonPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.nama?.take(1)?.uppercase() ?: "U",
                            color = ObsidianBg,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = currentUser?.nama ?: "Regular User",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "user@domain.com",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        
                        // Role Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonTeal.copy(alpha = 0.15f))
                                .border(1.dp, NeonTeal, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, null, tint = NeonTeal, modifier = Modifier.size(12.dp))
                            Text(
                                text = currentUser?.userRole?.label?.uppercase() ?: "USER",
                                color = NeonTeal,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
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
        }


        // Account Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Detail Akun & Akses",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = DarkStroke)

                // Detail Items
                DetailRow(label = "Status Akun", value = currentUser?.userStatus?.label ?: "Aktif", color = Color(0xFF00FF88))
                DetailRow(label = "Tanggal Registrasi", value = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentUser?.createdAt ?: System.currentTimeMillis())))
                DetailRow(label = "Sesi Terakhir", value = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentUser?.lastLogin ?: System.currentTimeMillis())))
                DetailRow(label = "Tipe Akses", value = "Client-Side Caching (Offline)")
            }
        }

        // Action Options Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, DarkStroke)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Aksi Cepat",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = DarkStroke)

                // Tombol Premium
                Button(
                    onClick = { onNavigateToSubscription() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kelola Langganan Premium", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Kebijakan Privasi
                Button(
                    onClick = { onNavigateToPrivacyPolicy() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Policy, null, tint = NeonTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kebijakan Privasi (Privacy Policy)", color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Ubah Password
                Button(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Lock, null, tint = NeonPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ubah Password Akun", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Logout
                Button(
                    onClick = { 
                        viewModel.logout() 
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar (Log Out)", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Ubah Password Dialog
    if (showChangePasswordDialog) {
        CyberDialog(
            title = "Ubah Password",
            onDismiss = { showChangePasswordDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPassword.isNotBlank() && newPassword.length >= 6) {
                            Toast.makeText(context, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            showChangePasswordDialog = false
                            currentPassword = ""
                            newPassword = ""
                        } else {
                            Toast.makeText(context, "Password baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Gunakan password aman demi melindungi kredensial Anda.", color = TextMuted, fontSize = 11.sp)
                
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Password Saat Ini", color = TextMuted) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru (Min 6 Karakter)", color = TextMuted) },
                    visualTransformation = PasswordVisualTransformation(),
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
}

@Composable
fun DetailRow(label: String, value: String, color: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
