package com.example.rbac.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkStroke
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Translate"
    )

    val shimmerColors = listOf(
        MidnightSurface,
        MidnightSurface.copy(alpha = 0.6f),
        MidnightSurface,
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush, RoundedCornerShape(8.dp))
            .border(0.5.dp, DarkStroke, RoundedCornerShape(8.dp))
    )
}

@Composable
fun RbacSkeletonLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerItem(modifier = Modifier.size(48.dp).clip(CircleShape))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                ShimmerItem(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                ShimmerItem(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
        }
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(1.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerItem(modifier = Modifier.size(48.dp).clip(CircleShape))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                ShimmerItem(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
                ShimmerItem(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
            }
        }
    }
}

@Composable
fun CyberDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, DarkStroke, RoundedCornerShape(16.dp)),
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
