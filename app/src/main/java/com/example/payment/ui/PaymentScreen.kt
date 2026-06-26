package com.example.payment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.payment.model.PaymentStatus
import com.example.payment.ui.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val transaction by viewModel.transaction.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(transaction?.status) {
        if (transaction?.status == PaymentStatus.SUCCESS) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pembayaran QRIS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val txn = transaction
        if (txn == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PaymentStatusCard(txn.status, timeLeft)
                
                Spacer(Modifier.height(24.dp))

                QRISCard(txn)

                Spacer(Modifier.height(24.dp))

                TransactionDetailCard(txn, onCopy = { clipboardManager.setText(AnnotatedString(it)) })

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.checkStatus() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = txn.status != PaymentStatus.VERIFYING
                ) {
                    if (txn.status == PaymentStatus.VERIFYING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("MEMVERIFIKASI...")
                    } else {
                        Text("SAYA SUDAH BAYAR", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(onClick = onBack) {
                    Text("Batalkan")
                }
            }
        }
    }
}

@Composable
fun PaymentStatusCard(status: PaymentStatus, timeLeft: Long) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when(status) {
                    PaymentStatus.PENDING -> "Selesaikan pembayaran dalam:"
                    PaymentStatus.VERIFYING -> "Sedang memverifikasi..."
                    PaymentStatus.SUCCESS -> "Pembayaran Berhasil!"
                    else -> "Status: ${status.name}"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (status == PaymentStatus.PENDING) {
                Text(
                    String.format("%02d:%02d", minutes, seconds),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun QRISCard(txn: com.example.payment.model.PaymentTransaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(txn.storeName, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black)
        Text("NMID : ID1026509242271", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        
        Image(
            painter = painterResource(id = com.example.R.drawable.qris_static),
            contentDescription = "QRIS Payment",
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        
        Spacer(Modifier.height(16.dp))
        Text("SATU QRIS UNTUK SEMUA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text("Cek aplikasi penyelenggara di: www.aspi-qris.id", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun TransactionDetailCard(txn: com.example.payment.model.PaymentTransaction, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            DetailItem("Total Bayar", "Rp ${String.format("%,.0f", txn.totalAmount)}", isHighlight = true, onCopy = onCopy)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DetailItem("Invoice ID", txn.invoiceId, onCopy = onCopy)
            DetailItem("Kode Transaksi", txn.transactionCode, onCopy = onCopy)
            DetailItem("Kode Unik", txn.uniqueCode.toString())
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, isHighlight: Boolean = false, onCopy: ((String) -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Medium,
                fontSize = if (isHighlight) 18.sp else 14.sp,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (onCopy != null) {
                IconButton(onClick = { onCopy(value.replace("Rp ", "").replace(".", "")) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", Modifier.size(16.dp))
                }
            }
        }
    }
}
