package com.example.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.subscription.model.SubscriptionPlan
import com.example.subscription.ui.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onBack: () -> Unit
) {
    val info by viewModel.subscriptionInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meydi AI Premium") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                PremiumHeader()
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text(
                    "Pilih Paket Langganan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                SubscriptionPlan.values().forEach { plan ->
                    val priceFormatted = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).format(plan.price)
                    PricingCard(
                        plan = plan,
                        priceLabel = priceFormatted,
                        isSelected = info.plan == plan,
                        onSelect = { onSelectPlan(plan) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun PremiumHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "👑",
                fontSize = 48.sp
            )
            Text(
                "Buka Potensi Penuh AI",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Akses fitur HD, AI Premium, dan Tanpa Batas",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PricingCard(
    plan: SubscriptionPlan,
    priceLabel: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = when (plan) {
                        SubscriptionPlan.WEEKLY -> "Premium Mingguan"
                        SubscriptionPlan.MONTHLY -> "Premium Bulanan"
                        SubscriptionPlan.THREE_MONTHS -> "Premium 3 Bulan"
                        SubscriptionPlan.SIX_MONTHS -> "Premium 6 Bulan"
                        SubscriptionPlan.ANNUAL -> "Premium Tahunan"
                        SubscriptionPlan.LIFETIME -> "Premium Lifetime"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    priceLabel,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Terpilih", tint = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "Pilih",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
