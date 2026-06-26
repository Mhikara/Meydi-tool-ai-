package com.example.auth.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auth.model.User
import com.example.auth.model.UserRole
import com.example.auth.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateToSubscription: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        user?.let { u ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                
                // Profile Image
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (u.profilePicture != null) {
                        AsyncImage(
                            model = u.profilePicture,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u.fullName.firstOrNull()?.toString() ?: "?",
                                fontSize = 48.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (u.isPremium) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(u.fullName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(u.email, color = Color.Gray, fontSize = 14.sp)
                
                if (u.isPremium) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PENGGUNA PREMIUM", color = Color(0xFFB8860B), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    ProfileMenuItem(Icons.Default.Person, "Edit Profil") { }
                    ProfileMenuItem(Icons.Default.Security, "Keamanan & Password") { }
                    ProfileMenuItem(Icons.Default.Notifications, "Notifikasi") { }
                    ProfileMenuItem(Icons.Default.History, "Riwayat Transaksi") { }
                    
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    
                    ProfileMenuItem(Icons.Default.WorkspacePremium, "Langganan Premium", color = Color(0xFFB8860B)) { onNavigateToSubscription() }
                    ProfileMenuItem(Icons.Default.Description, "Kebijakan Privasi") { onNavigateToPrivacyPolicy() }
                    ProfileMenuItem(Icons.Default.Logout, "Keluar", color = MaterialTheme.colorScheme.error) {
                        viewModel.logout()
                        onBack()
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text("Versi 1.1.0", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color)
            Spacer(Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = if (color == Color.Unspecified) Color.Unspecified else color)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
