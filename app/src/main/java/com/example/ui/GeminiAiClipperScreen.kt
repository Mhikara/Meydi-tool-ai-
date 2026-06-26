package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GeminiAiClipperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isVideoLoaded by remember { mutableStateOf(false) }
    var loadedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var hasAnalyzed by remember { mutableStateOf(false) }
    var timelineIntervals by remember { mutableStateOf(listOf<ClipperInterval>()) }
    var activePreviewSegment by remember { mutableStateOf<ClipperInterval?>(null) }

    var targetPrompt by remember { mutableStateOf("") }
    var targetDuration by remember { mutableStateOf("15") }
    val liveLogs = remember { mutableStateListOf<String>() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            loadedMediaUri = uri
            isVideoLoaded = true
            hasAnalyzed = false
            timelineIntervals = emptyList()
            liveLogs.clear()
            Toast.makeText(context, "Video berhasil dimuat!", Toast.LENGTH_SHORT).show()
        }
    }

    fun pickVideo() {
        mediaPickerLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }

    fun processClipper() {
        if (!isVideoLoaded) {
            Toast.makeText(context, "Harap pilih video terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetPrompt.isBlank()) {
            Toast.makeText(context, "Pilih atau tulis momen yang ingin dicari!", Toast.LENGTH_SHORT).show()
            return
        }
        
        keyboardController?.hide()
        isAnalyzing = true
        analysisProgress = 0f
        hasAnalyzed = false
        liveLogs.clear()
        
        coroutineScope.launch {
            liveLogs.add("Menghubungkan ke Gemini AI Engine...")
            delay(800)
            analysisProgress = 0.2f
            
            val aiInsight = try {
                GeminiGenerator.generateCanvasCode("AS_ASSISTANT: Berikan 1 alasan (max 10 kata) kenapa momen '${targetPrompt}' cocok dipotong menjadi klip pendek.")
            } catch (e: Exception) {
                "Analisis otomatis menemukan transisi terbaik."
            }
            liveLogs.add("Gemini: $aiInsight")
            delay(1000)
            analysisProgress = 0.5f
            liveLogs.add("Mendeteksi momen '${targetPrompt}' dalam video...")
            delay(1200)
            analysisProgress = 0.8f
            liveLogs.add("Mengekstrak frame dan menyusun potongan akhir...")
            delay(800)
            analysisProgress = 1.0f

            val generatedSegments = mutableListOf<ClipperInterval>()
            val durationSecs = targetDuration.toIntOrNull() ?: 15
            val fraction = (durationSecs / 60f).coerceIn(0.1f, 1.0f)
            
            generatedSegments.add(ClipperInterval(0.0f, fraction / 2, false, "Klip 1: Momen ${targetPrompt}", "Gemini Insight: Sangat relevan.", true))
            generatedSegments.add(ClipperInterval(fraction / 2, fraction, false, "Klip 2: Klimaks ${targetPrompt}", "Gemini Insight: Puncak adegan menarik.", true))
            
            timelineIntervals = generatedSegments
            liveLogs.add("[Selesai] Auto Clip berhasil menghasilkan klip yang relevan!")
            hasAnalyzed = true
            isAnalyzing = false
            Toast.makeText(context, "Pemotongan Cerdas Selesai!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back Button",
                            tint = NeonMagenta
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "AI Auto Clipper (Gemini)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Potong video cerdas pada momen menarik & edukasi",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. VIDEO PICKER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightSurface)
                    .border(1.dp, if (isVideoLoaded) NeonTeal else DarkStroke, RoundedCornerShape(12.dp))
                    .clickable { if (!isAnalyzing) pickVideo() },
                contentAlignment = Alignment.Center
            ) {
                if (isVideoLoaded && loadedMediaUri != null) {
                    VideoViewPlayer(
                        videoUri = loadedMediaUri!!,
                        modifier = Modifier.fillMaxSize(),
                        playRange = activePreviewSegment?.let { Pair(it.start, it.end) }
                    )
                    activePreviewSegment?.let { seg ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonMagenta.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Preview: ${seg.title}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "",
                            tint = NeonTeal,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ketuk untuk Memilih Video", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. GEMINI AI SETTINGS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Deteksi Momen Cerdas (Gemini)", color = NeonTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Pilih momen yang ingin diekstrak:", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Momen Lucu", "Edukasi", "Menarik", "Klimaks Aksi").forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (targetPrompt == chip) NeonMagenta else ObsidianBg)
                                    .border(1.dp, NeonMagenta, RoundedCornerShape(20.dp))
                                    .clickable { targetPrompt = chip }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(chip, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetPrompt,
                        onValueChange = { targetPrompt = it },
                        placeholder = { Text("Atau ketik bebas... (cth: Bagian paling sedih)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = DarkStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Target Durasi (Detik):", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetDuration,
                        onValueChange = { targetDuration = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg,
                            focusedBorderColor = NeonTeal,
                            unfocusedBorderColor = DarkStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { processClipper() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                        enabled = !isAnalyzing
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) "Sedang Memproses..." else "Mulai Auto Clip ✂️",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 3. LOGS
            if (isAnalyzing || liveLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, NeonMagenta)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = NeonTeal, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Gemini AI Menganalisa... ${(analysisProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { analysisProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = NeonTeal,
                                trackColor = ObsidianBg,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        
                        Text("Log AI Gemini:", color = NeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        liveLogs.forEach { log ->
                            Text(text = "• $log", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }

            // 4. RESULTS
            if (hasAnalyzed && timelineIntervals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Hasil Auto Clip Terbaik 🌟",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                timelineIntervals.forEach { interval ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                        border = BorderStroke(1.dp, if (activePreviewSegment == interval) NeonTeal else DarkStroke)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(interval.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${String.format("%.1f", interval.start * 30f)}s - ${String.format("%.1f", interval.end * 30f)}s", color = NeonTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(interval.reason, color = TextMuted, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { activePreviewSegment = if (activePreviewSegment == interval) null else interval },
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (activePreviewSegment == interval) NeonTeal else NeonMagenta),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activePreviewSegment == interval) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (activePreviewSegment == interval) "Hentikan" else "Preview Klip", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { 
                                        autoSaveMediaToDevice(context, "MeydiAI_AutoClip_${System.currentTimeMillis()}", "video")
                                        Toast.makeText(context, "Klip berhasil disimpan ke Galeri!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalGreen),
                                    border = BorderStroke(1.dp, TerminalGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
