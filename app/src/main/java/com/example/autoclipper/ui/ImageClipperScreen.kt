package com.example.autoclipper.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoclipper.viewmodel.ClipperViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageClipperScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ClipperViewModel = viewModel()
    
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processedBitmap by viewModel.processedBitmap.collectAsState()
    val detectedObjects by viewModel.detectedObjects.collectAsState()
    val processMessage by viewModel.processMessage.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadImage(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Auto Clipper", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonTeal)
                    }
                },
                actions = {
                    if (processedBitmap != null) {
                        IconButton(onClick = { viewModel.resetEdit() }) {
                            Icon(Icons.Default.Restore, contentDescription = "Reset", tint = Color.White)
                        }
                        IconButton(onClick = { 
                            viewModel.saveResult { success ->
                                Toast.makeText(context, if (success) "Berhasil disimpan!" else "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = NeonTeal)
                        }
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
            
            // Image Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightSurface)
                    .border(1.dp, DarkStroke, RoundedCornerShape(16.dp))
                    .clickable { if (!isProcessing && processedBitmap == null) launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (processedBitmap != null) {
                    Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "Preview Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // TODO: Draw bounding boxes for detected objects
                    // For now, we display a count below
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ketuk untuk memilih foto", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonTeal)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(processMessage, color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (processMessage.isNotEmpty() && !isProcessing) {
                Text(processMessage, color = NeonTeal, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            
            // Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    Text("Ganti Foto", color = Color.White)
                }
                
                Button(
                    onClick = { viewModel.removeBackground() },
                    modifier = Modifier.weight(1.5f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing && processedBitmap != null
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus BG", color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.detectObjects() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing && processedBitmap != null
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deteksi Objek", color = NeonTeal)
                }
                
                Button(
                    onClick = { 
                        if (detectedObjects.isNotEmpty()) {
                            viewModel.cropObject(detectedObjects.first())
                        } else {
                            Toast.makeText(context, "Deteksi objek terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing && processedBitmap != null && detectedObjects.isNotEmpty()
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop Otomatis", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
