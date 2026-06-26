package com.example.downloader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.downloader.model.DownloadStatus
import com.example.downloader.model.DownloadTask
import com.example.downloader.model.MediaFormat
import com.example.downloader.viewmodel.DownloaderViewModel
import com.example.ui.theme.DarkStroke
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalDownloaderScreen(
    onBack: () -> Unit
) {
    val viewModel: DownloaderViewModel = viewModel()
    val tasks by viewModel.tasks.collectAsState()
    val currentUrl by viewModel.currentInputUrl.collectAsState()
    val isAnalyzing by viewModel.analyzingUrl.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Downloader", "Dashboard")

    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Universal Downloader", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = ObsidianBg,
                    contentColor = NeonTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = NeonTeal
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, color = if (selectedTabIndex == index) NeonTeal else TextMuted, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        containerColor = ObsidianBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedTabIndex == 0) {
                DownloaderContent(
                    viewModel = viewModel,
                    tasks = tasks,
                    currentUrl = currentUrl,
                    isAnalyzing = isAnalyzing,
                    clipboardManager = clipboardManager,
                    keyboardController = keyboardController
                )
            } else {
                DashboardContent(tasks = tasks)
            }
        }
    }
}

@Composable
fun DownloaderContent(
    viewModel: DownloaderViewModel,
    tasks: List<DownloadTask>,
    currentUrl: String,
    isAnalyzing: Boolean,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            // Header / Input Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = currentUrl,
                    onValueChange = { viewModel.setUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste link video atau audio...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightSurface,
                        unfocusedContainerColor = MidnightSurface,
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = DarkStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        viewModel.analyzeUrl()
                    }),
                    trailingIcon = {
                        if (currentUrl.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setUrl("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        } else {
                            IconButton(onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    viewModel.setUrl(clipText)
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonTeal)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.analyzeUrl()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                    shape = RoundedCornerShape(12.dp),
                    enabled = currentUrl.isNotBlank() && !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analisis Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            HorizontalDivider(color = DarkStroke, thickness = 1.dp)

            // Queue List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum ada tugas unduhan", color = TextMuted, fontSize = 16.sp)
                        Text("Dukung YouTube, TikTok, Instagram, dll.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onDownloadFormat = { format -> viewModel.startDownload(task.id, format) },
                            onPause = { viewModel.pauseDownload(task.id) },
                            onResume = { viewModel.resumeDownload(task.id) },
                            onCancel = { viewModel.cancelDownload(task.id) },
                            onRemove = { viewModel.removeTask(task.id) }
                        )
                    }
                }
            }
        }
    }

@Composable
fun DashboardContent(tasks: List<DownloadTask>) {
    val completedCount = tasks.count { it.status == DownloadStatus.COMPLETED }
    val totalBytes = tasks.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.totalBytes }
    val totalSizeMb = if (totalBytes > 0) totalBytes / (1024 * 1024) else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Statistik Unduhan", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Selesai",
                    value = "$completedCount File",
                    icon = Icons.Default.CheckCircle,
                    color = NeonTeal
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Ukuran",
                    value = "$totalSizeMb MB",
                    icon = Icons.Default.Storage,
                    color = Color(0xFF9C27B0)
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Platform Tersedia", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grid of supported platforms
            val platforms = listOf("YouTube", "TikTok", "Instagram", "X (Twitter)", "Facebook", "Pinterest", "Reddit")
            
            platforms.chunked(2).forEach { rowPlatforms ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPlatforms.forEach { platform ->
                        Card(
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                            border = BorderStroke(1.dp, DarkStroke)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(platform, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (rowPlatforms.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkStroke)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    onDownloadFormat: (MediaFormat) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkStroke)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task Header (URL or Error)
            if (task.status == DownloadStatus.FAILED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gagal: ${task.errorMessage}", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                    }
                }
            } else if (task.status == DownloadStatus.FETCHING_INFO) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonTeal, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mengambil informasi media...", color = Color.White, fontSize = 14.sp)
                }
            }

            // Media Info
            if (task.mediaInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail
                    AsyncImage(
                        model = task.mediaInfo.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )

                    // Title & Platform
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.mediaInfo.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(task.platform.displayName, color = NeonTeal, fontSize = 12.sp)
                            task.mediaInfo.duration?.let {
                                Text(" • $it", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Area Based on Status
                when (task.status) {
                    DownloadStatus.READY -> {
                        // Format Selection
                        Text("Pilih Format:", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            task.mediaInfo.formats.take(3).forEach { format ->
                                Button(
                                    onClick = { onDownloadFormat(format) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBg),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(format.resolution, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(format.size, color = NeonTeal, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                    DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (task.status == DownloadStatus.DOWNLOADING) "Mengunduh... ${task.speed}" else "Dijeda",
                                    color = if (task.status == DownloadStatus.PAUSED) Color.Yellow else NeonTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(task.progress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = NeonTeal,
                                trackColor = ObsidianBg
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (task.status == DownloadStatus.DOWNLOADING) {
                                    IconButton(onClick = onPause) {
                                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.Yellow)
                                    }
                                } else {
                                    IconButton(onClick = onResume) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = NeonTeal)
                                    }
                                }
                                IconButton(onClick = onCancel) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                                }
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(NeonTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selesai", tint = NeonTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Berhasil diunduh", color = NeonTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Tersimpan di Galeri/Downloads", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onRemove) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Riwayat", tint = TextMuted)
                            }
                        }
                    }
                    DownloadStatus.CANCELED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dibatalkan", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onRemove) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = TextMuted)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
