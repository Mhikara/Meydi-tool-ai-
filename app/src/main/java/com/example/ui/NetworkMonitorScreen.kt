package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// --- THEME COLORS ---
private val DarkBg = Color(0xFF0A0A10)
private val PanelBg = Color(0xFF161622)
private val NeonTeal = Color(0xFF00FFCC)
private val NeonRed = Color(0xFFFF3366)
private val NeonYellow = Color(0xFFFFD700)
private val TextMuted = Color(0xFFAAAAAA)

data class NetworkLog(val time: String, val status: String, val ping: Int, val event: String, val isError: Boolean = false)
data class ApiEndpoint(val name: String, val url: String, var status: Int, var ttfb: Int)

@Composable
fun NetworkMonitorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    // Core Ping State
    var isOnline by remember { mutableStateOf(true) }
    var currentPing by remember { mutableStateOf(45) }
    var maxRetries by remember { mutableStateOf(3) }
    var retriesCount by remember { mutableStateOf(0) }
    
    // Latency & Packet Loss State
    var minPing by remember { mutableStateOf(45) }
    var maxPing by remember { mutableStateOf(45) }
    var avgPing by remember { mutableStateOf(45) }
    var packetLoss by remember { mutableStateOf(0.0) }
    
    // Speed Monitor State
    var downloadSpeed by remember { mutableStateOf(125.5f) } // Mbps
    var uploadSpeed by remember { mutableStateOf(45.2f) } // Mbps
    
    // Bandwidth Usage State
    var totalTx by remember { mutableStateOf(10.2f) } // GB
    var totalRx by remember { mutableStateOf(45.8f) } // GB
    
    // Logs State
    var networkLogs by remember { mutableStateOf(listOf<NetworkLog>()) }
    
    // API Endpoints State
    var endpoints by remember { 
        mutableStateOf(listOf(
            ApiEndpoint("Main Server", "api.geministudio.com", 200, 120),
            ApiEndpoint("Auth Service", "auth.geministudio.com", 200, 85),
            ApiEndpoint("Storage S3", "s3.geministudio.com", 200, 210)
        )) 
    }

    // Ping Simulation Loop
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            delay(2000) // Interval 2 detik
            val success = Random.nextFloat() > 0.05f || retriesCount >= maxRetries
            
            if (success) {
                isOnline = true
                retriesCount = 0
                currentPing = Random.nextInt(20, 150)
                
                // Update min/max/avg
                if (currentPing < minPing) minPing = currentPing
                if (currentPing > maxPing) maxPing = currentPing
                avgPing = (minPing + maxPing + currentPing) / 3
                
                // Speed fluctuation simulation
                downloadSpeed = 100f + Random.nextFloat() * 50f
                uploadSpeed = 30f + Random.nextFloat() * 20f
                
                // Traffic simulation
                totalTx += 0.01f
                totalRx += 0.05f
                
                // Log periodic
                if (Random.nextFloat() > 0.8f) {
                    val log = NetworkLog(sdf.format(Date()), "ONLINE", currentPing, "Ping Successful")
                    networkLogs = listOf(log) + networkLogs.take(49)
                }
            } else {
                isOnline = false
                retriesCount++
                currentPing = -1 // RTO
                packetLoss += 0.5
                
                val log = NetworkLog(sdf.format(Date()), "OFFLINE", -1, "RTO - Timeout", isError = true)
                networkLogs = listOf(log) + networkLogs.take(49)
                
                if (retriesCount >= 3) {
                    val errorLog = NetworkLog(sdf.format(Date()), "ALERT", -1, "Destination Unreachable (Down)", isError = true)
                    networkLogs = listOf(errorLog) + networkLogs.take(49)
                }
            }
            
            // Randomize API Status slightly
            endpoints = endpoints.map { 
                if (Random.nextFloat() < 0.02f) it.copy(status = 500, ttfb = -1) 
                else it.copy(status = 200, ttfb = Random.nextInt(50, 300))
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(PanelBg)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NETWORK DIAGNOSTICS",
                    color = NeonTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Core Ping & Status
            item {
                Text("A. SISTEM PING JARINGAN (CORE PING)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelBg)
                            .border(1.dp, if (isOnline) NeonTeal else NeonRed, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) NeonTeal else NeonRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(if (isOnline) "[🟢 ONLINE]" else "[🔴 OFFLINE]", color = if (isOnline) NeonTeal else NeonRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (!isOnline && retriesCount > 0) {
                                Text("Reconnecting ($retriesCount/$maxRetries)...", color = NeonYellow, fontSize = 10.sp)
                            }
                        }
                    }

                    // Ping Value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelBg)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Realtime Ping", color = TextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentPing >= 0) "${currentPing} ms" else "RTO", 
                                color = if (currentPing < 0) NeonRed else if (currentPing > 100) NeonYellow else Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 28.sp
                            )
                        }
                    }
                }
            }

            // 2. Advanced Diagnostics
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("B. FITUR MONITORING (ADVANCED)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Latency & Packet Loss
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DiagnosticCard("Latency Spikes", "${minPing}/${avgPing}/${maxPing} ms", "Min/Avg/Max", modifier = Modifier.weight(1f))
                    DiagnosticCard("Packet Loss", String.format("%.2f%%", packetLoss), if (packetLoss > 2.0) "⚠️ WARNING" else "Normal", modifier = Modifier.weight(1f), isWarning = packetLoss > 2.0)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speed & Bandwidth
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DiagnosticCard("Ul/Dl Speed", "${uploadSpeed.toInt()}/${downloadSpeed.toInt()} Mbps", "Real-time Traffic", modifier = Modifier.weight(1f))
                    DiagnosticCard("Bandwidth", String.format("%.1f/%.1f GB", totalTx, totalRx), "TX / RX (Sesi ini)", modifier = Modifier.weight(1f))
                }
            }

            // 3. API Status Monitor
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("C. API STATUS MONITOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PanelBg).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    endpoints.forEach { api ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(api.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(api.url, color = TextMuted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (api.status == 200) "200 OK" else "${api.status} ERROR", 
                                    color = if (api.status == 200) NeonTeal else NeonRed, 
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                                Text("TTFB: ${if(api.ttfb > 0) "${api.ttfb} ms" else "Timeout"}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // 4. Network History Logs
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("D. NETWORK HISTORY LOGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(PanelBg).padding(12.dp)) {
                    if (networkLogs.isEmpty()) {
                        Text("Waiting for logs...", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(networkLogs) { log ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("[${log.time}] ", color = TextMuted, fontSize = 10.sp)
                                    Text(log.status + " | ", color = if (log.isError) NeonRed else NeonTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(log.event, color = if (log.isError) NeonRed else Color.White, fontSize = 10.sp)
                                    if (log.ping >= 0) {
                                        Text(" (${log.ping}ms)", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier, isWarning: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBg)
            .border(1.dp, if (isWarning) NeonRed else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = if (isWarning) NeonRed else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, color = if (isWarning) NeonRed else NeonTeal, fontSize = 10.sp)
    }
}
