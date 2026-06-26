package com.example.autoclipper.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoclipper.processing.VideoProcessor
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoClipperScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var videoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var processMessage by remember { mutableStateOf("") }
    var isComplete by remember { mutableStateOf(false) }
    
    val videoProcessor = remember { VideoProcessor() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            isComplete = false
            progress = 0f
            processMessage = "Video siap diproses"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Auto Clipper", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = com.example.ui.theme.NeonMagenta)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Video Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightSurface)
                    .border(1.dp, DarkStroke, RoundedCornerShape(16.dp))
                    .clickable { if (!isProcessing) launcher.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                if (videoUri != null) {
                    // For brevity, we don't implement the full ExoPlayer here.
                    // Assuming we have the VideoViewPlayer from the project
                    com.example.ui.VideoViewPlayer(
                        videoUri = videoUri!!,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, tint = com.example.ui.theme.NeonMagenta, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ketuk untuk memilih video", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(progress = { progress }, color = com.example.ui.theme.NeonMagenta)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("${(progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(processMessage, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Auto Video Background Removal", color = com.example.ui.theme.NeonMagenta, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Fitur ini menggunakan AI untuk mengekstrak objek bergerak dari video secara frame-by-frame dan menghapus latar belakangnya.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    if (videoUri == null) {
                        Toast.makeText(context, "Pilih video terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    coroutineScope.launch {
                        isProcessing = true
                        processMessage = "Mengekstrak frame..."
                        val success = videoProcessor.processVideoBackgroundRemoval(
                            videoUri = videoUri!!,
                            onProgress = { p -> 
                                progress = p 
                                processMessage = if (p < 0.5f) "Memisahkan foreground..." else "Menyusun ulang video..."
                            }
                        )
                        isProcessing = false
                        if (success) {
                            isComplete = true
                            processMessage = "Selesai!"
                            Toast.makeText(context, "Video berhasil diproses (Simulasi)", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonMagenta),
                shape = RoundedCornerShape(12.dp),
                enabled = !isProcessing && videoUri != null
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isComplete) "Proses Ulang" else "Hapus Background Video", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
