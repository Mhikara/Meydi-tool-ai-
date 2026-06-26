package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkStroke
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kebijakan Privasi", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg
                )
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonTeal.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Policy, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Kebijakan Privasi Meydi AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Terakhir Diperbarui: 25 Juni 2026", color = TextMuted, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = DarkStroke)

            // Content Sections
            PrivacySection(
                title = "1. Informasi yang Kami Kumpulkan",
                content = "Kami mengumpulkan jenis informasi berikut untuk menyediakan dan meningkatkan layanan kami:\n\n" +
                        "• Informasi Pribadi: Nama dan alamat email saat Anda mendaftar melalui Google Sign-In atau email.\n" +
                        "• Konten Pengguna: Teks, gambar, audio, dan video yang Anda unggah atau buat menggunakan layanan AI kami.\n" +
                        "• Data Perangkat & Penggunaan: Log kerusakan (crash logs), model perangkat, dan analitik penggunaan dasar (melalui Firebase Analytics)."
            )

            PrivacySection(
                title = "2. Tujuan Pengumpulan Data",
                content = "Data yang kami kumpulkan digunakan untuk:\n\n" +
                        "• Otentikasi dan keamanan akun Anda.\n" +
                        "• Memproses permintaan AI Anda (misal: analisis gambar, generasi teks).\n" +
                        "• Mendiagnosis masalah teknis dan meningkatkan performa aplikasi.\n" +
                        "• Mengirimkan pemberitahuan penting (push notifications) terkait pembaruan atau layanan."
            )

            PrivacySection(
                title = "3. Berbagi Data dengan Pihak Ketiga",
                content = "Kami TIDAK menjual data pribadi Anda. Kami hanya membagikan data dengan layanan pihak ketiga yang esensial untuk pengoperasian aplikasi:\n\n" +
                        "• Firebase & Google Cloud: Untuk otentikasi, database (Firestore), dan analitik.\n" +
                        "• Penyedia API AI: Teks dan media yang Anda proses dapat dikirim ke layanan API AI terenkripsi (seperti Gemini API) secara real-time untuk pemrosesan, namun tidak digunakan untuk melatih model pihak ketiga tanpa izin.\n\n" +
                        "Semua transmisi data dienkripsi (HTTPS/TLS)."
            )

            PrivacySection(
                title = "4. Hak dan Kendali Anda (Penghapusan Data)",
                content = "Anda memiliki kendali penuh atas data Anda. Anda dapat:\n\n" +
                        "• Meminta salinan data pribadi Anda.\n" +
                        "• Menghapus akun Anda dan seluruh data yang terkait secara permanen langsung dari menu Pengaturan Profil di dalam aplikasi."
            )
            
            PrivacySection(
                title = "5. Izin Aplikasi (Permissions)",
                content = "Aplikasi meminta izin berikut hanya jika diperlukan:\n\n" +
                        "• Kamera: Untuk mengambil foto/video langsung untuk dianalisis oleh AI.\n" +
                        "• Penyimpanan (Penyimpanan Eksternal / Media): Untuk mengunggah foto, video, atau dokumen dari perangkat ke AI, dan menyimpan hasil generasi.\n" +
                        "• Notifikasi: Untuk memberi tahu Anda saat tugas pemrosesan latar belakang selesai."
            )

            HorizontalDivider(color = DarkStroke)

            // Contact Section
            Text("Hubungi Kami", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Jika Anda memiliki pertanyaan tentang Kebijakan Privasi ini atau ingin meminta penghapusan data secara manual, silakan hubungi kami:",
                color = TextMuted,
                fontSize = 14.sp
            )
            
            Button(
                onClick = { 
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:meydihikara@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan Kebijakan Privasi Meydi AI")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MidnightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = NeonTeal, modifier = Modifier.padding(end = 8.dp))
                Text("meydihikara@gmail.com", color = NeonTeal)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = NeonTeal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(content, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
