package com.example.autoclipper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClipperDashboardScreen(
    onNavigateToImageClipper: () -> Unit,
    onNavigateToVideoClipper: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Auto Clipper", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        containerColor = ObsidianBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pilih Media untuk Diedit",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hapus background dan potong objek otomatis menggunakan AI",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Image Clipper Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToImageClipper() },
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text("Edit Foto", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Hapus BG & Crop Objek", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Video Clipper Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToVideoClipper() },
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.example.ui.theme.NeonMagenta.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, tint = com.example.ui.theme.NeonMagenta, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text("Edit Video", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Hapus BG Frame-by-Frame", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
