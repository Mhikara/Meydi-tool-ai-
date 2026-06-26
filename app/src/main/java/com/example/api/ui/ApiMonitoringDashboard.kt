package com.example.api.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.api.manager.ApiEndpoint
import com.example.api.manager.ApiKeyModel
import com.example.api.manager.ApiKeyStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiMonitoringDashboard(
    endpoints: List<ApiEndpoint>,
    apiKeys: List<ApiKeyModel>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Management System", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Endpoint Status", style = MaterialTheme.typography.titleMedium)
            }
            
            items(endpoints) { endpoint ->
                EndpointItem(endpoint)
            }
            
            item {
                Spacer(Modifier.height(8.dp))
                Text("API Key Health", style = MaterialTheme.typography.titleMedium)
            }
            
            items(apiKeys) { key ->
                ApiKeyItem(key)
            }
        }
    }
}

@Composable
fun EndpointItem(endpoint: ApiEndpoint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = if (endpoint.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(endpoint.name, fontWeight = FontWeight.Bold)
                Text(endpoint.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Badge(
                containerColor = if (endpoint.isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Text(
                    if (endpoint.isHealthy) "HEALTHY" else "DOWN",
                    color = if (endpoint.isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ApiKeyItem(key: ApiKeyModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(key.provider, fontWeight = FontWeight.Bold)
                Text("ID: ${key.id}", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                key.status.name,
                color = when(key.status) {
                    ApiKeyStatus.VALID -> Color(0xFF4CAF50)
                    ApiKeyStatus.INVALID, ApiKeyStatus.EXPIRED -> Color(0xFFF44336)
                    else -> Color.Gray
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}
